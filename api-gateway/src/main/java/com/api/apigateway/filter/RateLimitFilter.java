package com.api.apigateway.filter;

import com.api.apigateway.utils.RedisLimitUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import jakarta.annotation.Resource;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    @Resource
    private RedisLimitUtil redisLimitUtil;
    @Value("${api.gateway.default-limit-count:100}")
    private int defaultLimitCount;
    @Value("${api.gateway.default-limit-period:60}")
    private int defaultLimitPeriod;
    @Value("${api.gateway.limit-dimension:accessKey}")
    private String limitDimension;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String accessKey = exchange.getRequest().getHeaders().getFirst("accessKey");
        String key = buildLimitKey(exchange, accessKey);

        boolean allow = redisLimitUtil.tryAcquire(key, defaultLimitCount, defaultLimitPeriod);
        if (!allow) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    private String buildLimitKey(ServerWebExchange exchange, String accessKey) {
        String path = exchange.getRequest().getPath().value();
        String ip = "unknown";
        if (exchange.getRequest().getRemoteAddress() != null
                && exchange.getRequest().getRemoteAddress().getAddress() != null) {
            ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        if ("ip".equalsIgnoreCase(limitDimension)) {
            return "limit:ip:" + ip;
        }
        if ("route".equalsIgnoreCase(limitDimension)) {
            return "limit:route:" + path;
        }
        // 如果 accessKey 非空（有有效文本），则使用 accessKey 作为身份标识
        //如果 accessKey 为空，则降级使用 ip 地址作为身份标识
        String identity = StringUtils.hasText(accessKey) ? accessKey : ip;
        return "limit:accessKey:" + identity;
    }

    @Override
    public int getOrder() {
        return -90;
    }
}