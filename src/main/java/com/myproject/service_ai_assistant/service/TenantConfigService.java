package com.myproject.service_ai_assistant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.service_ai_assistant.dto.TenantConfigDTO;
import com.myproject.service_ai_assistant.entity.TenantConfig;

/**
 * 租户配置服务接口
 */
public interface TenantConfigService extends IService<TenantConfig> {

    /**
     * 获取租户配置
     * @param tenantId 租户 ID
     * @return 配置 DTO
     */
    TenantConfigDTO getConfig(Long tenantId);

    /**
     * 根据租户 ID 获取配置实体（用于登录时快速加载）
     * @param tenantId 租户 ID
     * @return 租户配置实体
     */
    TenantConfig getByTenantId(Long tenantId);

    /**
     * 保存租户配置
     * @param tenantId 租户 ID
     * @param configDTO 配置 DTO
     * @return 是否成功
     */
    boolean saveConfig(Long tenantId, TenantConfigDTO configDTO);
}
