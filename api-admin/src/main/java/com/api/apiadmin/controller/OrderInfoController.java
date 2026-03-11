package com.api.apiadmin.controller;

import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.entity.OrderInfo;
import com.api.apiadmin.service.OrderInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orderInfo")
public class OrderInfoController {
    @Autowired
    private OrderInfoService orderInfoService;
    /**
     * 创建订单,未支付状态
     * 返回订单信息
     */
    @RequestMapping("/create")
    public SaResult create(OrderInfo orderInfo){
        return orderInfoService.insert(orderInfo);
    }
}
