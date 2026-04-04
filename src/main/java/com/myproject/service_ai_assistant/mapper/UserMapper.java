package com.myproject.service_ai_assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproject.service_ai_assistant.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工信息 Mapper
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
    
    /**
     * 根据租户 ID 查询用户列表
     * @param tenantId 租户 ID
     * @return 用户列表
     */
    List<User> selectByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * 根据租户 ID 搜索用户（支持关键词搜索租户名、用户名、邮箱）
     * @param tenantId 租户 ID
     * @param keyword 搜索关键词
     * @return 用户列表
     */
    List<User> searchByTenantId(@Param("tenantId") Long tenantId, @Param("keyword") String keyword);
    
    /**
     * 登录时联合查询用户和租户配置
     * @param tenantId 租户 ID
     * @param username 用户名
     * @return 包含租户配置信息的用户对象（通过 @Results 映射额外字段）
     */
    java.util.Map<String, Object> selectUserWithConfig(@Param("tenantId") Long tenantId, @Param("username") String username);
    
    /**
     * 查询所有同名用户（包括已删除的，忽略 @TableLogic）
     * @param tenantId 租户 ID
     * @param username 用户名
     * @return 用户列表
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM user_info WHERE tenant_id = #{tenantId} AND username = #{username}")
    java.util.List<User> selectAllByUsernameIncludingDeleted(@Param("tenantId") Long tenantId, @Param("username") String username);
    
    /**
     * 物理删除用户（真正从数据库删除记录）
     * @param userId 用户 ID
     * @return 影响行数
     */
    int physicallyDeleteById(@Param("id") Long userId);
}