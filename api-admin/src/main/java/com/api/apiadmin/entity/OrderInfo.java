package com.api.apiadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("order_info")
public class OrderInfo {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long interfaceId;
    private Long productId;
    private BigDecimal totalAmount;
    private Integer payStatus;
    private Date payTime;
    private String paymentMethod;
    private Date createTime;
    private String tradeNo;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
