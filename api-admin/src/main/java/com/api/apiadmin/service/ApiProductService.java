package com.api.apiadmin.service;

import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.ApiProduct;
import com.api.apiadmin.mapper.ApiProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApiProductService extends ServiceImpl<ApiProductMapper, ApiProduct> {
    @Autowired
    private ApiProductMapper apiProductMapper;

    public Result insert(ApiProduct apiProduct){
        apiProductMapper.insert(apiProduct);
        return Result.ok("新增成功");
    }
    
    public Result update(ApiProduct apiProduct){
        apiProductMapper.updateById(apiProduct);
        return Result.ok("更新成功");
    }
    
    public Result delete(Integer id){
        apiProductMapper.deleteById(id);
        return Result.ok("删除成功");
    }
    
    public Result selectPage(Integer pageNum, Integer pageSize, Long interfaceId, Integer status){
        LambdaQueryWrapper<ApiProduct> wrapper = new LambdaQueryWrapper<>();
        if (interfaceId != null) {
            wrapper.eq(ApiProduct::getInterfaceId, interfaceId);
        }
        if (status != null) {
            wrapper.eq(ApiProduct::getStatus, status);
        }
        wrapper.orderByDesc(ApiProduct::getCreateTime);
        Page<ApiProduct> page = new Page<>(pageNum, pageSize);
        Page<ApiProduct> result = page(page, wrapper);
        return Result.ok(result);
    }
    
    // 更新上下架状态
    public Result updateStatus(Integer id, Integer status){
        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId(Long.valueOf(id));
        apiProduct.setStatus(status);
        apiProductMapper.updateById(apiProduct);
        return Result.ok("状态更新成功");
    }
    
    public Long getCallCountById(Long id){
        ApiProduct apiProduct = apiProductMapper.selectById(id);
        return apiProduct.getCallCount();
    }
}
