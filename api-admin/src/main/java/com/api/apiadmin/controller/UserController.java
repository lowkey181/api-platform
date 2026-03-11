package com.api.apiadmin.controller;

import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.User;
import com.api.apiadmin.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @RequestMapping("/login")
    public Result login(String username, String password) {
        return userService.login(username, password);
    }

    // 获取当前登录用户信息
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/info")
    public Result info() {
        // 从 Security 上下文获取当前登录用户 ID
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User user = userService.getById(userId);
        user.setPassword(null); // 密码不返回
        return Result.ok(user);
    }
    @RequestMapping("/logout")
    public Result logout(HttpServletRequest request) {
        return userService.logout(request);
    }

    @RequestMapping("/register")
    public Result register(User user) {
        return userService.register(user);
    }

    @RequestMapping("/qw")
    public Result oaljksd() {
        return Result.ok("asdda");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test")
    public Result info4() {
        return Result.ok("asdda");
    }
}
