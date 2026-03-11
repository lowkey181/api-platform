package com.api.apiadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("pay_record")
public class PayRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long orderId;
    private String orderNo;
    private String transactionId;
    private BigDecimal payAmount;
    private String payType;
    private Integer payStatus;
    private Date createTime;
    private String tradeNo;

}
