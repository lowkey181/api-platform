package com.api.apiadmin.service.file;


import com.api.apiadmin.config.Minio.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileStorageFactory {

    private final StorageProperties properties;
    private final MinioFileStorageService minioService;

    public MinioFileStorageService getService() {
        // 使用 MinIO，暂时只有一种存储服务
        return minioService;
    }
}
