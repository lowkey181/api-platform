package com.api.apiadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_interface_auth")
public class UserInterfaceAuth {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long interfaceId;
    private Long maxCallCount;
    private Long usedCallCount;
    private LocalDateTime expireTime;
    private Integer status;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String name;

    @TableField(exist = false)
    private String description;

    @TableField(exist = false)
    private String url;

    @TableField(exist = false)
    private String method;

    @TableField(exist = false)
    private String requestParams;

    @TableField(exist = false)
    private String responseResult;
}
