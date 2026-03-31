package com.api.apiservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @GetMapping("/hello")
    public String hello() {
        return "这是业务接口，网关转发成功！";
    }

    @GetMapping("/list")
    public List<String> hello2() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        return list;
    }
}