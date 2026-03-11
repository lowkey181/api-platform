//package com.api.apiadmin.auto;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.event.ContextClosedEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.core.annotation.Order;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.Set;
//
//@Component
//@Slf4j
//public class RedisCleanupAspect {
//
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//
//    @EventListener(ContextClosedEvent.class)
//    @Order(1)  // 控制执行顺序
//    public void handleContextClosed() {
//        log.info("接收到应用关闭事件，开始清理Redis数据...");
//
//        try {
//            // 清理临时数据
//            cleanupTemporaryData();
//
//            // 清理会话数据
////            cleanupSessionData();
//
//            log.info("Redis数据清理完成");
//        } catch (Exception e) {
//            log.error("清理Redis数据时发生错误", e);
//        }
//    }
//
//    private void cleanupTemporaryData() {
//        Set<String> tempKeys = redisTemplate.keys("login:user:");
//        if (tempKeys != null && !tempKeys.isEmpty()) {
//            redisTemplate.delete(tempKeys);
//            log.info("已删除 {} 个临时数据", tempKeys.size());
//        }
//    }
//
////    private void cleanupSessionData() {
////        Set<String> sessionKeys = redisTemplate.keys("session:*");
////        if (sessionKeys != null && !sessionKeys.isEmpty()) {
////            redisTemplate.delete(sessionKeys);
////            log.info("已删除 {} 个会话数据", sessionKeys.size());
////        }
////    }
//}