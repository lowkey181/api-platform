package com.api.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class GatewayConfig {
    public static final String MODIFIED_REQUEST_BODY_ATTR = "modifiedRequestBody";
    public static final String MODIFIED_RESPONSE_BODY_ATTR = "modifiedResponseBody";

    @Value("${api.gateway.backend-service-uri:lb://api-service}")
    private String backendServiceUri;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("modify_body_route", r -> r
                        .path("/api/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .modifyRequestBody(String.class, String.class,
                                        MediaType.APPLICATION_JSON_VALUE,
                                        rewriteRequestBody())
                                .modifyResponseBody(String.class, String.class,
                                        MediaType.APPLICATION_JSON_VALUE,
                                        rewriteResponseBody())
                        )
                        .uri(backendServiceUri)
                )
                .build();
    }

    /**
     * 请求体改写 Bean
     * Bean 名称必须与 YAML 中配置的 rewriteRequestBody 一致
     */
    @Bean
    public RewriteFunction<String, String> rewriteRequestBody() {
        return new RewriteFunction<String, String>() {
            @Override
            public Mono<String> apply(ServerWebExchange exchange, String body) {
                // 获取请求信息
                String path = exchange.getRequest().getPath().value();
                String method = String.valueOf(exchange.getRequest().getMethod());

                log.info("[请求] {} {}，原始请求体：{}", method, path, body);

                // 处理请求体


                if (body == null || body.trim().isEmpty()) {
                    log.debug("请求体为空，返回默认空 JSON");
                    return Mono.just("{}");
                }
                String modified = body.replace("}",
                        ",\"gatewayTime\":" + System.currentTimeMillis() + "}");

                // Base64 解码
                // String decoded = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);

                log.info("[请求] 修改后请求体：{}", modified);
                // 将修改后的请求体存入 Attribute，供 SignFilter 使用
                exchange.getAttributes().put(MODIFIED_REQUEST_BODY_ATTR, modified);

                return Mono.just(modified);
            }
        };
    }

    /**
     * 响应体改写 Bean
     * Bean 名称必须与 YAML 中配置的 rewriteResponseBody 一致
     */
    @Bean
    public RewriteFunction<String, String> rewriteResponseBody() {
        return new RewriteFunction<String, String>() {
            @Override
            public Mono<String> apply(ServerWebExchange exchange, String body) {
                String path = exchange.getRequest().getPath().value();

                log.info("[响应] {} 原始响应体：{}", path, body);

                //处理响应体

                //包装统一响应格式
                String wrapped = String.format(
                        "{\"code\":200,\"success\":true,\"data\":\"%s\",\"timestamp\":%d,\"path\":\"%s\"}",
                        body,
                        System.currentTimeMillis(),
                        path
                );



                log.info("[响应] 修改后响应体：{}", wrapped);
                // 将修改后的响应体存入 Attribute，供 SignFilter 使用
                exchange.getAttributes().put(MODIFIED_RESPONSE_BODY_ATTR, wrapped);

                return Mono.just(wrapped);
            }
        };
    }
}