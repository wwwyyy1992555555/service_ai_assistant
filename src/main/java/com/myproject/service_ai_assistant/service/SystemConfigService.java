package com.myproject.service_ai_assistant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.service_ai_assistant.dto.SystemConfigDTO;
import com.myproject.service_ai_assistant.entity.SystemConfig;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService extends IService<SystemConfig> {

    /**
     * 获取系统配置
     * @param tenantId 租户 ID
     * @return 系统配置 DTO
     */
    SystemConfigDTO getConfig(Long tenantId);

    /**
     * 保存系统配置
     * @param tenantId 租户 ID
     * @param configDTO 配置 DTO
     * @return 是否成功
     */
    boolean saveConfig(Long tenantId, SystemConfigDTO configDTO);
}
