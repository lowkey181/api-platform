package com.api.apiadmin.controller;

import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.ApiInterface;
import com.api.apiadmin.mapper.ApiInterfaceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequestMapping("/invoke")
public class InvokeController {

    @Autowired
    private ApiInterfaceMapper apiInterfaceMapper;
    
    @Autowired
    private RestTemplate restTemplate;

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("/call")
    public Result getInterfaceUrl(@RequestParam Integer interfaceId) {
        try {
            // 1. 根据 interfaceId 获取接口信息
            ApiInterface apiInterface = apiInterfaceMapper.selectById(interfaceId);
            if (apiInterface == null) {
                return Result.error("接口不存在");
            }
            
            if (apiInterface.getStatus() == 0) {
                return Result.error("接口已禁用");
            }
            
            // 2. 返回接口 URL（前端通过网关调用）
            String url = apiInterface.getUrl();
            log.info("返回接口 URL: {}", url);
            
            return Result.ok(url);
        } catch (Exception e) {
            log.error("获取接口 URL 失败", e);
            return Result.error("系统错误: " + e.getMessage());
        }
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

            // 1. 根据 interfaceId 获取接口信息
            ApiInterface apiInterface = apiInterfaceMapper.selectById(interfaceId);
            if (apiInterface == null) {
                return Result.error("接口不存在");
            }
            
            if (apiInterface.getStatus() == 0) {
                return Result.error("接口已禁用");
            }
            
            // 2. 通过网关调用接口
            String interfaceUrl = apiInterface.getUrl();
            String gatewayUrl = "http://localhost:8090/api" + interfaceUrl;
            
            log.info("通过网关调用接口: {}", gatewayUrl);
            log.info("请求头 - accessKey: {}, timestamp: {}, nonce: {}", accessKey, timestamp, nonce);
            
            // 3. 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("accessKey", accessKey);
            headers.set("sign", sign);
            headers.set("timestamp", timestamp);
            headers.set("nonce", nonce);
            headers.set("Authorization", Authorization);



            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 4. 调用网关
            ResponseEntity<String> response;
            try {
                if ("GET".equalsIgnoreCase(apiInterface.getMethod())) {
                    response = restTemplate.exchange(gatewayUrl, HttpMethod.GET, entity, String.class);
                } else if ("POST".equalsIgnoreCase(apiInterface.getMethod())) {
                    response = restTemplate.exchange(gatewayUrl, HttpMethod.POST, entity, String.class);
                } else {
                    return Result.error("不支持的请求方法: " + apiInterface.getMethod());
                }
                
                log.info("网关返回状态码: {}", response.getStatusCode());
                return Result.ok(response.getBody());
            } catch (Exception e) {
                log.error("调用网关失败: {}", e.getMessage(), e);
                return Result.error("调用接口失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("系统错误", e);
            return Result.error("系统错误: " + e.getMessage());
        }
    }
}
