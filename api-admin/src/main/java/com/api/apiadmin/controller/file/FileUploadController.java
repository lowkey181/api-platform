package com.api.apiadmin.controller.file;

import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.entity.FileInfo;
import com.api.apiadmin.mapper.FileInfoMapper;
import com.api.apiadmin.service.file.FileInfoService;
import com.api.apiadmin.service.file.FileStorageFactory;
import com.api.apiadmin.service.file.MinioFileStorageService;
import com.api.apiadmin.service.file.MinioManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageFactory storageFactory;
    @Autowired
    private MinioManageService minioManageService;
    @Autowired
    private FileInfoMapper fileInfoMapper;
    @Autowired
    private FileInfoService fileInfoService;
    // 允许的压缩包扩展名
    private static final Set<String> ALLOWED_COMPRESSED_EXTENSIONS = Set.of(
            ".zip", ".rar", ".7z", ".gz", ".tar", ".tgz"
    );
    /**
     * 单文件上传（支持秒传）
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileMd5", required = false) String fileMd5) {
        MinioFileStorageService storageService = storageFactory.getService();
        Map<String, Object> result = new HashMap<>();
        try {
            if (file.isEmpty()) {
                result.put("code", 400);
                result.put("message", "文件不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            // 验证文件
            String validationError = validateFile(file);
            if (validationError != null) {
                result.put("code", 400);
                result.put("message", validationError);
                return ResponseEntity.badRequest().body(result);
            }

            // 如果前端没有传 MD5，后端计算
            if (fileMd5 == null || fileMd5.isEmpty()) {
                fileMd5 = calculateMD5(file.getInputStream());
            }

            // 1. 检查数据库是否已存在相同 MD5 的文件
            FileInfo existFile = fileInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileInfo>()
                    .eq(FileInfo::getFileMd5, fileMd5)
            );

            if (existFile != null) {
                // 文件已存在，直接返回已有 URL（秒传成功）
                result.put("code", 200);
                result.put("data", existFile.getFileUrl());
                result.put("message", "秒传成功");
                result.put("isQuickUpload", true);
                return ResponseEntity.ok(result);
            }

            // 2. 文件不存在，执行正常上传流程
            String url = storageService.uploadFile(file);
            
            // 3. 保存文件信息到数据库
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(file.getOriginalFilename());
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFileUrl(url);
            fileInfo.setFileSize(file.getSize());
            fileInfo.setFileType(file.getContentType());
            fileInfo.setStorageType("minio");
            fileInfo.setObjectName(extractObjectName(url));
            fileInfo.setUploaderId(userId);
            fileInfo.setCreateTime(LocalDateTime.now());
            
            fileInfoService.saveFileInfo(fileInfo);

            result.put("code", 200);
            result.put("data", url);
            result.put("message", "上传成功");
            result.put("isQuickUpload", false);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "上传失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 计算文件的 MD5 值
     */
    private String calculateMD5(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        int bytesRead;
        
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }
        
        byte[] digestBytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digestBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 从 URL 中提取对象名称
     */
    private String extractObjectName(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            // 假设 URL 格式为：http://endpoint/bucket/objectName
            int thirdSlashIndex = 0;
            int count = 0;
            for (int i = 0; i < url.length(); i++) {
                if (url.charAt(i) == '/') {
                    count++;
                    if (count == 3) {
                        thirdSlashIndex = i;
                        break;
                    }
                }
            }
            return url.substring(thirdSlashIndex + 1);
        } catch (Exception e) {
            return null;
        }
    }
    /**
     * 验证文件（类型和大小）
     */
    private String validateFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        // 检查文件名
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "文件名不能为空";
        }

        // 检查文件大小（500MB）
        long maxSize = 500 * 1024 * 1024;
        System.out.println("文件大小：" + file.getSize());
        if (file.getSize() > maxSize) {
            return "文件大小不能超过 500MB";
        }

        // 检查文件扩展名
        String extension = getFileExtension(originalFilename).toLowerCase();

        // 如果是压缩包，验证扩展名
        if (isCompressedExtension(extension)) {
            return null; // 压缩包验证通过
        }

        // 对于其他类型，同时检查扩展名和 contentType
        if (!isValidFileType(contentType) && !isValidFileExtension(extension)) {
            return "不支持的文件类型";
        }

        return null;
    }
    /**
     * 根据扩展名判断是否为允许的文件类型
     */
    private boolean isValidFileExtension(String extension) {
        // 图片扩展名
        if (List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp").contains(extension)) {
            return true;
        }

        // 视频扩展名
        if (List.of(".mp4", ".avi", ".mov", ".wmv", ".flv").contains(extension)) {
            return true;
        }

        // 文档扩展名
        if (List.of(".pdf", ".doc", ".docx", ".txt").contains(extension)) {
            return true;
        }

        return false;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

    /**
     * 判断是否为允许的压缩包扩展名
     */
    private boolean isCompressedExtension(String extension) {
        return ALLOWED_COMPRESSED_EXTENSIONS.contains(extension);
    }
    /**
     * 多文件上传
     */
    @PostMapping("/uploads")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        MinioFileStorageService storageService = storageFactory.getService();

        Map<String, Object> result = new HashMap<>();
        try {
            if (files == null || files.length == 0) {
                result.put("code", 400);
                result.put("message", "文件不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            java.util.List<String> urls = new java.util.ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty() && isValidFileType(file.getContentType())) {
                    String url = storageService.uploadFile(file);
                    urls.add(url);
                }
            }

            result.put("code", 200);
            result.put("data", urls);
            result.put("message", "批量上传成功");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "上传失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteFile(@RequestParam("url") String url) {
        MinioFileStorageService storageService = storageFactory.getService();

        Map<String, Object> result = new HashMap<>();
        try {
            storageService.deleteFile(url);
            result.put("code", 200);
            result.put("message", "删除成功");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "删除失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 验证文件类型
     */
    private boolean isValidFileType(String contentType) {
        if (contentType == null) {
            return false;
        }

        // 图片类型
        if (contentType.startsWith("image/")) {
            return true;
        }

        // 视频类型
        if (contentType.startsWith("video/")) {
            return true;
        }

        // 压缩包类型
        if (isCompressedFileType(contentType)) {
            return true;
        }

        // 其他允许的类型
        return switch (contentType) {
            case "application/pdf", "text/plain", "application/msword",
                 "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> true;
            default -> false;
        };
    }

    /**
     * 判断是否为压缩包类型
     */
    private boolean isCompressedFileType(String contentType) {
        return switch (contentType) {
            case "application/zip",
                 "application/x-zip-compressed",
                 "application/x-rar-compressed",
                 "application/x-7z-compressed",
                 "application/gzip",
                 "application/x-gzip",
                 "application/x-tar" -> true;
            default -> false;
        };
    }

    /**
     * 获取文件列表（不依赖数据库）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public SaResult listFiles(
            @RequestParam(value = "prefix", required = false) String prefix,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        try {
            List<FileInfo> fileList = minioManageService.listFilesByPage(pageNum, pageSize, prefix);
            long total = minioManageService.countFilesByPrefix(prefix);

            Map<String, Object> pageData = new HashMap<>();
            pageData.put("list", fileList);
            pageData.put("total", total);
            pageData.put("pageNum", pageNum);
            pageData.put("pageSize", pageSize);

            return SaResult.ok().setData(pageData);
        } catch (Exception e) {
            return SaResult.error("获取文件列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有文件（不分页，适合小数据量）
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public SaResult listAllFiles(
            @RequestParam(value = "prefix", required = false) String prefix
    ) {
        try {
            List<FileInfo> fileList = minioManageService.listFilesByPrefix(prefix);
            return SaResult.ok().setData(fileList);
        } catch (Exception e) {
            return SaResult.error("获取文件列表失败：" + e.getMessage());
        }
    }

    /**
     * 重命名文件
     */
    @PutMapping("/rename")
    @PreAuthorize("hasRole('ADMIN')")
    public SaResult renameFile(
            @RequestParam("oldObjectName") String oldObjectName,
            @RequestParam("newFileName") String newFileName
    ) {
        try {
            // 验证新文件名
            if (newFileName == null || newFileName.trim().isEmpty()) {
                return SaResult.error("新文件名不能为空");
            }
            
            // 验证扩展名是否一致
            String oldExtension = getFileExtension1(oldObjectName);
            String newExtension = getFileExtension1(newFileName);
            if (!oldExtension.equalsIgnoreCase(newExtension)) {
                return SaResult.error("不能修改文件扩展名");
            }
            
            minioManageService.renameFile(oldObjectName, newFileName);
            return SaResult.ok("重命名成功");
        } catch (IllegalArgumentException e) {
            return SaResult.error(e.getMessage());
        } catch (Exception e) {
            return SaResult.error("重命名失败：" + e.getMessage());
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension1(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex != -1 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }
}
