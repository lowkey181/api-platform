package com.api.apiadmin.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogDlqConsumer {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.mq.failed-log-key:mq:failed:api-call-log}")
    private String failedLogKey;

    @Value("${app.mq.failed-log-max-size:10000}")
    private long failedLogMaxSize;

    @RabbitListener(queues = "${spring.rabbitmq.queue.log-dlq}")
    public void receiveDlqMessage(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        // 存储失败的消息以供重播/审核。
        redisTemplate.opsForList().leftPush(failedLogKey, payload);
        redisTemplate.opsForList().trim(failedLogKey, 0, failedLogMaxSize - 1);
        log.error("日志消息进入死信队列，已写入Redis备用池，message={}", payload);
    }
}
