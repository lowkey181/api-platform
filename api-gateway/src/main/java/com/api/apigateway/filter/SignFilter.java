package com.api.apigateway.filter;

import cn.hutool.core.util.StrUtil;
import com.api.apigateway.config.SaResult;
import com.api.apigateway.enity.ApiCallLogDTO;
import com.api.apigateway.enity.Blacklist;
import com.api.apigateway.mapper.BlackListMapper;
import com.api.apigateway.utils.IpUtil;
import com.api.apigateway.utils.SignUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SignFilter implements GlobalFilter, Ordered {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Value("${spring.rabbitmq.queue.log}")
    private String logQueue;
    @Autowired
    private BlackListMapper blackListMapper;
    // 注入 WebClient
    private final WebClient webClient = WebClient.create("http://localhost:9002");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取你原来的 4 个请求头（不变！）
        String accessKey = exchange.getRequest().getHeaders().getFirst("accessKey");
        String sign = exchange.getRequest().getHeaders().getFirst("sign");
        String timestamp = exchange.getRequest().getHeaders().getFirst("timestamp");
        String nonce = exchange.getRequest().getHeaders().getFirst("nonce");

        log.info("接收到请求: accessKey={}, sign={}, timestamp={}, nonce={}", accessKey, sign, timestamp, nonce);
        // 1. =============黑名单检查=========
        log.info("开始检查黑名单");
        ServerHttpRequest request = exchange.getRequest();
        String clientIp = IpUtil.getClientIp(request);

        // 查 IP 是否在黑名单（且状态=1）
        Boolean isIpBlack = redisTemplate.hasKey("blacklist:ip:" + clientIp);
//        boolean isIpBlack = blackListMapper.exists(new QueryWrapper<Blacklist>()
//                .eq("ip", clientIp)
//                .eq("status", 1));

        // 查 accessKey 是否在黑名单（且状态=1）
        boolean isAkBlack = false;
        if (accessKey != null) {
            isAkBlack = redisTemplate.hasKey("blacklist:accessKey:" + accessKey);
        }
//        boolean isAkBlack = false;
//        if (accessKey != null) {
//            isAkBlack = blackListMapper.exists(new QueryWrapper<Blacklist>()
//                    .eq("access_key", accessKey)
//                    .eq("status", 1));
//        }


        // 任意命中 → 直接拒绝
        if (isIpBlack || isAkBlack) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        log.info("黑名单检查结束,不在黑名单之列");
        //====================================
        // 1. 判空（不变）
        if (StrUtil.isBlank(accessKey) || StrUtil.isBlank(sign) || StrUtil.isBlank(timestamp) || StrUtil.isBlank(nonce)) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }

        // 2. 时间戳过期校验（新增！）
        long currentTime = System.currentTimeMillis();
        long reqTime = Long.parseLong(timestamp);
        if (Math.abs(currentTime - reqTime) > 60000 * 5) { // 超过5分钟过期
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 3. nonce 防重放（核心！新增！）
        String nonceKey = "sign:nonce:" + nonce;
        Boolean hasNonce = redisTemplate.hasKey(nonceKey);
        if (hasNonce) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        redisTemplate.opsForValue().set(nonceKey, "1", 5, TimeUnit.MINUTES);

        // 4. 根据 accessKey 查询 secretKey（你原来的代码，不变）
        String sql = "select user_id, secret_key from app where access_key = ? and status = 1";
        String secretKey;
        long userId;
        try {
            Map<String, Object> map = jdbcTemplate.queryForMap(sql, accessKey);
            secretKey = (String) map.get("secret_key");
            userId = Long.parseLong(map.get("user_id").toString()); // ← 这就是 userId！！！
        } catch (EmptyResultDataAccessException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 5. 验签（你原来的逻辑，完全不变）
        Map<String, String> paramMap = new TreeMap<>();
        paramMap.put("accessKey", accessKey);
        paramMap.put("timestamp", timestamp);
        paramMap.put("nonce", nonce);
        log.debug("待验签参数: {}", paramMap);
        String computedSign = SignUtil.sign(paramMap, secretKey);
        if (!computedSign.equals(sign)) {
            log.warn("验签失败: computedSign={}, receivedSign={}", computedSign, sign);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // ====================== 发送日志到 MQ ======================
        String path = exchange.getRequest().getPath().value();
        String interfacePath = path.replaceFirst("^/api", "");
        // 例如：/api/random/text
        String sql2 = "select id from api_interface where url = ? and status = 1";
        Integer interfaceId;
        try {
            interfaceId = jdbcTemplate.queryForObject(sql2, new Object[]{interfacePath}, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            // 接口不存在
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }

        //------------调用 api-admin 的权限校验接口
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("interfaceId", interfaceId);

        return webClient.post()
                .uri("/userInterfaceAuth/callApi")
                .header("Authorization")
                .bodyValue(params)
                .retrieve()
                .bodyToMono(SaResult.class)
                .flatMap(result -> {
                    if (result.isSuccess()) {
                        log.info("调用权限校验成功: userId={}, interfaceId={}", userId, interfaceId);
                        
                        // ====================== 存储日志到 MQ ======================
                        sendCallLogToMQ(exchange, userId, interfaceId, accessKey);
                        
                        // 校验通过 → 放行
                        return chain.filter(exchange);
                    } else {
                        log.warn("权限校验失败: {}", result.getMsg());
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }
                })
                .onErrorResume(e -> {
                    log.error("权限校验服务异常: {}", e.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                });
    }

    /**
     * 发送调用日志到 MQ
     */
    private void sendCallLogToMQ(ServerWebExchange exchange, long userId, Integer interfaceId, String accessKey) {
        ServerHttpRequest request = exchange.getRequest();
        String requestIp = IpUtil.getClientIp(request);

        ApiCallLogDTO logDto = new ApiCallLogDTO();
        logDto.setUserId(userId);
        if (interfaceId != null) {
            logDto.setInterfaceId(Long.valueOf(interfaceId));
        }
        logDto.setAccessKey(accessKey);
        logDto.setRequestIp(requestIp);
        logDto.setStatus(1);

        // 异步发送！不阻塞网关！
        rabbitTemplate.convertAndSend(logQueue, logDto);
        log.info("已发送接口调用日志到 MQ: {}", logDto);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}