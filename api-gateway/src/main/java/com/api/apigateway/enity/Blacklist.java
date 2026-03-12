package com.api.apigateway.enity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("blacklist")
public class Blacklist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String accessKey;  // 黑名单accessKey
    private String ip;          // 黑名单IP
    private String reason;      // 拉黑原因
    private Integer status;     // 1-拉黑 0-解除
    private Date createTime;    // 创建时间
}