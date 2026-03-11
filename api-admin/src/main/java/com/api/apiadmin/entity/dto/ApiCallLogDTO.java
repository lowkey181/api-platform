package com.api.apiadmin.entity.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiCallLogDTO {

    private Long userId;
    private String accessKey;
    private Long interfaceId;
    private String requestIp;
    private String requestParams;
    private String responseResult;
    private Long useTime;
    private Integer status;
    private String errorMsg;
    private LocalDateTime createTime;

}
