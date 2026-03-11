package com.api.apigateway;

import com.api.apigateway.utils.SignUtil;

import java.util.Map;
import java.util.TreeMap;

public class SignTest {
    public static void main(String[] args) {
        // 从数据库里拿到的真实值
        String accessKey = "hTKv1UZyiFZz71x7lEfedu9H";
        String secretKey = "WXSSy6MsqVDhiA0Z3QA2B9sgaDbwQbJt";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "random_test_nonce";

        // 构建参数
        Map<String, String> paramMap = new TreeMap<>();
        paramMap.put("accessKey", accessKey);
        paramMap.put("timestamp", timestamp);
        paramMap.put("nonce", nonce);

        // 生成签名
        String sign = SignUtil.sign(paramMap, secretKey);
        System.out.println("accessKey: " + accessKey);
        System.out.println("timestamp: " + timestamp);
        System.out.println("nonce: " + nonce);
        System.out.println("sign: " + sign);
    }
}