package com.api.apiadmin.service;

import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.entity.ApiInterface;
import com.api.apiadmin.entity.OrderInfo;
import com.api.apiadmin.entity.UserInterfaceAuth;
import com.api.apiadmin.mapper.ApiCallLogMapper;
import com.api.apiadmin.mapper.ApiInterfaceMapper;
import com.api.apiadmin.mapper.UserInterfaceAuthMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserInterfaceAuthService extends ServiceImpl<UserInterfaceAuthMapper, UserInterfaceAuth> {
    @Autowired
    private UserInterfaceAuthMapper userInterfaceAuthMapper;
    @Autowired
    private ApiInterfaceMapper apiInterfaceMapper;
    @Autowired
    private ApiCallLogMapper apiCallLogMapper;
    @Autowired
    private ApiProductService apiProductService;

    public SaResult insert(UserInterfaceAuth userInterfaceAuth){
        userInterfaceAuth.setUserId((Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        userInterfaceAuthMapper.insert(userInterfaceAuth);
        return SaResult.ok();
    }
    public SaResult update(UserInterfaceAuth userInterfaceAuth){
        userInterfaceAuth.setUserId((Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        userInterfaceAuthMapper.updateById(userInterfaceAuth);
        return SaResult.ok();
    }
    public SaResult delete(Integer id){
        userInterfaceAuthMapper.deleteById(id);
        return SaResult.ok();
    }
    public SaResult selectPage(Integer pageNum, Integer pageSize){
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        MPJLambdaWrapper<UserInterfaceAuth> wrapper = new MPJLambdaWrapper<UserInterfaceAuth>()
                .selectAll(UserInterfaceAuth.class)
                .eq(UserInterfaceAuth::getUserId, userId);
        Page<UserInterfaceAuth> page = new Page<>(pageNum, pageSize);
        Page<UserInterfaceAuth> pageResult = userInterfaceAuthMapper.selectJoinPage(page, UserInterfaceAuth.class, wrapper);
//        return Result.ok(pageResult);
        return SaResult.ok().setData(pageResult);
    }

    // 调用接口
    public SaResult callApi(Long userId,Long interfaceId){
        MPJLambdaWrapper<UserInterfaceAuth> wrapper = new MPJLambdaWrapper<UserInterfaceAuth>()
                .select(UserInterfaceAuth::getExpireTime, UserInterfaceAuth::getMaxCallCount, UserInterfaceAuth::getStatus, UserInterfaceAuth::getUsedCallCount)
                .eq(UserInterfaceAuth::getUserId, userId)
                .eq(UserInterfaceAuth::getInterfaceId, interfaceId);
        UserInterfaceAuth userInterfaceAuth = userInterfaceAuthMapper.selectJoinOne(UserInterfaceAuth.class, wrapper);
        ApiInterface apiInterface = apiInterfaceMapper.selectById(interfaceId);
        if (apiInterface == null){
            return SaResult.error("接口不存在");
        }
        if (apiInterface.getStatus() == 0){
            return SaResult.error("此接口暂时不能使用");
        }
        if (userInterfaceAuth == null){
            return SaResult.error("未授权");
        }
        if (userInterfaceAuth.getStatus() == 0){
            return SaResult.error("您被禁止使用此接口");
        }
        if (userInterfaceAuth.getExpireTime().isBefore(LocalDateTime.now())){
            return SaResult.error("接口权限已过期");
        }
        if (userInterfaceAuth.getMaxCallCount() != -1 && userInterfaceAuth.getUsedCallCount() >= userInterfaceAuth.getMaxCallCount()){//-1 表示不限制调用次数
            return SaResult.error("接口调用次数已用完");
        }
        userInterfaceAuth.setUsedCallCount(userInterfaceAuth.getUsedCallCount() + 1);
        System.out.println("更新用户接口权限：" + userInterfaceAuth);
        userInterfaceAuthMapper.update(userInterfaceAuth,
                new MPJLambdaWrapper<UserInterfaceAuth>()
                        .eq(UserInterfaceAuth::getUserId, userId)
                        .eq(UserInterfaceAuth::getInterfaceId, interfaceId));
//        ApiCallLog apiCallLog = new ApiCallLog();
//        apiCallLog.setAccessKey(accessKey);
//        apiCallLog.setInterfaceId(interfaceId);
//        apiCallLog.setRequestIp("127.0.0.1");
//        apiCallLog.setRequestParams("{}");
//        apiCallLog.setResponseResult("{}");
//        apiCallLog.setUseTime(0L);
//        apiCallLog.setStatus("success");
//        apiCallLog.setErrorMsg("");
//        apiCallLogMapper.insert(apiCallLog);


        return SaResult.ok("调用接口成功");
    }

    public void addUserInterfaceCount(OrderInfo orderInfo){
        if (orderInfo== null){
            SaResult.error("订单不存在");
            return;
        }
        UserInterfaceAuth userInterfaceAuth = getUserInterfaceAuth(orderInfo.getUserId(), orderInfo.getInterfaceId());
        if (userInterfaceAuth != null){
            userInterfaceAuth.setMaxCallCount(apiProductService.getCallCountById(orderInfo.getProductId())+1);
            userInterfaceAuthMapper.updateById(userInterfaceAuth);
            SaResult.ok("余额添加成功");
        }else {
            UserInterfaceAuth dbuserInterfaceAuth = new UserInterfaceAuth();
            dbuserInterfaceAuth.setUserId(orderInfo.getUserId());
            dbuserInterfaceAuth.setInterfaceId(orderInfo.getInterfaceId());
            dbuserInterfaceAuth.setMaxCallCount(apiProductService.getCallCountById(orderInfo.getProductId()));
            dbuserInterfaceAuth.setUsedCallCount(1L);
            dbuserInterfaceAuth.setExpireTime(null);
            dbuserInterfaceAuth.setStatus(1);
            userInterfaceAuthMapper.insert(dbuserInterfaceAuth);
            SaResult.ok("接口创建（购买）成功");
        }
    }

    public UserInterfaceAuth getUserInterfaceAuth(Long userId, Long interfaceId) {
        MPJLambdaWrapper<UserInterfaceAuth> wrapper = new MPJLambdaWrapper<UserInterfaceAuth>()
                .selectAll(UserInterfaceAuth.class)
                .eq(UserInterfaceAuth::getUserId, userId)
                .eq(UserInterfaceAuth::getInterfaceId, interfaceId);
        return userInterfaceAuthMapper.selectJoinOne(UserInterfaceAuth.class, wrapper);
    }
}
