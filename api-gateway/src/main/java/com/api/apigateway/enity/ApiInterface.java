package com.api.apigateway.enity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("api_interface")
public class ApiInterface {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String name;
    private String url;
    private String method;
    private String description;
    private String category;
    private String tags;
    private String version;
    private Long authorId;
    private String authorName;
    private Integer timeoutMs;
    private Integer retryCount;
    private Integer pricingType;
    private Integer dailyLimit;
    private String responseType;
    private Integer isOfficial;
    private Integer status;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private BigDecimal minPrice;
}
