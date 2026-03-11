package com.api.apiadmin.service;

import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.ApiInterface;
import com.api.apiadmin.mapper.ApiInterfaceMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApiInterfaceService extends ServiceImpl<ApiInterfaceMapper, ApiInterface> {
    @Autowired
    private ApiInterfaceMapper apiInterfaceMapper;

    public Result insert(ApiInterface apiInterface){
        return Result.ok(apiInterfaceMapper.insert(apiInterface));
    }

    public Result update(ApiInterface apiInterface){
        return Result.ok(apiInterfaceMapper.updateById(apiInterface));
    }

    public Result delete(Integer id){
        return Result.ok(apiInterfaceMapper.deleteById(id));
    }

    public Result selectPage(Integer pageNum, Integer pageSize){
        Page<ApiInterface> page = new Page<>(pageNum, pageSize);
        Page<ApiInterface> apiInterface =apiInterfaceMapper.selectPage(page, null);
        return Result.ok(apiInterface);
    }


}
//public SaResult selectPage(long current,
//                           long size,
//                           String name,
//                           String realName,
//                           Integer userId,
//                           Integer status) {
//    MPJLambdaWrapper<AliveProject> wrapper = new MPJLambdaWrapper<AliveProject>()
//            .selectAll(AliveProject.class)
//            .select(ProjectCategory::getName)
//            .select(Admin::getRealName)
//            .leftJoin(Admin.class, Admin::getId, AliveProject::getUserId)
//            .like(StrUtil.isNotBlank(name), AliveProject::getName, name)
//            .like(StrUtil.isNotBlank(realName), Admin::getRealName, realName)
//            .eq(ObjectUtil.isNotNull(userId), AliveProject::getUserId, userId)
//            .eq(ObjectUtil.isNotNull(status), AliveProject::getStatus, status);
//    Page<AliveProject> page = new Page<>(current, size);
//    Page<AliveProject> pageResult = aliveProjectMapper.selectJoinPage(page, AliveProject.class, wrapper);
//    return SaResult.ok().setData(pageResult);
//}