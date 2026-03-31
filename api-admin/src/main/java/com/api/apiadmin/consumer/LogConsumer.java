package com.api.apiadmin.consumer;


import com.api.apiadmin.entity.ApiCallLog;
import com.api.apiadmin.entity.dto.ApiCallLogDTO;
import com.api.apiadmin.mapper.ApiCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogConsumer {

    private final ApiCallLogMapper apiCallLogMapper;

    // 直接接收JSON序列化的DTO
    @RabbitListener(queues = "${spring.rabbitmq.queue.log}")
    public void receiveLog(ApiCallLogDTO logDTO) {
        try {
            // 字段映射入库
            ApiCallLog apiCallLog = new ApiCallLog();
            apiCallLog.setUserId(logDTO.getUserId());
            apiCallLog.setAccessKey(logDTO.getAccessKey());
            apiCallLog.setInterfaceId(logDTO.getInterfaceId());
            apiCallLog.setRequestIp(logDTO.getRequestIp()); // 映射到表的request_ip
            apiCallLog.setStatus(logDTO.getStatus());
            apiCallLog.setCreateTime(logDTO.getCreateTime());
            // 其他字段留空
            apiCallLog.setRequestParams(logDTO.getRequestParams());
            apiCallLog.setResponseResult(logDTO.getResponseResult());
            apiCallLog.setUseTime(logDTO.getUseTime());
            apiCallLog.setErrorMsg(logDTO.getErrorMsg());
            // 插入数据库
            apiCallLogMapper.insert(apiCallLog);
            log.info("日志入库成功，userId={}, interfaceId={}", apiCallLog.getUserId(), apiCallLog.getInterfaceId());
        } catch (Exception e) {
            log.error("日志入库失败，触发消息重试，error={}", e.getMessage(), e);
            throw new RuntimeException("日志入库失败", e);
        }
    }
}