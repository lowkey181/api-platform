package com.api.apigateway.utils;

import cn.hutool.core.util.StrUtil;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;
import java.util.TreeMap;

/**
 * 签名工具类（企业级标准）
 */
public class SignUtil {

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
}