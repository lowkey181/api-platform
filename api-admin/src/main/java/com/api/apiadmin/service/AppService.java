package com.api.apiadmin.service;

import cn.hutool.core.util.RandomUtil;
import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.App;
import com.api.apiadmin.mapper.AppMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppService extends ServiceImpl<AppMapper, App> {
    @Autowired
    private AppMapper appMapper;

    public List<String> getAllAccessKey(Long userId){
        MPJLambdaWrapper<App> wrapper = new MPJLambdaWrapper<App>()
                .select(App::getAccessKey)
                .eq(App::getUserId, userId);
        List<App> appList = appMapper.selectJoinList(wrapper);
        return appList.stream()
                .map(App::getAccessKey)
                .collect(java.util.stream.Collectors.toList());
    }

    // 根据用户ID查所有密钥
    public List<App> getByUserId(Long userId) {
        return list(new LambdaQueryWrapper<App>()
                .eq(App::getUserId, userId)
                .orderByDesc(App::getCreateTime));
    }

    // 根据accessKey查密钥（网关鉴权用）
    public App getByAccessKey(String accessKey) {
        return getOne(new LambdaQueryWrapper<App>()
                .eq(App::getAccessKey, accessKey));
    }

    public Result insert(String appName) {
        if (appName==null){
            return Result.error("请输入应用名称");
        }
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 生成密钥
        String accessKey = RandomUtil.randomString(24);
        String secretKey = RandomUtil.randomString(32);

        // 保存数据库
        App app = new App();
        app.setUserId(userId);
        app.setAppName(appName);
        app.setAccessKey(accessKey);
        app.setSecretKey(secretKey);
        app.setStatus(1);
        appMapper.insert(app);
        return Result.ok("创建成功");
    }


}
