package com.api.apiadmin.service;

import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.entity.ApiProduct;
import com.api.apiadmin.mapper.ApiProductMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApiProductService extends ServiceImpl<ApiProductMapper, ApiProduct> {
    @Autowired
    private ApiProductMapper apiProductMapper;

    public SaResult insert(ApiProduct apiProduct){
        apiProductMapper.insert(apiProduct);
        return SaResult.ok();
    }
    public SaResult update(ApiProduct apiProduct){
        apiProductMapper.updateById(apiProduct);
        return SaResult.ok();
    }
    public SaResult delete(Integer id){
        apiProductMapper.deleteById(id);
        return SaResult.ok();
    }
    public SaResult selectPage(Integer pageNum, Integer pageSize,Long interfaceId,Integer  status){
        MPJLambdaWrapper<ApiProduct> wrapper = new MPJLambdaWrapper<ApiProduct>()
                .selectAll(ApiProduct.class)
                .eq(ApiProduct::getInterfaceId, interfaceId)
                .eq(ApiProduct::getStatus, status);
        Page<ApiProduct> page = new Page<>(pageNum, pageSize);
        Page<ApiProduct> pageResult = apiProductMapper.selectJoinPage(page, ApiProduct.class, wrapper);
        return SaResult.ok().setData(pageResult);
    }
    // 更新上下架状态
    public SaResult updateStatus(Integer id, Integer status){
        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId(Long.valueOf(id));
        apiProduct.setStatus(status);
        apiProductMapper.updateById(apiProduct);
        return SaResult.ok();
    }
    public Long getCallCountById(Long id){
        ApiProduct apiProduct = apiProductMapper.selectById(id);

        return apiProduct.getCallCount();
    }
}
