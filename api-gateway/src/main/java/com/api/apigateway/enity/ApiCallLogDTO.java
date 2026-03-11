package com.api.apigateway.enity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ApiCallLogDTO  {

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
