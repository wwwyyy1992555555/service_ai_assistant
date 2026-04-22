package com.myproject.service_ai_assistant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.service_ai_assistant.entity.TenantInfo;

/**
 * 租户信息服务接口
 */
public interface TenantInfoService extends IService<TenantInfo> {

    /**
     * 分页查询租户列表
     * @param current 当前页码
     * @param size 每页大小
     * @param keyword 搜索关键词（租户编码或名称）
     * @param status 状态筛选（0-禁用，1-启用）
     * @param industryType 行业类型 ID
     * @return 分页结果
     */
    Page<TenantInfo> getTenantList(Integer current, Integer size, String keyword, Integer status, Integer industryType);

    /**
     * 创建租户
     * @param tenantInfo 租户信息
     * @return 创建的租户
     */
    TenantInfo createTenant(TenantInfo tenantInfo);

    /**
     * 删除租户
     * @param tenantId 租户 ID
     */
    void deleteTenant(Long tenantId);

    /**
     * 更新租户状态
     * @param tenantId 租户 ID
     * @param status 状态（0-禁用，1-启用）
     */
    void updateTenantStatus(Long tenantId, Integer status);

    /**
     * 获取租户详情
     * @param id 租户 ID
     * @return 租户信息
     */
    TenantInfo getTenantById(Long id);

    /**
     * 更新租户信息
     * @param tenantInfo 租户信息
     */
    void updateTenant(TenantInfo tenantInfo);

    /**
     * 发送注册验证码
     * @param contactEmail 联系邮箱
     */
    void sendVerifyCode(String contactEmail);

    /**
     * 租户自助注册
     * @param tenantName 企业名称
     * @param contactPerson 联系人
     * @param contactPhone 联系电话
     * @param contactEmail 联系邮箱
     * @param verifyCode 邮箱验证码
     * @param adminUsername 管理员用户名
     * @param adminPassword 管理员密码
     * @return 生成的租户编码（tenant_code）
     */
    String registerTenant(String tenantName, String contactPerson, String contactPhone, 
                         String contactEmail, String verifyCode, String adminUsername, String adminPassword);
}
