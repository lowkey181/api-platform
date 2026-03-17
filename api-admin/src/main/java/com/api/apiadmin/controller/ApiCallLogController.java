package com.api.apiadmin.controller;

import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.service.ApiCallLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/callLog")
public class ApiCallLogController {
    @Autowired
    private ApiCallLogService apiCallLogService;

    /**
     * 获取接口调用日志
     * @param pageNum 页码，默认为 1
     * @param pageSize 每页数量，默认为 10
     * @param interfaceId 接口 ID
     * @param status 状态
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 接口调用日志列表
     */
    public SaResult getApiCallLogs(@RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                   Long interfaceId,
                                   Integer status,
                                   String startDate,
                                   String endDate) {
        return apiCallLogService.getApiCallLogs(pageNum, pageSize, interfaceId, status, startDate, endDate);
    }
}
