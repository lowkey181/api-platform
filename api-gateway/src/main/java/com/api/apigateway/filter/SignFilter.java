package com.api.apigateway.filter;

import cn.hutool.core.util.StrUtil;
import com.api.apigateway.config.GatewayConfig;
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
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
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
    @Value("${spring.rabbitmq.exchange.log}")
    private String logExchange;
    @Value("${spring.rabbitmq.routing.log}")
    private String logRoutingKey;
    @Autowired
    private BlackListMapper blackListMapper;
    @Autowired
    private ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;
    @Value("${api.gateway.auth.base-url:http://localhost:9002}")
    private String authBaseUrl;
    @Value("${api.gateway.auth.timeout-ms:1500}")
    private long authTimeoutMs;
    @Value("${api.gateway.cache.app-auth-ttl-seconds:300}")
    private long appAuthCacheTtlSeconds;
    @Value("${api.gateway.cache.interface-ttl-seconds:300}")
    private long interfaceCacheTtlSeconds;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // ==========  全局记录开始时间（整个请求的起点） ==========
        long startTime = System.currentTimeMillis();
        // 获取 4 个请求头
        String accessKey = exchange.getRequest().getHeaders().getFirst("accessKey");
        String sign = exchange.getRequest().getHeaders().getFirst("sign");
        String timestamp = exchange.getRequest().getHeaders().getFirst("timestamp");
        String nonce = exchange.getRequest().getHeaders().getFirst("nonce");
        String Authorization= exchange.getRequest().getHeaders().getFirst("Authorization");

        log.info("接收到请求: accessKey={}, sign={}, timestamp={}, nonce={}", accessKey, sign, timestamp, nonce);
        //  =============黑名单检查=========
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
        // 1. 判空
        if (StrUtil.isBlank(accessKey) || StrUtil.isBlank(sign) || StrUtil.isBlank(timestamp) || StrUtil.isBlank(nonce)) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }

        // 2. 时间戳过期校验
        long currentTime = System.currentTimeMillis();
        long reqTime = Long.parseLong(timestamp);
        if (Math.abs(currentTime - reqTime) > 60000 * 5) { // 超过5分钟过期
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 3. nonce 防重放
        String nonceKey = "sign:nonce:" + nonce;
        Boolean hasNonce = redisTemplate.hasKey(nonceKey);
        if (hasNonce) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        redisTemplate.opsForValue().set(nonceKey, "1", 5, TimeUnit.MINUTES);

        // 4. 根据 accessKey 查询 secretKey
        String sql = "select user_id, secret_key from app where access_key = ? and status = 1";
        String secretKey;
        long userId;
        try {
            AppAuthInfo appAuthInfo = queryAppAuthInfo(sql, accessKey);
            secretKey = appAuthInfo.secretKey();
            userId = appAuthInfo.userId();
        } catch (EmptyResultDataAccessException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 5. 验签
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
            interfaceId = queryInterfaceId(sql2, interfacePath);
        } catch (EmptyResultDataAccessException e) {
            // 接口不存在
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }

        //------------调用 api-admin 的权限校验接口
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("interfaceId", interfaceId);
        String modifiedRequestBody = (String) exchange.getAttributes().get(GatewayConfig.MODIFIED_REQUEST_BODY_ATTR);
        String[] responseBodyHolder = new String[]{null};
        String[] errorHolder = new String[]{null};
        WebClient webClient = WebClient.builder().baseUrl(authBaseUrl).build();
        ReactiveCircuitBreaker circuitBreaker = circuitBreakerFactory.create("adminAuth");

        Mono<SaResult> authMono = webClient.post()
                .uri("/userInterfaceAuth/callApi")
                .header("Content-Type", "application/json")
                .header("Authorization", Authorization)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(SaResult.class)
                .timeout(Duration.ofMillis(authTimeoutMs));

        return circuitBreaker.run(authMono, throwable -> {
                    log.warn("权限校验降级触发: {}", throwable.getMessage());
                    return Mono.just(SaResult.error("鉴权服务暂不可用"));
                })
                .flatMap(result -> {
                    if (result != null && result.isSuccess()) {
                        log.info("权限校验成功");
                        return chain.filter(exchange).doOnSuccess(v -> {
                            responseBodyHolder[0] = (String) exchange.getAttributes().get(GatewayConfig.MODIFIED_RESPONSE_BODY_ATTR);
                            if (responseBodyHolder[0] != null) {
                                log.info("SignFilter 获取到改写后的响应体：{}", responseBodyHolder[0]);
                            }
                        });
                    }
                    log.error("权限校验失败: {}", result == null ? "null result" : result.getMsg());
                    if (result != null && "鉴权服务暂不可用".equals(result.getMsg())) {
                        return writeErrorResponse(exchange, HttpStatus.SERVICE_UNAVAILABLE, "鉴权服务暂不可用");
                    } else {
                        return writeErrorResponse(exchange, HttpStatus.FORBIDDEN, "权限校验失败");
                    }
                })
                // ========== 3. 【整个请求全部结束后】才统计时间 + 发日志 ==========
                .doFinally(signalType -> {
                    // 总耗时
                    long useTime = System.currentTimeMillis() - startTime;
                    if (modifiedRequestBody != null) {
                        log.info("SignFilter 获取到改写后的请求体：{}", modifiedRequestBody);
                    }

                    // ---------- 权限成功才发送日志 ----------
                    if (exchange.getResponse().getStatusCode() == HttpStatus.OK) {
                        sendCallLogToMQ(exchange, userId, interfaceId, accessKey, useTime, responseBodyHolder[0], modifiedRequestBody, null);
                    }
                })
                .onErrorResume(e -> {
                    log.error("服务异常", e);
                    errorHolder[0] = e.getMessage();
                    long useTime = System.currentTimeMillis() - startTime;
                    sendCallLogToMQ(exchange, userId, interfaceId, accessKey, useTime, responseBodyHolder[0], modifiedRequestBody, errorHolder[0]);
                    return writeErrorResponse(exchange, HttpStatus.SERVICE_UNAVAILABLE, "鉴权服务异常");
                });

    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":%d,\"msg\":\"%s\"}", status.value(), message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private AppAuthInfo queryAppAuthInfo(String sql, String accessKey) {
        String cacheKey = "cache:app:auth:" + accessKey;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            String[] parts = cached.split(":", 2);
            if (parts.length == 2) {
                return new AppAuthInfo(Long.parseLong(parts[0]), parts[1]);
            }
        }
        Map<String, Object> map = jdbcTemplate.queryForMap(sql, accessKey);
        long userId = Long.parseLong(map.get("user_id").toString());
        String secretKey = (String) map.get("secret_key");
        redisTemplate.opsForValue().set(cacheKey, userId + ":" + secretKey, appAuthCacheTtlSeconds, TimeUnit.SECONDS);
        return new AppAuthInfo(userId, secretKey);
    }

    private Integer queryInterfaceId(String sql, String interfacePath) {
        String cacheKey = "cache:interface:id:" + interfacePath;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            return Integer.parseInt(cached);
        }
        Integer interfaceId = jdbcTemplate.queryForObject(sql, new Object[]{interfacePath}, Integer.class);
        if (interfaceId != null) {
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(interfaceId), interfaceCacheTtlSeconds, TimeUnit.SECONDS);
        }
        return interfaceId;
    }

    private record AppAuthInfo(long userId, String secretKey) {
    }

    /**
     * 发送调用日志到 MQ
     */
    private void sendCallLogToMQ(ServerWebExchange exchange, long userId,
                                 Integer interfaceId, String accessKey,
                                 long useTime,String modifiedResponseBody,
                                 String modifiedRequestBody,String errorMsg) {
        ServerHttpRequest request = exchange.getRequest();
        String requestIp = IpUtil.getClientIp(request);

        ApiCallLogDTO logDto = new ApiCallLogDTO();
        logDto.setUserId(userId);
        if (interfaceId != null) {
            logDto.setInterfaceId(Long.valueOf(interfaceId));
        }
        logDto.setAccessKey(accessKey);
        logDto.setRequestIp(requestIp);
        if(errorMsg!= null){
            logDto.setErrorMsg(errorMsg);
            logDto.setStatus(0);
        }
        logDto.setStatus(1);
        logDto.setUseTime(useTime);
        logDto.setRequestParams(modifiedRequestBody);
        logDto.setResponseResult(modifiedResponseBody);

        // 异步发送！不阻塞网关！
        rabbitTemplate.convertAndSend(logExchange, logRoutingKey, logDto);
        log.info("已发送接口调用日志到 MQ: {}", logDto);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}