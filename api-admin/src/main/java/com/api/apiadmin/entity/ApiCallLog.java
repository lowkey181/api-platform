package com.api.apiadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("api_call_log")
public class ApiCallLog implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
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
