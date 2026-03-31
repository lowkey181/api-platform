package com.api.apiadmin.service.file;

import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.entity.FileInfo;
import com.api.apiadmin.mapper.FileInfoMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileInfoService extends ServiceImpl<FileInfoMapper, FileInfo> {
    @Autowired
    private FileInfoMapper fileInfoMapper;

    public SaResult saveFileInfo(FileInfo fileInfo) {
        boolean save = this.save(fileInfo);
        if (save) {
            return SaResult.ok("保存成功");
        }
        return SaResult.error("保存失败");
    }

}
