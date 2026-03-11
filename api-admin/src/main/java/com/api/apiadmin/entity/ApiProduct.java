package com.api.apiadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("api_product")
public class ApiProduct {
    @TableId(type = IdType.AUTO, value = "id")
    private Long id;

    private Long interfaceId;       // 对应哪个API接口（api_interface.id）
    private String productName;     // 套餐名：例如“随机文本-100次”
    private Long callCount;      // 包含多少次调用
    private BigDecimal price;      // 价格：10.00元

    private Integer status;        // 0下架 1上架
    private String remark;          // 描述

    private Date createTime;
    private Long productId;
}