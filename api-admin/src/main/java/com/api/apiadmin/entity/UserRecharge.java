package com.api.apiadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("user_recharge")
public class UserRecharge {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("order_id")
    private Long orderId;

    @TableField("interface_id")
    private Long interfaceId;

    @TableField("recharge_amount")
    private BigDecimal rechargeAmount;

    @TableField("balance_before")
    private BigDecimal balanceBefore;

    @TableField("balance_after")
    private BigDecimal balanceAfter;

    @TableField("pay_type")
    private String payType;

    @TableField("create_time")
    private Date createTime;

}
