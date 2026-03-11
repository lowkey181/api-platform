package com.api.apiadmin.service;

import cn.hutool.crypto.digest.BCrypt;
import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.User;
import com.api.apiadmin.mapper.UserMapper;
import com.api.apiadmin.util.JwtUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public Result login(String username, String password) {
        // 1. 查询用户
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 2. 校验密码
        if (!BCrypt.checkpw(password, user.getPassword())) {
            return Result.error("密码错误");
        }

        String redisKey = "login:user:" + user.getId();
        Boolean hasLogin = redisTemplate.hasKey(redisKey);

        // 已登录 → 直接返回，不让重复登录
        if (hasLogin) {
            redisTemplate.delete(redisKey);
//            return Result.fail("您已登录，请勿重复登录！");
        }

        // 3. 生成 JWT
        String token = jwtUtil.generateToken(user.getId(), username, user.getRole());
        // 4. 存入 Redis（2小时过期）
        redisTemplate.opsForValue().set(redisKey, token, 2, TimeUnit.HOURS);
        // 5. 返回成功 + token
        return Result.ok(token);
    }
    public Result logout(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String redisKey = "login:user:" + userId;
        redisTemplate.delete(redisKey);
        return Result.ok("退出成功");
    }

    public Result register(User user) {
        User exist=userMapper.selectByUsername(user.getUsername());
        if(exist!=null){
            return Result.error("用户名已存在");
        }
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        user.setStatus(1);
        user.setRole("USER");
        user.setCreateTime(null);
        user.setUpdateTime( null);
        userMapper.insert(user);
        return Result.ok("注册成功");
    }

}
