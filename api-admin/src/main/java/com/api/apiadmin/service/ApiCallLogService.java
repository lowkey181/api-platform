package com.api.apiadmin.service;

import com.api.apiadmin.entity.ApiCallLog;
import com.api.apiadmin.mapper.ApiCallLogMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApiCallLogService extends ServiceImpl<ApiCallLogMapper, ApiCallLog> {
    @Autowired
    private ApiCallLogMapper apiCallLogMapper;



}
