package com.api.apiadmin.service;

import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.ApiInterface;
import com.api.apiadmin.entity.UserInterfaceAuth;
import com.api.apiadmin.mapper.ApiInterfaceMapper;
import com.api.apiadmin.mapper.UserInterfaceAuthMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApiInterfaceService extends ServiceImpl<ApiInterfaceMapper, ApiInterface> {

    @Autowired
    private ApiInterfaceMapper apiInterfaceMapper;
    @Autowired
    private UserInterfaceAuthMapper userInterfaceAuthMapper;

    public Result insert(ApiInterface apiInterface) {
        if (apiInterface.getAuthorId() == null) {
            apiInterface.setIsOfficial(1);
        } else {
            apiInterface.setIsOfficial(0);
        }
        apiInterfaceMapper.insert(apiInterface);

        if (apiInterface.getAuthorId() != null && apiInterface.getAuthorId() > 0) {
            LambdaQueryWrapper<UserInterfaceAuth> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserInterfaceAuth::getUserId, apiInterface.getAuthorId())
                   .eq(UserInterfaceAuth::getInterfaceId, apiInterface.getId());
            if (userInterfaceAuthMapper.selectCount(wrapper) == 0) {
                UserInterfaceAuth auth = new UserInterfaceAuth();
                auth.setUserId(apiInterface.getAuthorId());
                auth.setInterfaceId(Long.valueOf(apiInterface.getId()));
                auth.setMaxCallCount(-1L);
                auth.setUsedCallCount(0L);
                auth.setExpireTime(java.time.LocalDateTime.now().plusYears(1));
                auth.setStatus(1);
                userInterfaceAuthMapper.insert(auth);
            }
        }
        return Result.ok(apiInterface.getId());
    }

    public Result update(ApiInterface apiInterface) {
        return Result.ok(apiInterfaceMapper.updateById(apiInterface));
    }

    public Result delete(Integer id) {
        return Result.ok(apiInterfaceMapper.deleteById(id));
    }

    public Result selectPage(Integer pageNum, Integer pageSize, Integer status, Long authorId) {
        LambdaQueryWrapper<ApiInterface> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ApiInterface::getStatus, status);
        }
        if (authorId != null && authorId > 0) {
            wrapper.eq(ApiInterface::getAuthorId, authorId);
        }
        wrapper.orderByDesc(ApiInterface::getCreateTime);
        Page<ApiInterface> page = new Page<>(pageNum, pageSize);
        Page<ApiInterface> result = page(page, wrapper);
        return Result.ok(result);
    }

    public Result getById(Integer id) {
        ApiInterface apiInterface = apiInterfaceMapper.selectById(id);
        return Result.ok(apiInterface);
    }
}
