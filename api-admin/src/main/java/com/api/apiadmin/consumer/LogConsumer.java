package com.api.apiadmin.consumer;


import com.api.apiadmin.entity.ApiCallLog;
import com.api.apiadmin.entity.dto.ApiCallLogDTO;
import com.api.apiadmin.mapper.ApiCallLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogConsumer {

    private final ApiCallLogMapper apiCallLogMapper;

    // 直接接收JSON序列化的DTO
    @RabbitListener(queues = "${spring.rabbitmq.queue.log}")
    public void receiveLog(ApiCallLogDTO logDTO) {
        try {
            // 字段映射入库
            ApiCallLog log = new ApiCallLog();
            log.setUserId(logDTO.getUserId());
            log.setAccessKey(logDTO.getAccessKey());
            log.setInterfaceId(logDTO.getInterfaceId());
            log.setRequestIp(logDTO.getRequestIp()); // 映射到表的request_ip
            log.setStatus(logDTO.getStatus());
            log.setCreateTime(logDTO.getCreateTime());
            // 其他字段留空
            log.setRequestParams(logDTO.getRequestParams());
            log.setResponseResult(logDTO.getResponseResult());
            log.setUseTime(logDTO.getUseTime());
            log.setErrorMsg(logDTO.getErrorMsg());
            // 插入数据库
            apiCallLogMapper.insert(log);
            System.out.println("日志入库成功：" + log);
        } catch (Exception e) {
            System.err.println("日志入库失败：" + e.getMessage());
        }
    }
}