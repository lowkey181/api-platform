package com.api.apiadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_info")
public class FileInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private String fileMd5;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private String storageType;
    private String objectName;
    private Long uploaderId;
    private LocalDateTime createTime;
}
