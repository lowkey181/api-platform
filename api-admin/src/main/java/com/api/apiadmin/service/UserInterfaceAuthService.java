package com.api.apiadmin.service;

import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.ApiInterface;
import com.api.apiadmin.entity.OrderInfo;
import com.api.apiadmin.entity.UserInterfaceAuth;
import com.api.apiadmin.mapper.ApiCallLogMapper;
import com.api.apiadmin.mapper.ApiInterfaceMapper;
import com.api.apiadmin.mapper.UserInterfaceAuthMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

    public Result insert(UserInterfaceAuth userInterfaceAuth){
        userInterfaceAuth.setUserId((Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        userInterfaceAuthMapper.insert(userInterfaceAuth);
        return Result.ok("新增成功");
    }
    
    public Result update(UserInterfaceAuth userInterfaceAuth){
        userInterfaceAuth.setUserId((Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        userInterfaceAuthMapper.updateById(userInterfaceAuth);
        return Result.ok("更新成功");
    }
    
    public Result delete(Integer id){
        userInterfaceAuthMapper.deleteById(id);
        return Result.ok("删除成功");
    }
    
    public Result selectPage(Integer pageNum, Integer pageSize){
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LambdaQueryWrapper<UserInterfaceAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInterfaceAuth::getUserId, userId);
        wrapper.orderByDesc(UserInterfaceAuth::getCreateTime);
        Page<UserInterfaceAuth> page = new Page<>(pageNum, pageSize);
        Page<UserInterfaceAuth> result = page(page, wrapper);
        return Result.ok(result);
    }

    // 调用接口
    public Result callApi(Long userId, Long interfaceId){
        LambdaQueryWrapper<UserInterfaceAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInterfaceAuth::getUserId, userId)
               .eq(UserInterfaceAuth::getInterfaceId, interfaceId);
        UserInterfaceAuth userInterfaceAuth = userInterfaceAuthMapper.selectOne(wrapper);
        
        ApiInterface apiInterface = apiInterfaceMapper.selectById(interfaceId);
        if (apiInterface == null){
            return Result.error("接口不存在");
        }
        if (apiInterface.getStatus() == 0){
            return Result.error("此接口暂时不能使用");
        }
        if (userInterfaceAuth == null){
            return Result.error("未授权");
        }
        if (userInterfaceAuth.getStatus() == 0){
            return Result.error("您被禁止使用此接口");
        }
        if (userInterfaceAuth.getExpireTime() != null && userInterfaceAuth.getExpireTime().isBefore(LocalDateTime.now())){
            return Result.error("接口权限已过期");
        }
        if (userInterfaceAuth.getMaxCallCount() != -1 && userInterfaceAuth.getUsedCallCount() >= userInterfaceAuth.getMaxCallCount()){
            return Result.error("接口调用次数已用完");
        }
        userInterfaceAuth.setUsedCallCount(userInterfaceAuth.getUsedCallCount() + 1);
        userInterfaceAuthMapper.updateById(userInterfaceAuth);
        return Result.ok("调用接口成功");
    }

    public void addUserInterfaceCount(OrderInfo orderInfo){
        if (orderInfo == null){
            return;
        }
        UserInterfaceAuth userInterfaceAuth = getUserInterfaceAuth(orderInfo.getUserId(), orderInfo.getInterfaceId());
        if (userInterfaceAuth != null){
            userInterfaceAuth.setMaxCallCount(apiProductService.getCallCountById(orderInfo.getProductId()) + userInterfaceAuth.getMaxCallCount());
            userInterfaceAuthMapper.updateById(userInterfaceAuth);
        }else {
            UserInterfaceAuth dbuserInterfaceAuth = new UserInterfaceAuth();
            dbuserInterfaceAuth.setUserId(orderInfo.getUserId());
            dbuserInterfaceAuth.setInterfaceId(orderInfo.getInterfaceId());
            dbuserInterfaceAuth.setMaxCallCount(apiProductService.getCallCountById(orderInfo.getProductId()));
            dbuserInterfaceAuth.setUsedCallCount(0L);
            dbuserInterfaceAuth.setExpireTime(LocalDateTime.now().plusYears(1));
            dbuserInterfaceAuth.setStatus(1);
            userInterfaceAuthMapper.insert(dbuserInterfaceAuth);
        }
    }

    public UserInterfaceAuth getUserInterfaceAuth(Long userId, Long interfaceId) {
        LambdaQueryWrapper<UserInterfaceAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInterfaceAuth::getUserId, userId)
               .eq(UserInterfaceAuth::getInterfaceId, interfaceId);
        return userInterfaceAuthMapper.selectOne(wrapper);
    }
}
