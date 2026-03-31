package com.api.apigateway.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${spring.rabbitmq.queue.blackList}")
    private String blackListQueue;

    // 网关只负责发送日志消息，不在网关端声明日志队列（由 api-admin 统一声明并带 DLX 参数）
    @Bean
    public Queue blackListQueue() {
        return new Queue(blackListQueue, true);
    }
}