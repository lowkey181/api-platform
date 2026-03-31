package com.api.apiadmin.service.file;


import com.api.apiadmin.config.Minio.StorageProperties;
import com.api.apiadmin.entity.FileInfo;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioManageService {

    private final StorageProperties properties;
    private MinioClient minioClient;

    /**
     * 获取 MinIO Client（懒加载）
     */
    private MinioClient getClient() {
        if (minioClient == null) {
            StorageProperties.MinioConfig config = properties.getMinio();

            // 验证配置
            if (config.getEndpoint() == null || config.getEndpoint().isEmpty()) {
                throw new IllegalStateException("MinIO endpoint 未配置");
            }
            if (config.getAccessKey() == null || config.getAccessKey().isEmpty()) {
                throw new IllegalStateException("MinIO accessKey 未配置");
            }
            if (config.getSecretKey() == null || config.getSecretKey().isEmpty()) {
                throw new IllegalStateException("MinIO secretKey 未配置");
            }

            log.info("初始化 MinIO Client: {}", config.getEndpoint());
            minioClient = MinioClient.builder()
                    .endpoint(config.getEndpoint())
                    .credentials(config.getAccessKey(), config.getSecretKey())
                    .build();
        }
        return minioClient;
    }

    /**
     * 获取所有文件列表
     */
    public List<FileInfo> listAllFiles() {
        return listFilesByPrefix(null);
    }

    /**
     * 根据前缀获取文件列表（支持目录筛选）
     * @param prefix 前缀，如 "images/"、"videos/"、"2026-03-25/"
     */
    public List<FileInfo> listFilesByPrefix(String prefix) {
        List<FileInfo> fileList = new ArrayList<>();

        try {
            String bucket = properties.getMinio().getBucket();

            // 判断是否是文件类型筛选 (images/, videos/, archives/, other/)
            boolean isTypeFilter = prefix != null && 
                (prefix.equals("images/") || prefix.equals("videos/") || 
                 prefix.equals("archives/") || prefix.equals("other/"));

            if (isTypeFilter) {
                // 获取所有文件，然后在内存中按类型筛选
                log.info("按文件类型筛选：{}", prefix);
                Iterable<Result<Item>> results = getClient().listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucket)
                                .recursive(true)
                                .build()
                );

                for (Result<Item> result : results) {
                    Item item = result.get();

                    // 跳过目录对象
                    if (item.isDir()) {
                        continue;
                    }

                    // 判断文件是否属于该类型
                    String objectName = item.objectName();
                    String fileName = extractFileName(objectName);
                    String fileType = extractFileType(fileName);
                    
                    if (!matchesTypeFilter(fileType, prefix)) {
                        continue; // 不匹配的类型直接跳过
                    }

                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setObjectName(objectName);
                    fileInfo.setFileSize(item.size());
                    fileInfo.setFileUrl(getFileUrl(objectName));
                    fileInfo.setCreateTime(LocalDateTime.now());
                    fileInfo.setFileName(fileName);
                    fileInfo.setFileType(fileType);

                    fileList.add(fileInfo);
                }
            } else {
                // 普通的前缀筛选（如日期前缀）
                log.info("按前缀筛选：{}", prefix != null ? prefix : "全部");
                Iterable<Result<Item>> results = getClient().listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucket)
                                .prefix(prefix != null ? prefix : "")
                                .recursive(true)
                                .build()
                );

                for (Result<Item> result : results) {
                    Item item = result.get();

                    if (item.isDir()) {
                        continue;
                    }

                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setObjectName(item.objectName());
                    fileInfo.setFileSize(item.size());
                    fileInfo.setFileUrl(getFileUrl(item.objectName()));
                    fileInfo.setCreateTime(LocalDateTime.now());

                    String objectName = item.objectName();
                    String fileName = extractFileName(objectName);
                    String fileType = extractFileType(fileName);

                    fileInfo.setFileName(fileName);
                    fileInfo.setFileType(fileType);

                    fileList.add(fileInfo);
                }
            }

            log.info("从 MinIO 获取文件列表成功，共 {} 个文件", fileList.size());

        } catch (Exception e) {
            log.error("获取 MinIO 文件列表失败", e);
            throw new RuntimeException("获取文件列表失败：" + e.getMessage(), e);
        }

        return fileList;
    }

    /**
     * 判断文件类型是否匹配筛选条件
     */
    private boolean matchesTypeFilter(String fileType, String prefix) {
        if (fileType == null) {
            return false;
        }
        
        switch (prefix) {
            case "images/":
                return fileType.startsWith("image/");
            case "videos/":
                return fileType.startsWith("video/");
            case "archives/":
                return fileType.contains("zip") || fileType.contains("rar") || 
                       fileType.contains("7z") || fileType.contains("gzip") || 
                       fileType.contains("tar") || fileType.contains("compressed");
            case "other/":
                return !fileType.startsWith("image/") && 
                       !fileType.startsWith("video/") && 
                       !(fileType.contains("zip") || fileType.contains("rar") || 
                         fileType.contains("7z") || fileType.contains("gzip") || 
                         fileType.contains("tar") || fileType.contains("compressed"));
            default:
                return true;
        }
    }

    /**
     * 分页获取文件列表
     */
    public List<FileInfo> listFilesByPage(int pageNum, int pageSize) {
        return listFilesByPage(pageNum, pageSize, null);
    }

    /**
     * 分页获取文件列表（带前缀筛选）
     */
    public List<FileInfo> listFilesByPage(int pageNum, int pageSize, String prefix) {
        List<FileInfo> allFiles = listFilesByPrefix(prefix);

        if (allFiles.isEmpty()) {
            return allFiles;
        }

        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allFiles.size());

        if (fromIndex >= allFiles.size()) {
            return new ArrayList<>();
        }

        return allFiles.subList(fromIndex, toIndex);
    }

    /**
     * 统计文件总数
     */
    public long countFiles() {
        return countFilesByPrefix(null);
    }

    /**
     * 统计指定前缀的文件数
     */
    public long countFilesByPrefix(String prefix) {
        return listFilesByPrefix(prefix).size();
    }

    /**
     * 生成文件访问 URL
     */
    private String getFileUrl(String objectName) {
        String endpoint = properties.getMinio().getEndpoint();
        String bucket = properties.getMinio().getBucket();
        return endpoint + "/" + bucket + "/" + objectName;
    }

    /**
     * 从对象路径提取文件名
     */
    private String extractFileName(String objectName) {
        int lastSlashIndex = objectName.lastIndexOf("/");
        if (lastSlashIndex != -1 && lastSlashIndex < objectName.length() - 1) {
            return objectName.substring(lastSlashIndex + 1);
        }
        return objectName;
    }

    /**
     * 从文件名提取文件类型（MIME）
     */
    private String extractFileType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        String extension = "";
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
            extension = fileName.substring(dotIndex + 1).toLowerCase();
        }

        // 常见扩展名对应的 MIME 类型
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            case "avi" -> "video/avi";
            case "mov" -> "video/quicktime";
            case "zip" -> "application/zip";
            case "rar" -> "application/x-rar-compressed";
            case "7z" -> "application/x-7z-compressed";
            case "gz" -> "application/gzip";
            case "tar" -> "application/x-tar";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }

    /**
     * 重命名文件
     */
    public void renameFile(String oldObjectName, String newFileName) {
        try {
            MinioClient client = getClient();
            String bucket = properties.getMinio().getBucket();
            
            // 获取旧文件的目录路径
            int lastSlashIndex = oldObjectName.lastIndexOf("/");
            String dirPath = lastSlashIndex != -1 ? oldObjectName.substring(0, lastSlashIndex + 1) : "";
            
            // 构建新对象的完整路径
            String newObjectName = dirPath + newFileName;
            
            // 复制文件（使用新名称）
            CopySource source = CopySource.builder()
                    .bucket(bucket)
                    .object(oldObjectName)
                    .build();
            
            client.copyObject(CopyObjectArgs.builder()
                    .source(source)
                    .bucket(bucket)
                    .object(newObjectName)
                    .build());
            
            // 删除旧文件
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(oldObjectName)
                    .build());
            
            log.info("文件重命名成功：{} -> {}", oldObjectName, newObjectName);
            
        } catch (Exception e) {
            log.error("文件重命名失败", e);
            throw new RuntimeException("文件重命名失败：" + e.getMessage(), e);
        }
    }
}
