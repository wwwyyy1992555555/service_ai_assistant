package com.myproject.service_ai_assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.service_ai_assistant.dto.TenantConfigDTO;
import com.myproject.service_ai_assistant.entity.TenantConfig;
import com.myproject.service_ai_assistant.entity.TenantInfo;
import com.myproject.service_ai_assistant.mapper.TenantConfigMapper;
import com.myproject.service_ai_assistant.mapper.TenantInfoMapper;
import com.myproject.service_ai_assistant.service.TenantConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 租户配置服务实现类
 */
@Slf4j
@Service
public class TenantConfigServiceImpl extends ServiceImpl<TenantConfigMapper, TenantConfig> implements TenantConfigService {

    @Autowired
    private TenantInfoMapper tenantInfoMapper;

    @Override
    public TenantConfigDTO getConfig(Long tenantId) {
        log.info("【获取租户配置】tenantId={}", tenantId);
        
        // 查询租户配置
        TenantConfig config = this.baseMapper.selectOne(
            new LambdaQueryWrapper<TenantConfig>()
                .eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getDeleted, 0)
        );
        
        // 转换为 DTO
        TenantConfigDTO dto = new TenantConfigDTO();
        if (config != null) {
            dto.setLogoUrl(config.getLogoUrl());
            dto.setWelcomeMessage(config.getWelcomeMessage());
            dto.setThemeColor(config.getThemeColor());
            dto.setServiceEmail(config.getServiceEmail());
            dto.setServicePhone(config.getServicePhone());
            dto.setServiceTime(config.getServiceTime());
        } else {
            // 默认值
            dto.setLogoUrl("");
            dto.setWelcomeMessage("您好，请问有什么可以帮您？");
            dto.setThemeColor("#1890ff");
            dto.setServiceEmail("");
            dto.setServicePhone("");
            dto.setServiceTime("工作时间：周一至周日 9:00-17:00");
        }
        
        // 从 tenant_info 获取租户名称
        try {
            TenantInfo tenantInfo = tenantInfoMapper.selectById(tenantId);
            if (tenantInfo != null) {
                dto.setTenantName(tenantInfo.getTenantName());
            }
        } catch (Exception e) {
            log.warn("【获取租户配置】获取租户名称失败：tenantId={}", tenantId, e);
        }
        
        log.debug("【获取租户配置成功】tenantId={}, config={}", tenantId, dto);
        
        return dto;
    }

    @Override
    public TenantConfig getByTenantId(Long tenantId) {
        log.debug("【快速加载租户配置】tenantId={}", tenantId);
        
        // 直接查询实体（不转换 DTO，用于登录时快速加载）
        TenantConfig config = this.baseMapper.selectOne(
            new LambdaQueryWrapper<TenantConfig>()
                .eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getDeleted, 0)
        );
        
        if (config == null) {
            log.debug("【快速加载租户配置】配置不存在，返回 null tenantId={}", tenantId);
            return null;
        }
        
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveConfig(Long tenantId, TenantConfigDTO configDTO) {
        log.info("【保存租户配置】tenantId={}, config={}", tenantId, configDTO);
        
        try {
            // 查询现有配置
            TenantConfig existingConfig = this.baseMapper.selectOne(
                new LambdaQueryWrapper<TenantConfig>()
                    .eq(TenantConfig::getTenantId, tenantId)
                    .eq(TenantConfig::getDeleted, 0)
            );
            
            TenantConfig config;
            if (existingConfig == null) {
                // 新建配置
                config = new TenantConfig();
                config.setTenantId(tenantId);
            } else {
                // 更新现有配置
                config = existingConfig;
            }
            
            // 设置配置值（不再处理 companyName，名称由 tenant_info 统一管理）
            config.setWelcomeMessage(configDTO.getWelcomeMessage());
            config.setThemeColor(configDTO.getThemeColor());
            config.setServiceEmail(configDTO.getServiceEmail());
            config.setServicePhone(configDTO.getServicePhone());
            config.setServiceTime(configDTO.getServiceTime());
            
            // 保存或更新
            boolean success = this.saveOrUpdate(config);
            
            if (success) {
                log.info("【保存租户配置成功】tenantId={}", tenantId);
                return true;
            } else {
                log.error("【保存租户配置失败】tenantId={}", tenantId);
                return false;
            }
        } catch (Exception e) {
            log.error("【保存租户配置异常】tenantId={}, error={}", tenantId, e.getMessage(), e);
            throw e; // 重新抛出异常，触发事务回滚
        }
    }

    /**
     * 创建默认配置
     */
    private TenantConfigDTO createDefaultConfig() {
        TenantConfigDTO dto = new TenantConfigDTO();
        dto.setLogoUrl("");
        dto.setWelcomeMessage("您好，请问有什么可以帮您？");
        dto.setThemeColor("#1890ff");
        dto.setServiceEmail("");
        dto.setServicePhone("");
        dto.setServiceTime("工作时间：周一至周日 9:00-17:00");
        return dto;
    }
}
