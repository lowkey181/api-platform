package com.api.apigateway.utils;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class RedisLimitUtil {

    private static final Logger logger = LoggerFactory.getLogger(RedisLimitUtil.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> SLIDING_LIMIT_SCRIPT;

    static {
        SLIDING_LIMIT_SCRIPT = new DefaultRedisScript<>();
        SLIDING_LIMIT_SCRIPT.setLocation(new ClassPathResource("limit-sliding.lua"));
        SLIDING_LIMIT_SCRIPT.setResultType(Long.class);
    }

    /**
     * 滑动窗口限流判断
     * @param key 限流 key
     * @param maxCount 窗口内最大请求数
     * @param windowMs 窗口大小（毫秒）
     * @return true=允许，false=限流
     */
    public boolean tryAcquire(String key, int maxCount, long windowMs) {
        try {
            Long result = redisTemplate.execute(
                    SLIDING_LIMIT_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(windowMs),
                    String.valueOf(maxCount)
            );
            return result != null && result > 0;
        } catch (DataAccessException e) {
            logger.error("Redis 限流失效，key: {}", key, e);
            return true;
        }
    }
}
