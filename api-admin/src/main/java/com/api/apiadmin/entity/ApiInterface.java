package com.api.apiadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("api_interface")
public class ApiInterface {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String name;            // 接口名称
    private String url;             // 接口路径
    private String method;          // HTTP 方法 GET/POST
    private String description;     // 接口描述

    private String category;        // 分类：天气/翻译/AI/工具/金融
    private String tags;            // 标签：json,免费,高可用
    private String version;         // 版本
    private Long authorId;          // 发布者用户ID（0=平台官方）
    private String authorName;      // 发布者名称
    private String webhookUrl;      // 调用后回调地址
    private Integer timeoutMs;      // 超时时间(ms)
    private Integer retryCount;     // 失败重试次数
    private Integer pricingType;    // 计费类型 1=按次 2=包月
    private Integer urlType;        // 1=内部路径(/api/xxx) 2=外部全URL(http://...)
    private Integer dailyLimit;     // 每日调用上限，null=不限
    private String responseType;    // 返回类型
    private Integer isOfficial;     // 1=官方 0=用户发布

    private Integer status;         // 0下架 1上架
    private LocalDateTime createTime;

    @TableField(exist = false)
    private BigDecimal minPrice;    // 最低套餐价格（查询时联表计算）

//    @TableField(exist = false)
    private String requestParams;   // 兼容旧字段

//    @TableField(exist = false)
    private String responseResult;  // 兼容旧字段
}
