package com.api.apiadmin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "app.redis")
@Data
public class RedisCleanupConfig {

    private boolean cleanupOnShutdown = true;
    private List<String> cleanupPatterns = new ArrayList<>();

    @Bean
    public ApplicationListener<ContextClosedEvent> redisCleanupListener(
            StringRedisTemplate redisTemplate) {
        return event -> {
            if (cleanupOnShutdown) {
                System.out.println("关闭应用，清理Redis登录数据...");

                for (String pattern : cleanupPatterns) {
                    Set<String> keys = redisTemplate.keys(pattern);
                    if (keys != null && !keys.isEmpty()) {
                        redisTemplate.delete(keys);
                        System.out.println("已删除模式 " + pattern + " 的数据: " + keys.size() + " 个");
                    }
                }
            }
        };
    }
}