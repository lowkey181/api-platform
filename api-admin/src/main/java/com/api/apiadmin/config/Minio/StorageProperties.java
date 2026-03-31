package com.api.apiadmin.config.Minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String type = "minio";
    private MinioConfig minio = new MinioConfig();
    private OssConfig oss = new OssConfig();

    @Data
    public static class MinioConfig {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
    }

    @Data
    public static class OssConfig {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
        private String domain;
    }
}
