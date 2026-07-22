package com.api.apiadmin.controller;

import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.ApiInterface;
import com.api.apiadmin.entity.App;
import com.api.apiadmin.mapper.ApiInterfaceMapper;
import com.api.apiadmin.service.AppService;
import com.api.apiadmin.util.HmacSHA256Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/invoke")
public class InvokeController {

    @Autowired
    private ApiInterfaceMapper apiInterfaceMapper;
    @Autowired
    private AppService appService;

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("/call")
    public Result getInterfaceUrl(@RequestParam Integer interfaceId) {
        ApiInterface apiInterface = apiInterfaceMapper.selectById(interfaceId);
        if (apiInterface == null) return Result.error("接口不存在");
        if (apiInterface.getStatus() == 0) return Result.error("接口已禁用");
        return Result.ok(apiInterface.getUrl());
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("/invoke")
    public Result invokeInterface(
            @RequestParam Integer interfaceId,
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam String timestamp,
            @RequestParam String nonce,
            @RequestParam String Authorization,
            @RequestParam(required = false) String body) {
        try {
            ApiInterface apiInterface = apiInterfaceMapper.selectById(interfaceId);
            if (apiInterface == null) return Result.error("接口不存在");
            if (apiInterface.getStatus() == 0) return Result.error("接口已禁用");

            String interfaceUrl = apiInterface.getUrl();
            String targetUrl;
            if (apiInterface.getUrlType() != null && apiInterface.getUrlType() == 2) {
                targetUrl = interfaceUrl;
            } else {
                targetUrl = "http://localhost:8090/api" + interfaceUrl;
            }
            log.info("通过网关调用接口: {}", targetUrl);

            HttpHeaders headers = new HttpHeaders();
            if (apiInterface.getUrlType() == null || apiInterface.getUrlType() != 2) {
                headers.set("accessKey", accessKey);
                headers.set("sign", sign);
                headers.set("timestamp", timestamp);
                headers.set("nonce", nonce);
                headers.set("Authorization", Authorization);
            }
            headers.set("Accept-Encoding", "identity");

            org.springframework.web.reactive.function.client.WebClient.RequestBodySpec requestBodySpec;
            org.springframework.http.HttpMethod httpMethod = "GET".equalsIgnoreCase(apiInterface.getMethod())
                    ? HttpMethod.GET : HttpMethod.POST;
            requestBodySpec = WebClient.create()
                    .method(httpMethod)
                    .uri(targetUrl)
                    .headers(h -> h.addAll(headers));
            if (!"GET".equalsIgnoreCase(apiInterface.getMethod()) && body != null) {
                requestBodySpec.bodyValue(body);
            }
            java.util.concurrent.atomic.AtomicInteger statusCode = new java.util.concurrent.atomic.AtomicInteger(0);
            String responseBody = requestBodySpec.exchangeToMono(response -> {
                statusCode.set(response.statusCode().value());
                return response.bodyToMono(String.class);
            }).block();

            log.info("网关返回状态码: {}, 响应长度: {}", statusCode.get(),
                    responseBody != null ? responseBody.length() : 0);
            return Result.ok(java.util.Map.of(
                    "httpStatus", statusCode.get(),
                    "useTime", 0,
                    "response", responseBody
            ));
        } catch (Exception e) {
            log.error("调用网关失败: {}", e.getMessage(), e);
            return Result.error("调用接口失败: " + e.getMessage());
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/test")
    public Result testInterface(@RequestParam Integer interfaceId,
                                @RequestParam(required = false) String body,
                                HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            ApiInterface apiInterface = apiInterfaceMapper.selectById(interfaceId);
            if (apiInterface == null) return Result.error("接口不存在");
            if (apiInterface.getStatus() == 0) return Result.error("接口已禁用");

            String interfaceUrl = apiInterface.getUrl();
            String targetUrl;
            if (apiInterface.getUrlType() != null && apiInterface.getUrlType() == 2) {
                targetUrl = interfaceUrl; // 外部全URL，直接请求
            } else {
                targetUrl = "http://localhost:8090/api" + interfaceUrl; // 内部路径，走网关
            }
            log.info("测试接口: {}", targetUrl);

            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Optional<App> appOpt = appService.getByUserId(userId).stream()
                    .filter(a -> a.getStatus() == 1)
                    .findFirst();
            if (appOpt.isEmpty()) {
                return Result.error("请先在\"我的应用\"中创建应用");
            }
            App app = appOpt.get();

            String timestamp = String.valueOf(System.currentTimeMillis());
            String nonce = java.util.UUID.randomUUID().toString().substring(0, 8);
            String signStr = app.getAccessKey() + timestamp + nonce;
            String sign = HmacSHA256Utils.encrypt(signStr, app.getSecretKey());

            String authHeader = request.getHeader("Authorization");
            HttpHeaders headers = new HttpHeaders();
            if (apiInterface.getUrlType() == null || apiInterface.getUrlType() != 2) {
                headers.set("accessKey", app.getAccessKey());
                headers.set("sign", sign);
                headers.set("timestamp", timestamp);
                headers.set("nonce", nonce);
                headers.set("Authorization", authHeader);
            }
            headers.set("Accept-Encoding", "identity");

            if ("GET".equalsIgnoreCase(apiInterface.getMethod()) && body != null && !body.isBlank()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, String> queryMap = mapper.readValue(body, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, String>>() {});
                    StringBuilder qs = new StringBuilder();
                    for (var e : queryMap.entrySet()) {
                        if (qs.length() > 0) qs.append("&");
                        qs.append(java.net.URLEncoder.encode(e.getKey(), "UTF-8"))
                          .append("=")
                          .append(java.net.URLEncoder.encode(e.getValue(), "UTF-8"));
                    }
                    if (qs.length() > 0) {
                        targetUrl += (targetUrl.contains("?") ? "&" : "?") + qs;
                    }
                } catch (Exception e) {
                    log.warn("解析 query 参数失败: {}", e.getMessage());
                }
            }

            org.springframework.web.reactive.function.client.WebClient.RequestBodySpec requestBodySpec;
            org.springframework.http.HttpMethod httpMethod = "GET".equalsIgnoreCase(apiInterface.getMethod())
                    ? HttpMethod.GET : HttpMethod.POST;
            requestBodySpec = WebClient.create()
                    .method(httpMethod)
                    .uri(targetUrl)
                    .headers(h -> h.addAll(headers));
            if (!"GET".equalsIgnoreCase(apiInterface.getMethod()) && body != null) {
                requestBodySpec.bodyValue(body);
            }
            java.util.concurrent.atomic.AtomicInteger statusCode2 = new java.util.concurrent.atomic.AtomicInteger(0);
            String responseBody = requestBodySpec.exchangeToMono(response -> {
                statusCode2.set(response.statusCode().value());
                return response.bodyToMono(String.class);
            }).block();

            long useTime = System.currentTimeMillis() - startTime;
            log.info("测试完成 - 状态: {}, 耗时: {}ms, 响应长度: {}", statusCode2.get(), useTime,
                    responseBody != null ? responseBody.length() : 0);

            return Result.ok(java.util.Map.of(
                    "httpStatus", statusCode2.get(),
                    "useTime", useTime,
                    "response", responseBody
            ));
        } catch (Exception e) {
            long useTime = System.currentTimeMillis() - startTime;
            log.error("测试接口失败: {}", e.getMessage(), e);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Connection refused")) {
                errorMsg = "网关服务不可用，请确认网关是否启动";
            }
            return Result.ok(java.util.Map.of(
                    "httpStatus", 0,
                    "useTime", useTime,
                    "error", errorMsg
            ));
        }
    }
}
