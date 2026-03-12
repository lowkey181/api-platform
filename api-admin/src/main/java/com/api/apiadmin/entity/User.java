package com.api.apiadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data

@TableName("user")
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    private  Long id;
    private  String username;
    private  String password;
    private  String phone;
    private  String email;
    private  Integer status;
    private  String role;
    private  LocalDateTime createTime;
    private  LocalDateTime updateTime;

    @TableField(exist = false)
    private List<String> accessKey;
}
