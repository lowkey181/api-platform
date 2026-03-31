 package com.api.apiadmin.service.file;

import com.api.apiadmin.config.Minio.StorageProperties;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorageService {

    private final StorageProperties properties;
    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            StorageProperties.MinioConfig config = properties.getMinio();
            log.info("开始初始化 MinIO，endpoint: {}, bucket: {}", config.getEndpoint(), config.getBucket());

            // 验证配置
            if (config.getEndpoint() == null || config.getEndpoint().isEmpty()) {
                throw new IllegalArgumentException("MinIO endpoint 不能为空");
            }
            if (config.getAccessKey() == null || config.getAccessKey().isEmpty()) {
                throw new IllegalArgumentException("MinIO accessKey 不能为空");
            }
            if (config.getSecretKey() == null || config.getSecretKey().isEmpty()) {
                throw new IllegalArgumentException("MinIO secretKey 不能为空");
            }
            if (config.getBucket() == null || config.getBucket().isEmpty()) {
                throw new IllegalArgumentException("MinIO bucket 不能为空");
            }

            minioClient = MinioClient.builder()
                    .endpoint(config.getEndpoint())
                    .credentials(config.getAccessKey(), config.getSecretKey())
                    .build();

            // 检查 bucket 是否存在，不存在则创建
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(config.getBucket())
                    .build());

            if (!found) {
                log.info("Bucket '{}' 不存在，正在创建...", config.getBucket());
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(config.getBucket())
                        .build());
            } else {
                log.info("Bucket '{}' 已存在", config.getBucket());
            }

            // 设置 bucket 为公共读（无论是否存在都设置）
            String policyJson = "{\n" +
                    "  \"Version\": \"2012-10-17\",\n" +
                    "  \"Statement\": [{\n" +
                    "    \"Effect\": \"Allow\",\n" +
                    "    \"Principal\": \"*\",\n" +
                    "    \"Action\": [\"s3:GetObject\"],\n" +
                    "    \"Resource\": [\"arn:aws:s3:::" + config.getBucket() + "/*\"]\n" +
                    "  }]\n" +
                    "}";
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(config.getBucket())
                    .config(policyJson)
                    .build());
            log.info("Bucket '{}' 公共读策略设置成功", config.getBucket());

            log.info("MinIO 初始化成功，endpoint: {}, bucket: {}", config.getEndpoint(), config.getBucket());
        } catch (Exception e) {
            log.error("MinIO 初始化失败：{}", e.getMessage(), e);
            throw new RuntimeException("MinIO 初始化失败：" + e.getMessage(), e);
        }
    }

    public String uploadFile(MultipartFile file) {
        return uploadFile(file, null);
    }

    public String uploadFile(MultipartFile file, String path) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new IllegalArgumentException("文件名不能为空");
            }

            // 生成唯一文件名
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;

            // 根据文件类型自动分类到不同目录
            String typeDir = getFileTypeDirectory(file.getContentType(), originalFilename);
            String objectName = (path != null ? path + "/" : "") +
                    typeDir + "/" +
                    java.time.LocalDate.now().toString() + "/" + fileName;

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getMinio().getBucket())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            // 返回访问 URL
            String url = getUrl(objectName);
            log.info("文件上传成功：{}", url);
            return url;

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);
        }
    }

    public void deleteFile(String url) {
        try {
            String objectName = extractObjectName(url);
            if (objectName != null) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(properties.getMinio().getBucket())
                        .object(objectName)
                        .build());
                log.info("文件删除成功：{}", url);
            }
        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new RuntimeException("文件删除失败：" + e.getMessage(), e);
        }
    }

    public InputStream getFileInputStream(String url) {
        try {
            String objectName = extractObjectName(url);
            if (objectName != null) {
                return minioClient.getObject(GetObjectArgs.builder()
                        .bucket(properties.getMinio().getBucket())
                        .object(objectName)
                        .build());
            }
            return null;
        } catch (Exception e) {
            log.error("获取文件流失败", e);
            throw new RuntimeException("获取文件流失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取文件访问 URL
     */
//    private String getUrl(String objectName) throws Exception {
//        // 生成预签名 URL（有效期 7 天）
//        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
//                .method(Method.GET)
//                .bucket(properties.getMinio().getBucket())
//                .object(objectName)
//                .expiry(604800) // 7 天
//                .build());
//    }
    private String getUrl(String objectName) throws Exception {
        StorageProperties.MinioConfig config = properties.getMinio();

        // 构建公开访问 URL（永久有效）
        return config.getEndpoint() + "/" + config.getBucket() + "/" + objectName;
    }
    /**
     * 从 URL 中提取对象名称
     */
    private String extractObjectName(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            String bucket = properties.getMinio().getBucket();
            int index = url.indexOf(bucket);
            if (index != -1) {
                return url.substring(index + bucket.length() + 1);
            }
        } catch (Exception e) {
            log.error("解析对象名失败", e);
        }
        return null;
    }

    /**
     * 根据文件类型返回对应的目录
     * @param contentType MIME 类型
     * @param fileName 文件名
     * @return 目录名 (images/videos/archives/other)
     */
    private String getFileTypeDirectory(String contentType, String fileName) {
        if (contentType != null) {
            // 图片类型
            if (contentType.startsWith("image/")) {
                return "images";
            }
            // 视频类型
            if (contentType.startsWith("video/")) {
                return "videos";
            }
            // 压缩包类型
            if (contentType.startsWith("application/zip") ||
                contentType.startsWith("application/x-zip-compressed") ||
                contentType.startsWith("application/x-rar-compressed") ||
                contentType.startsWith("application/x-7z-compressed") ||
                contentType.startsWith("application/gzip") ||
                contentType.startsWith("application/x-tar")) {
                return "archives";
            }
        }
        
        // 如果 contentType 为空或不匹配，根据扩展名判断
        if (fileName != null && !fileName.isEmpty()) {
            String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
            // 图片扩展名
            if (List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp").contains(extension)) {
                return "images";
            }
            // 视频扩展名
            if (List.of(".mp4", ".avi", ".mov", ".wmv", ".flv").contains(extension)) {
                return "videos";
            }
            // 压缩包扩展名
            if (List.of(".zip", ".rar", ".7z", ".gz", ".tar", ".tgz").contains(extension)) {
                return "archives";
            }
        }
        
        // 默认为 other 目录
        return "other";
    }

    /**
     * 上传合并后的文件到 MinIO
     */
    public String uploadMergedFile(File mergedFile, String originalFilename, String fileMd5) {
        try {
            // 使用 MD5 作为唯一标识，避免重复
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = fileMd5 + extension;

            // 根据文件类型自动分类到不同目录
            String contentType = java.nio.file.Files.probeContentType(mergedFile.toPath());
            // 如果无法识别 content type，使用默认值
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            String typeDir = getFileTypeDirectory(contentType, originalFilename);
            String objectName = typeDir + "/" +
                    java.time.LocalDate.now().toString() + "/" + fileName;

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getMinio().getBucket())
                    .object(objectName)
                    .stream(java.nio.file.Files.newInputStream(mergedFile.toPath()), mergedFile.length(), -1)
                    .contentType(contentType)
                    .build());

            // 返回访问 URL
            String url = getUrl(objectName);
            log.info("合并文件上传成功：{}", url);
            return url;

        } catch (Exception e) {
            log.error("合并文件上传失败", e);
            throw new RuntimeException("合并文件上传失败：" + e.getMessage(), e);
        }
    }
}
