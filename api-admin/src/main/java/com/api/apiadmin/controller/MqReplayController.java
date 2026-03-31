package com.api.apiadmin.controller;

import com.api.apiadmin.config.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mq/replay")
@RequiredArgsConstructor
public class MqReplayController {

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.mq.failed-log-key:mq:failed:api-call-log}")
    private String failedLogKey;
    @Value("${spring.rabbitmq.exchange.log}")
    private String logExchange;
    @Value("${spring.rabbitmq.routing.log}")
    private String logRoutingKey;

    @PostMapping("/log")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> replayFailedLogs(
            @RequestParam(defaultValue = "100") Integer batchSize,
            @RequestParam(defaultValue = "false") Boolean dryRun) {
        int safeBatchSize = Math.min(Math.max(batchSize, 1), 1000);
        long queued = redisTemplate.opsForList().size(failedLogKey) == null ? 0 : redisTemplate.opsForList().size(failedLogKey);

        if (Boolean.TRUE.equals(dryRun)) {
            Map<String, Object> dryRunResult = new HashMap<>();
            dryRunResult.put("batchSize", safeBatchSize);
            dryRunResult.put("queued", queued);
            dryRunResult.put("message", "dryRun模式未执行重放");
            return Result.ok(dryRunResult);
        }

        int replayed = 0;
        int failed = 0;
        for (int i = 0; i < safeBatchSize; i++) {
            String payload = redisTemplate.opsForList().rightPop(failedLogKey);
            if (payload == null) {
                break;
            }
            try {
                Object body = objectMapper.readValue(payload, Object.class);
                rabbitTemplate.convertAndSend(logExchange, logRoutingKey, body);
                replayed++;
            } catch (Exception ex) {
                failed++;
                // Publish failed again, put back to tail to avoid loss.
                redisTemplate.opsForList().rightPush(failedLogKey, payload);
                log.error("重放失败，消息已回滚到Redis。error={}", ex.getMessage(), ex);
            }
        }

        long remaining = redisTemplate.opsForList().size(failedLogKey) == null ? 0 : redisTemplate.opsForList().size(failedLogKey);
        Map<String, Object> result = new HashMap<>();
        result.put("batchSize", safeBatchSize);
        result.put("replayed", replayed);
        result.put("failed", failed);
        result.put("remaining", remaining);
        return Result.ok(result);
    }
}
