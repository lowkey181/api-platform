package com.api.apiadmin.controller.gateway;

import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.entity.App;
import com.api.apiadmin.entity.dto.SignRequest;
import com.api.apiadmin.service.AppService;
import com.api.apiadmin.util.HmacSHA256Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GatewayController {
    @Autowired
    private AppService appService;

    @PostMapping("/sign/generate")
    public SaResult generateSign(@RequestBody SignRequest request) {
        String accessKey = request.getAccessKey();
        String timestamp = request.getTimestamp();
        String nonce = request.getNonce();

        // 根据 accessKey 查 secretKey
        App app = appService.getByAccessKey(accessKey);
        String secretKey = app.getSecretKey();

        // 生成签名
        String str = accessKey + timestamp + nonce;
        String sign = HmacSHA256Utils.encrypt(str, secretKey);

        // 把 sign 返回给前端
        return SaResult.ok(sign);
    }
}
