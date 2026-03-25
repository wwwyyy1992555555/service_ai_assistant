package com.myproject.service_ai_assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproject.service_ai_assistant.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户信息 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     * @param tenantId 租户 ID
     * @param username 用户名
     * @return 用户信息
     */
    User selectByUsername(@Param("tenantId") Long tenantId, @Param("username") String username);

    /**
     * 根据手机号查询用户
     * @param tenantId 租户 ID
     * @param phone 手机号
     * @return 用户信息
     */
    User selectByPhone(@Param("tenantId") Long tenantId, @Param("phone") String phone);
}
