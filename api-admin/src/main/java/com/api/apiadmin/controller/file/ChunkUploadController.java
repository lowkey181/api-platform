package com.api.apiadmin.controller.file;

import com.api.apiadmin.entity.FileInfo;
import com.api.apiadmin.mapper.FileInfoMapper;
import com.api.apiadmin.service.file.FileInfoService;
import com.api.apiadmin.service.file.FileStorageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/file/chunk")
@RequiredArgsConstructor
public class ChunkUploadController {

    @Autowired
    private FileStorageFactory storageFactory;
    
    @Autowired
    private FileInfoMapper fileInfoMapper;
    
    @Autowired
    private FileInfoService fileInfoService;

    // 临时文件目录
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/file-upload/";
    
    // 记录正在上传的分片（用于清理）
    private static final Map<String, Set<Integer>> uploadingChunks = new ConcurrentHashMap<>();

    /**
     * 初始化分片上传
     */
    @PostMapping("/init")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> initChunkUpload(
            @RequestParam String fileName,
            @RequestParam Long fileSize,
            @RequestParam String fileMd5,
            @RequestParam Integer totalChunks) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            // 1. 检查文件是否已存在（秒传）
            FileInfo existFile = fileInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileInfo>()
                    .eq(FileInfo::getFileMd5, fileMd5)
            );
            
            if (existFile != null) {
                // 文件已存在，直接返回
                result.put("code", 200);
                result.put("data", existFile.getFileUrl());
                result.put("message", "秒传成功");
                result.put("isQuickUpload", true);
                return ResponseEntity.ok(result);
            }
            
            // 2. 创建临时目录
            String uploadId = UUID.randomUUID().toString().replace("-", "");
            String tempDirPath = TEMP_DIR + uploadId;
            File tempDir = new File(tempDirPath);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            // 3. 记录上传信息
            uploadingChunks.put(uploadId, ConcurrentHashMap.newKeySet());
            
            result.put("code", 200);
            result.put("data", new InitChunkResult(uploadId, tempDirPath));
            result.put("message", "初始化成功");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("初始化分片上传失败", e);
            result.put("code", 500);
            result.put("message", "初始化失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 上传分片
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> uploadChunk(
            @RequestParam String uploadId,
            @RequestParam Integer chunkIndex,
            @RequestParam String fileMd5,
            @RequestParam MultipartFile chunk) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. 获取临时目录路径
            String tempDirPath = TEMP_DIR + uploadId;
            File tempDir = new File(tempDirPath);
            if (!tempDir.exists()) {
                result.put("code", 400);
                result.put("message", "上传会话已过期，请重新上传");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 2. 保存分片到临时目录
            String chunkFileName = String.format("%s_%d.chunk", fileMd5, chunkIndex);
            Path chunkPath = Paths.get(tempDirPath, chunkFileName);
            Files.copy(chunk.getInputStream(), chunkPath, StandardCopyOption.REPLACE_EXISTING);
            
            // 3. 记录已上传的分片
            Set<Integer> chunks = uploadingChunks.get(uploadId);
            if (chunks != null) {
                chunks.add(chunkIndex);
            }
            
            log.info("分片 {}/{} 上传成功", chunkIndex, fileMd5);
            
            result.put("code", 200);
            result.put("message", "分片上传成功");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("上传分片失败", e);
            result.put("code", 500);
            result.put("message", "分片上传失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 合并分片
     */
    @PostMapping("/merge")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> mergeChunks(
            @RequestParam String uploadId,
            @RequestParam String fileName,
            @RequestParam String fileMd5,
            @RequestParam Long fileSize,
            @RequestParam Integer totalChunks,
            @RequestParam(required = false) String fileType) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            // 1. 获取临时目录
            String tempDirPath = TEMP_DIR + uploadId;
            File tempDir = new File(tempDirPath);
            if (!tempDir.exists()) {
                result.put("code", 400);
                result.put("message", "上传会话不存在或已过期");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 2. 验证分片是否完整
            Set<Integer> uploadedChunks = uploadingChunks.get(uploadId);
            if (uploadedChunks == null || uploadedChunks.size() != totalChunks) {
                result.put("code", 400);
                result.put("message", "分片不完整，请检查所有分片是否上传成功");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 3. 合并分片
            File mergedFile = new File(TEMP_DIR + fileMd5 + ".tmp");
            try (var outputStream = Files.newOutputStream(mergedFile.toPath())) {
                for (int i = 0; i < totalChunks; i++) {
                    String chunkFileName = String.format("%s_%d.chunk", fileMd5, i);
                    Path chunkPath = Paths.get(tempDirPath, chunkFileName);
                    if (Files.exists(chunkPath)) {
                        byte[] chunkBytes = Files.readAllBytes(chunkPath);
                        outputStream.write(chunkBytes);
                    } else {
                        result.put("code", 400);
                        result.put("message", "分片 " + i + " 不存在");
                        return ResponseEntity.badRequest().body(result);
                    }
                }
            }
            
            // 4. 将合并后的文件上传到 MinIO
            var storageService = storageFactory.getService();
            String url = storageService.uploadMergedFile(mergedFile, fileName, fileMd5);
            
            // 5. 保存文件信息到数据库
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(fileName);
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFileUrl(url);
            fileInfo.setFileSize(fileSize);
            fileInfo.setFileType(fileType);
            fileInfo.setStorageType("minio");
            fileInfo.setUploaderId(userId);
            fileInfo.setCreateTime(java.time.LocalDateTime.now());
            fileInfoService.saveFileInfo(fileInfo);
            
            // 6. 清理临时文件
            cleanupTempFiles(uploadId, mergedFile);
            
            log.info("文件合并成功：{}", fileName);
            
            result.put("code", 200);
            result.put("data", url);
            result.put("message", "上传成功");
            result.put("isQuickUpload", false);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("合并分片失败", e);
            result.put("code", 500);
            result.put("message", "合并失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 查询已上传的分片
     */
    @GetMapping("/chunks/{uploadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> getUploadedChunks(@PathVariable String uploadId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Set<Integer> chunks = uploadingChunks.get(uploadId);
            result.put("code", 200);
            result.put("data", chunks != null ? chunks : new HashSet<>());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(String uploadId, File... extraFiles) throws IOException {
        // 清理分片目录
        String tempDirPath = TEMP_DIR + uploadId;
        File tempDir = new File(tempDirPath);
        if (tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            tempDir.delete();
        }
        
        // 清理额外的临时文件
        for (File file : extraFiles) {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
        
        // 清理记录
        uploadingChunks.remove(uploadId);
    }

    /**
     * 初始化结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class InitChunkResult {
        private String uploadId;
        private String tempDirPath;
    }
}
