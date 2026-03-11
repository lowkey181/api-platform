package com.api.apigateway.utils;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.util.Collections;

@Component
public class RedisLimitUtil {

    private static final Logger logger = LoggerFactory.getLogger(RedisLimitUtil.class);

    @Autowired
    private StringRedisTemplate redisTemplate;
//    private RedisTemplate<String, Object> redisTemplate;

    private static final DefaultRedisScript<Long> LIMIT_SCRIPT;

    static {
        LIMIT_SCRIPT = new DefaultRedisScript<>();
        LIMIT_SCRIPT.setLocation(new ClassPathResource("limit.lua"));
        LIMIT_SCRIPT.setResultType(Long.class);
    }

    /**
     * 限流判断
     * @param key 限流 key
     * @param maxCount 最大次数
     * @param time 时间（秒）
     * @return true=允许，false=限流
     */
    public boolean tryAcquire(String key, int maxCount, int time) {
        System.out.println("key:"+key+" maxCount:"+maxCount+" time:"+time);
        System.out.println(Collections.singletonList(key));
        try {
            Long count = redisTemplate.execute(
                    LIMIT_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(maxCount),    // ✅ 改为传变量，不要用硬编码 "100"
                    String.valueOf(time)
            );
            logger.info("Redis 限流成功，key: {}, count: {}", key, count);
            return count != null && count <= maxCount;
        } catch (DataAccessException e) {
            logger.error("Redis 限流失效，key: {}", key, e);
            return true;
        }
    }
}
