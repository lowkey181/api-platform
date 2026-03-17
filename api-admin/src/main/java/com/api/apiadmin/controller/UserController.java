package com.api.apiadmin.controller;

import com.api.apiadmin.config.Result;
import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.entity.User;
import com.api.apiadmin.service.AppService;
import com.api.apiadmin.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private AppService appService;
    
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
        user.setAccessKey(appService.getAllAccessKey(userId));
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

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        return userService.selectPage(pageNum, pageSize);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping("/updateStatus")
    public Result updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        return userService.updateStatus(id, status);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping("/delete")
    public Result deleteUser(@RequestParam Long id) {
        return userService.deleteUser(id);
    }
    // 获取用户数量,不包括禁用的
    @RequestMapping("/getUserCount")
    public SaResult getUserCount() {
        return userService.getUserCount();
    }
}
