package com.api.apiadmin.service;

import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.entity.OrderInfo;
import com.api.apiadmin.entity.UserInterfaceAuth;
import com.api.apiadmin.mapper.OrderInfoMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class OrderInfoService extends ServiceImpl<OrderInfoMapper, OrderInfo> {
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private UserInterfaceAuthService userInterfaceAuthService;
    /*
    创建订单
     */
    public SaResult insert(OrderInfo orderInfo){
        Long timestamp=System.currentTimeMillis()*1000000+new Random().nextInt(1000000);
        UserInterfaceAuth userInterfaceAuth =userInterfaceAuthService.getUserInterfaceAuth(orderInfo.getUserId(), orderInfo.getInterfaceId());
        if (userInterfaceAuth!=null){
            if (userInterfaceAuth.getStatus()==0){
                return SaResult.error("您被禁止使用此接口,不能购买相关产品");
            }
        }

        orderInfo.setUserId((Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        orderInfo.setPayStatus(0);//0未支付 1已支付 2已取消
        orderInfo.setOrderNo(String.valueOf(timestamp));
        orderInfoMapper.insert(orderInfo);
        return SaResult.ok().setData(orderInfo);
    }
    public SaResult update(OrderInfo orderInfo){
        orderInfoMapper.updateById(orderInfo);
        return SaResult.ok();
    }

    public OrderInfo getByOrderNo(String orderNo){
        MPJLambdaWrapper<OrderInfo> wrapper = new MPJLambdaWrapper<OrderInfo>()
                .selectAll(OrderInfo.class)
                .eq(OrderInfo::getOrderNo, orderNo);
        return orderInfoMapper.selectOne(wrapper);
    }
    public SaResult delete(Integer id){
        orderInfoMapper.deleteById(id);
        return SaResult.ok();
    }
    public SaResult selectPage(Integer pageNum, Integer pageSize){
        Page<OrderInfo> page = new Page<>(pageNum, pageSize);
        Page<OrderInfo> orderInfo =orderInfoMapper.selectPage(page, null);
        return SaResult.ok().setData(orderInfo);
    }

}
