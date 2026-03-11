//package com.api.apiadmin.config;
//
//import cn.hutool.core.util.StrUtil;
//import com.api.apiadmin.util.JwtUtil;
//import io.jsonwebtoken.Claims;
//import jakarta.annotation.Resource;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.web.servlet.HandlerInterceptor;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//
//public class JwtInterceptor implements HandlerInterceptor {
//
//    private final JwtUtil jwtUtil;
//    @Resource
//    private StringRedisTemplate redisTemplate;
//
//    public JwtInterceptor(JwtUtil jwtUtil) {
//        this.jwtUtil = jwtUtil;
//    }
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        // 1. 获取请求头 token
//        String token = request.getHeader("Authorization");
//
//        // 2. 没有 token
//        if (StrUtil.isBlank(token)) {
//            response.setStatus(401);
//            return false;
//        }
//
//        // 3. 去掉 Bearer 前缀
//        if (token.startsWith("Bearer ")) {
//            token = token.substring(7);
//        }
//
//        // 4. 验证 token
//        Claims claims = jwtUtil.extractAllClaims(token);
//        if (claims == null) {
//            response.setStatus(401);
//            return false;
//        }
//
//        Long userId = Long.parseLong(claims.getSubject());
//        String redisKey = "login:user:" + userId;
//
//        //  核心：Redis 不存在 = 未登录
//        Boolean hasKey = redisTemplate.hasKey(redisKey);
//        if (!hasKey) {
//            response.setStatus(401);
//            return false;
//        }
//
//
//        request.setAttribute("userId", userId);
//        return true;
//    }
//}
