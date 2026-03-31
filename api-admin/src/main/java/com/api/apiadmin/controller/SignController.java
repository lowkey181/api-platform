package com.api.apiadmin.controller;

import cn.hutool.core.util.StrUtil;
import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.App;
import com.api.apiadmin.service.AppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/sign")
public class SignController {

    @Autowired
    private AppService appService;

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("/generate")
    public Result generate() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        // 获取用户的所有应用
        List<App> apps = appService.getByUserId(userId);
        
        if (apps == null || apps.isEmpty()) {
            return Result.error("用户没有应用，请先创建应用");
        }
        
        // 取第一个应用
        App app = apps.get(0);
        String accessKey = app.getAccessKey();
        String secretKey = app.getSecretKey();
        
        // 生成签名参数
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = generateNonce();
        Map<String, String> params = new HashMap<>();
        params.put("timestamp", timestamp);
        params.put("nonce", nonce);
        params.put("accessKey", accessKey);

        String sign = sign(params, secretKey);
        Map<String, Object> data = new HashMap<>();
        data.put("accessKey", accessKey);
        data.put("timestamp", timestamp);
        data.put("nonce", nonce);
        data.put("sign", sign);
        return Result.ok(data);
    }

    /**
     * 生成签名
     */
    public static String sign(Map<String, String> paramMap, String secretKey) {
        try {
            // 1. 参数排序（TreeMap自动排序）
            Map<String, String> sortedMap = new TreeMap<>(paramMap);
            // 2. 拼接字符串 k=v&k=v
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(value)) {
                    sb.append(key).append("=").append(value).append("&");
                }
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            String content = sb.toString();
            // 3. HMAC-SHA256 加密
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(content.getBytes());
            // 4. 转16进制
            return byte2Hex(bytes);
        } catch (Exception e) {
            throw new RuntimeException("签名生成失败", e);
        }
    }
    /**
     * 字节转16进制
     */
    private static String byte2Hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                sb.append("0");
            }
            sb.append(hex);
        }
        return sb.toString().toLowerCase();
    }
    private String generateNonce() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
