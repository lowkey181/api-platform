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
    @Value("${spring.rabbitmq.queue.log}")
    private String logQueue;
    @Autowired
    private BlackListMapper blackListMapper;
    // 注入 WebClient
    private final WebClient webClient = WebClient.create("http://localhost:9002");
    // ========== 【新增】用于存储改写后的请求体和响应体 ==========
    private String modifiedRequestBody;
    private String modifiedResponseBody;
    private String errorMsg;
    private long useTime;
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
            Map<String, Object> map = jdbcTemplate.queryForMap(sql, accessKey);
            secretKey = (String) map.get("secret_key");
            userId = Long.parseLong(map.get("user_id").toString());
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
                .header("Content-Type", "application/json")
                .header("Authorization", Authorization)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(SaResult.class)
                .flatMap(result -> {
                    if (result.isSuccess()) {
                        log.info("权限校验成功");
                        return chain.filter(exchange)
                                .doOnSuccess(v -> {//this加不加都行
                                    this.modifiedResponseBody = (String) exchange.getAttributes().get(GatewayConfig.MODIFIED_RESPONSE_BODY_ATTR);
                                    if (modifiedResponseBody != null) {
                                        log.info("SignFilter 获取到改写后的响应体：{}", modifiedResponseBody);
                                    }
                                });
                    } else {
                        log.error("权限校验失败");
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }
                })
                // ========== 3. 【整个请求全部结束后】才统计时间 + 发日志 ==========
                .doFinally(signalType -> {
                    // 总耗时
                    useTime = System.currentTimeMillis() - startTime;
                    // ========== 从 Attribute 中获取处理后的请求体 ==========
                    this.modifiedRequestBody = (String) exchange.getAttributes().get(GatewayConfig.MODIFIED_REQUEST_BODY_ATTR);
                    if (modifiedRequestBody != null) {
                        log.info("SignFilter 获取到改写后的请求体：{}", modifiedRequestBody);
                    }

                    // ---------- 权限成功才发送日志 ----------
                    if (exchange.getResponse().getStatusCode() == HttpStatus.OK) {
                        System.out.println(modifiedRequestBody+modifiedResponseBody);
                        sendCallLogToMQ(exchange, userId, interfaceId, accessKey, useTime, modifiedResponseBody,modifiedRequestBody,null);
                    }
                })
                .onErrorResume(e -> {
                    log.error("服务异常", e);
                    errorMsg=e.getMessage();
                    sendCallLogToMQ(exchange, userId, interfaceId, accessKey, useTime, modifiedResponseBody,modifiedRequestBody,errorMsg);
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                });

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
        rabbitTemplate.convertAndSend(logQueue, logDto);
        log.info("已发送接口调用日志到 MQ: {}", logDto);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}