package com.api.apiadmin.mapper;

import com.api.apiadmin.entity.UserInterfaceAuth;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserInterfaceAuthMapper extends MPJBaseMapper<UserInterfaceAuth> {

    @Update("""
            UPDATE user_interface_auth
            SET used_call_count = used_call_count + 1
            WHERE id = #{id}
              AND status = 1
              AND (max_call_count = -1 OR used_call_count < max_call_count)
              AND (expire_time IS NULL OR expire_time >= NOW())
            """)
    int increaseUsedCallCountIfAllowed(@Param("id") Long id);
}
