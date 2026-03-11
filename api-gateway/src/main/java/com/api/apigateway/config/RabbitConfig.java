package com.api.apigateway.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${spring.rabbitmq.queue.log}")
    private String logQueue;

    // 创建队列
    @Bean
    public Queue logQueue() {
        return new Queue(logQueue, true); // 持久化队列
    }
}