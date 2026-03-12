package com.api.apiadmin.service;

import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.ApiInterface;
import com.api.apiadmin.mapper.ApiInterfaceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApiInterfaceService extends ServiceImpl<ApiInterfaceMapper, ApiInterface> {
    @Autowired
    private ApiInterfaceMapper apiInterfaceMapper;

    public Result insert(ApiInterface apiInterface){
        return Result.ok(apiInterfaceMapper.insert(apiInterface));
    }

    public Result update(ApiInterface apiInterface){
        return Result.ok(apiInterfaceMapper.updateById(apiInterface));
    }

    public Result delete(Integer id){
        return Result.ok(apiInterfaceMapper.deleteById(id));
    }

    public Result selectPage(Integer pageNum, Integer pageSize, Integer status){
        LambdaQueryWrapper<ApiInterface> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ApiInterface::getStatus, status);
        }
        wrapper.orderByDesc(ApiInterface::getCreateTime);
        Page<ApiInterface> page = new Page<>(pageNum, pageSize);
        Page<ApiInterface> result = page(page, wrapper);
        return Result.ok(result);
    }
}
