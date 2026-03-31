package com.api.apiadmin.config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {
    @Value("${spring.rabbitmq.exchange.log}")
    private String logExchangeName;
    @Value("${spring.rabbitmq.exchange.log-dlx}")
    private String logDlxExchangeName;
    @Value("${spring.rabbitmq.queue.log}")
    private String logQueueName;
    @Value("${spring.rabbitmq.queue.log-dlq}")
    private String logDlqName;
    @Value("${spring.rabbitmq.routing.log}")
    private String logRoutingKey;
    @Value("${spring.rabbitmq.routing.log-dlq}")
    private String logDlqRoutingKey;

    // 配置JSON消息转换器
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 让RabbitTemplate使用JSON序列化
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public DirectExchange logExchange() {
        return new DirectExchange(logExchangeName, true, false);
    }

    @Bean
    public DirectExchange logDlxExchange() {
        return new DirectExchange(logDlxExchangeName, true, false);
    }

    @Bean
    public Queue logQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", logDlxExchangeName);
        args.put("x-dead-letter-routing-key", logDlqRoutingKey);
        return new Queue(logQueueName, true, false, false, args);
    }

    @Bean
    public Queue logDlqQueue() {
        return new Queue(logDlqName, true);
    }

    @Bean
    public Binding logQueueBinding(Queue logQueue, DirectExchange logExchange) {
        return BindingBuilder.bind(logQueue).to(logExchange).with(logRoutingKey);
    }

    @Bean
    public Binding logDlqBinding(Queue logDlqQueue, DirectExchange logDlxExchange) {
        return BindingBuilder.bind(logDlqQueue).to(logDlxExchange).with(logDlqRoutingKey);
    }
}