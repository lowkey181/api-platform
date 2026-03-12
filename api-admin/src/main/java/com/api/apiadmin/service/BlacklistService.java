package com.api.apiadmin.service;

import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.entity.Blacklist;
import com.api.apiadmin.mapper.BlacklistMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlacklistService extends ServiceImpl<BlacklistMapper, Blacklist> {
    @Autowired
    private BlacklistMapper blacklistMapper;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    private static final String BLACKLIST_IP_KEY = "blacklist:ip:";
    private static final String BLACKLIST_ACCESS_KEY = "blacklist:accessKey:";

    public void redisAddIpAccess() {
        MPJLambdaWrapper<Blacklist> wrapper = new MPJLambdaWrapper<Blacklist>()
                .select(Blacklist::getIp)
                .eq(Blacklist::getStatus, 1);
        List<Blacklist> listIp = list(wrapper);
        for (Blacklist blacklist : listIp) {
            redisTemplate.opsForValue().set(BLACKLIST_IP_KEY + blacklist.getIp(), "1");
        }
        MPJLambdaWrapper<Blacklist> wrapper1 = new MPJLambdaWrapper<Blacklist>()
                .select(Blacklist::getAccessKey)
                .eq(Blacklist::getStatus, 1);
        List<Blacklist> listAccess = list(wrapper1);
        for (Blacklist blacklist : listAccess) {
            redisTemplate.opsForValue().set(BLACKLIST_ACCESS_KEY + blacklist.getAccessKey(), "1");
        }
        SaResult.ok().setData(list(wrapper));
    }
    public SaResult addBlacklist(Blacklist blacklist) {
        if (blacklist.getIp() == null && blacklist.getAccessKey() == null) {
            return SaResult.error("IP和accessKey不能同时为空");
        }
        if (blacklist.getIp()!= null && blacklist.getAccessKey() != null) {
            return SaResult.error("IP和accessKey不能同时存在");
        }
        if (blacklist.getIp() != null) {
            if (redisTemplate.opsForValue().get(BLACKLIST_IP_KEY + blacklist.getIp()) != null) {
                return SaResult.error("IP已存在");
            }
            redisTemplate.opsForValue().set(BLACKLIST_IP_KEY + blacklist.getIp(), "1");
        }else {
            if (redisTemplate.opsForValue().get(BLACKLIST_ACCESS_KEY + blacklist.getAccessKey()) != null) {
                return SaResult.error("accessKey已存在");
            }
            redisTemplate.opsForValue().set(BLACKLIST_ACCESS_KEY + blacklist.getAccessKey(), "1");
        }
        blacklist.setStatus(1);
        boolean save = save(blacklist);

        if (save) {
            return SaResult.ok("添加成功");
        }
        return SaResult.error("添加失败");
    }
    public SaResult deleteBlacklist(Long id) {
        boolean removeById = removeById(id);
        if (removeById) {
            redisTemplate.delete(BLACKLIST_IP_KEY);
            redisTemplate.delete(BLACKLIST_ACCESS_KEY);
            redisAddIpAccess();
            return SaResult.ok("删除成功");
        }
        return SaResult.error("删除失败");
    }
    public SaResult updateBlacklist(Blacklist blacklist) {
        boolean updateById = updateById(blacklist);
        if (updateById) {
            redisTemplate.delete(BLACKLIST_IP_KEY);
            redisTemplate.delete(BLACKLIST_ACCESS_KEY);
            redisAddIpAccess();
            return SaResult.ok("修改成功");
        }
        return SaResult.error("修改失败");
    }
    public SaResult getPage(Integer pageNum, Integer pageSize,Integer status) {
        if (redisTemplate.opsForValue().get(BLACKLIST_IP_KEY)==null&&redisTemplate.opsForValue().get(BLACKLIST_ACCESS_KEY)==null){
            redisAddIpAccess();
        }
        MPJLambdaWrapper<Blacklist> wrapper = new MPJLambdaWrapper<Blacklist>()
                .selectAll()
                .eq(Blacklist::getStatus, status);
        Page<Blacklist> page = new Page<>(pageNum, pageSize);
        Page<Blacklist> pageResult = blacklistMapper.selectJoinPage(page, Blacklist.class, wrapper);
        return SaResult.ok().setData(pageResult);
    }


}
