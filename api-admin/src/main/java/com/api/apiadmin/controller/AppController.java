package com.api.apiadmin.controller;

import com.api.apiadmin.config.Result;
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
        return Result.ok(appService.insert(appName));
    }
}