package com.api.apiadmin.mapper;

import com.api.apiadmin.entity.User;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends MPJBaseMapper<User> {
    @Select("select * from user where username = #{username}")
    User selectByUsername(String username);
}
