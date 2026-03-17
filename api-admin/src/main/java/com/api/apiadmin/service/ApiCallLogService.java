package com.api.apiadmin.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.entity.ApiCallLog;
import com.api.apiadmin.mapper.ApiCallLogMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApiCallLogService extends ServiceImpl<ApiCallLogMapper, ApiCallLog> {
    @Autowired
    private ApiCallLogMapper apiCallLogMapper;

    public SaResult getApiCallLogs(Integer pageNum, Integer pageSize, Long interfaceId, Integer  status,String startDate,
                                   String endDate) {
        MPJLambdaWrapper<ApiCallLog> wrapper = new MPJLambdaWrapper<ApiCallLog>()
                .selectAll(ApiCallLog.class)
                .eq(ObjectUtil.isNotNull(interfaceId),ApiCallLog::getInterfaceId, interfaceId)
                .eq(ObjectUtil.isNotNull(status),ApiCallLog::getStatus, status)
                .ge(StrUtil.isNotBlank(startDate),ApiCallLog::getCreateTime, startDate)
                .le(StrUtil.isNotBlank(endDate),ApiCallLog::getCreateTime, endDate);
        Page<ApiCallLog> page = new Page<>(pageNum, pageSize);
        Page<ApiCallLog> result = page(page, wrapper);
        return SaResult.ok().setData(result);
    }


}
