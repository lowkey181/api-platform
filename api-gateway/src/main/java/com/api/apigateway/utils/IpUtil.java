package com.api.apigateway.utils;

import org.springframework.http.server.reactive.ServerHttpRequest;
import java.net.InetSocketAddress;

public class IpUtil {

    /**
     * 获取客户端真实IP（网关专用）
     */
    public static String getClientIp(ServerHttpRequest request) {
        try {
            // 真实IP
            String realIp = request.getHeaders().getFirst("X-Real-IP");
            if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
                return realIp;
            }

            // 多级代理
            String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank() && !"unknown".equalsIgnoreCase(forwarded)) {
                return forwarded.split(",")[0].trim();
            }

            // 兜底
            InetSocketAddress address = request.getRemoteAddress();
            if (address != null) {
                return address.getAddress().getHostAddress();
            }
        } catch (Exception e) {
            // 忽略异常
        }

        return "unknown";
    }
}