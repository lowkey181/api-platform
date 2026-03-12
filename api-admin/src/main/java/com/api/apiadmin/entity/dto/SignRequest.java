package com.api.apiadmin.entity.dto;

import lombok.Data;

@Data
public class SignRequest {
    private String accessKey;
    private String timestamp;
    private String nonce;
}
