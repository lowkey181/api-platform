//package com.api.apigateway.filter;
//
//import cn.hutool.core.util.StrUtil;
//import com.api.apigateway.config.SaResult;
//import com.api.apigateway.enity.ApiCallLogDTO;
//import com.api.apigateway.utils.IpUtil;
//import com.api.apigateway.utils.SignUtil;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.dao.EmptyResultDataAccessException;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.core.Ordered;
//import org.springframework.stereotype.Component;
//import jakarta.annotation.Resource;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.time.Duration;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.TreeMap;
//import java.util.concurrent.TimeUnit;
//
//@Component
//public class SignFilter implements GlobalFilter, Ordered {
//
//    @Resource
//    private JdbcTemplate jdbcTemplate;
//
//    @Resource
//    private RedisTemplate<String, String> redisTemplate;
//    @Resource
//    private RabbitTemplate rabbitTemplate;
//    @Value("${spring.rabbitmq.queue.log}")
//    private String logQueue;
//    // 注入 WebClient
//    private final WebClient webClient = WebClient.create("http://localhost:9002");
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        // 获取你原来的 4 个请求头（不变！）
//        String accessKey = exchange.getRequest().getHeaders().getFirst("accessKey");
//        String sign = exchange.getRequest().getHeaders().getFirst("sign");
//        String timestamp = exchange.getRequest().getHeaders().getFirst("timestamp");
//        String nonce = exchange.getRequest().getHeaders().getFirst("nonce");
//
//        System.out.println("accessKey: " + accessKey+ "sign: " + sign+"timestamp: " + timestamp+ "nonce: " + nonce);
//        // 1. 判空（不变）
//        if (StrUtil.isBlank(accessKey) || StrUtil.isBlank(sign) || StrUtil.isBlank(timestamp) || StrUtil.isBlank(nonce)) {
//            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
//            return exchange.getResponse().setComplete();
//        }
//
//        // 2. 时间戳过期校验（新增！）
//        long currentTime = System.currentTimeMillis();
//        long reqTime = Long.parseLong(timestamp);
//        if (Math.abs(currentTime - reqTime) > 60000 * 5) { // 超过5分钟过期
//            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            return exchange.getResponse().setComplete();
//        }
//
//        // 3. nonce 防重放（核心！新增！）
//        String nonceKey = "sign:nonce:" + nonce;
//        Boolean hasNonce = redisTemplate.hasKey(nonceKey);
//        if (Boolean.TRUE.equals(hasNonce)) {
//            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            return exchange.getResponse().setComplete();
//        }
//        redisTemplate.opsForValue().set(nonceKey, "1", 5, TimeUnit.MINUTES);
//
//        // 4. 根据 accessKey 查询 secretKey（你原来的代码，不变）
//        String sql = "select user_id, secret_key from app where access_key = ? and status = 1";
//        String secretKey;
//        long userId;
//        try {
//            Map<String, Object> map = jdbcTemplate.queryForMap(sql, accessKey);
//            secretKey = (String) map.get("secret_key");
//            userId = Long.parseLong(map.get("user_id").toString()); // ← 这就是 userId！！！
//        } catch (EmptyResultDataAccessException e) {
//            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            return exchange.getResponse().setComplete();
//        }
//
//        // 5. 验签（你原来的逻辑，完全不变）
//        Map<String, String> paramMap = new TreeMap<>();
//        paramMap.put("accessKey", accessKey);
//        paramMap.put("timestamp", timestamp);
//        paramMap.put("nonce", nonce);
//        System.out.println("paramMap: " + paramMap);
//        String computedSign = SignUtil.sign(paramMap, secretKey);
//        if (!computedSign.equals(sign)) {
//            System.out.println("验签失败,401");
//            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            return exchange.getResponse().setComplete();
//        }
//
//        // ====================== 发送日志到 MQ ======================
//        String path = exchange.getRequest().getPath().value();
//        String interfacePath = path.replaceFirst("^/api", "");
//        // 例如：/api/random/text
//        String sql2 = "select id from api_interface where url = ? and status = 1";
//        Integer interfaceId;
//        try {
//            interfaceId = jdbcTemplate.queryForObject(sql2, new Object[]{interfacePath}, Integer.class);
//        } catch (EmptyResultDataAccessException e) {
//            // 接口不存在
//            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
//            return exchange.getResponse().setComplete();
//        }
//
//        //------------调用 api-admin 的权限校验接口
//        // 构造请求参数
//        Map<String, Object> params = new HashMap<>();
//        params.put("userId", userId);
//        params.put("interfaceId", interfaceId);
//
//        // 调用 api-admin 的权限校验接口
//        Mono<Void> mono =webClient.post()
//                .uri("/userInterfaceAuth/callApi")
//                .bodyValue(params)
//                .retrieve()
//                .bodyToMono(SaResult.class)
//                .flatMap(result -> {
//                    if (result.isSuccess()) {
//                        System.out.println("调用 api-admin 的权限校验接口成功");
//                        // 校验通过 → 放行
//                        return chain.filter(exchange);
//                    } else {
//                        // 校验失败 → 返回错误
//                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
//                        return exchange.getResponse().setComplete().log(result.getMsg());
//                    }
//                });
//        // 必须订阅才会真正执行HTTP请求
////        mono.subscribe();           // 方式1：订阅（异步，不阻塞）
//// 或
//        String result = mono.block().toString(); // 方式2：阻塞等待结果（同步）
//        System.out.println("result: " + result);
//        // ====================== 存储日志到 MQ ======================
//        ServerHttpRequest request = exchange.getRequest();
//        String requestIp = IpUtil.getClientIp(request);
//
//        ApiCallLogDTO log = new ApiCallLogDTO();
//        log.setUserId(userId);          // 你前面从DB查出来的用户ID
//        if (interfaceId != null) {
//            log.setInterfaceId(Long.valueOf(interfaceId)); // 你解析的接口 ID
//        }
//        log.setAccessKey(accessKey);
//        log.setRequestIp(requestIp);
//        log.setStatus(1);
////        log.setCreateTime(new Date());
//        // 异步发送！不阻塞网关！
//        rabbitTemplate.convertAndSend(logQueue, log);
//        System.out.println("log: " + log);
//        System.out.println("logQueue: " + logQueue);
//        System.out.println("验签成功");
//        // 6. 放行
//        return chain.filter(exchange);
//    }
//
//    @Override
//    public int getOrder() {
//        return -100;
//    }
//}