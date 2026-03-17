package com.api.apiadmin.controller;

import com.api.apiadmin.config.Result;
import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.mapper.AppMapper;
import com.api.apiadmin.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/app")
public class AppController {

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AppService appService;
    @Autowired
    private AppMapper appMapper;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/key/create")
    public Result createKey(@RequestParam String appName) {
        return appService.insert(appName);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("/list")
    public Result listApps() {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return Result.ok(appService.getByUserId(userId));
    }
    @PreAuthorize("isAuthenticated()")
    @RequestMapping("/admin/list")
    public Result AllApps() {
        return Result.ok(appService.getAllApps());
    }

    /**
     * 获取应用数量,不包括禁用的
     */
    @PreAuthorize("isAuthenticated()")
    @RequestMapping("/admin/count")
    public SaResult getAppCount() {
        return appService.getAppCount();
    }
}