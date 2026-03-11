package com.api.apiadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
    private Long userId;
    private Long orderId;
    private Long interfaceId;
    private BigDecimal rechargeAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String payType;
    private Date createTime;

}
