package com.api.apiadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("app")
public class App {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String appName;
    private String accessKey;
    private String secretKey;
    private Integer status;
    private LocalDateTime createTime;
}