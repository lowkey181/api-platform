package com.api.apigateway.filter;

import com.api.apigateway.utils.RedisLimitUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import jakarta.annotation.Resource;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    @Resource
    private RedisLimitUtil redisLimitUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String accessKey = exchange.getRequest().getHeaders().getFirst("accessKey");
        String key = "limit:api:" + accessKey;

        // 3次/60秒
        boolean allow = redisLimitUtil.tryAcquire(key, 3, 60);
        if (!allow) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -90;
    }
}