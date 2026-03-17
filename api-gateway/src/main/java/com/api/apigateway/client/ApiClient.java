package com.api.apigateway.client;

import cn.hutool.core.util.RandomUtil;
import com.api.apigateway.utils.SignUtil;
import cn.hutool.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ApiClient {
    private final String accessKey;
    private final String secretKey;
    private final String gatewayUrl;

    public ApiClient(String accessKey, String secretKey, String gatewayUrl) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.gatewayUrl = gatewayUrl;
    }

    public String request(String path) {
        Map<String, String> headers = createSignHeaders();
        return HttpRequest.get(gatewayUrl + path)
                .addHeaders(headers)
                .execute().body();
    }

    private Map<String, String> createSignHeaders() {
        Map<String, String> paramMap = new TreeMap<>();
        paramMap.put("accessKey", accessKey);
        paramMap.put("timestamp", String.valueOf(System.currentTimeMillis()));
//        paramMap.put("nonce", RandomUtil.randomString(6));
        paramMap.put("nonce", java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16));//随机数,保持一致
        String sign = SignUtil.sign(paramMap, secretKey);

        Map<String, String> headers = new HashMap<>(paramMap);
        headers.put("sign", sign);
        return headers;
    }
}