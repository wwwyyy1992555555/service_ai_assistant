package com.myproject.service_ai_assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.service_ai_assistant.dto.SystemConfigDTO;
import com.myproject.service_ai_assistant.entity.SystemConfig;
import com.myproject.service_ai_assistant.mapper.SystemConfigMapper;
import com.myproject.service_ai_assistant.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务实现类
 */
@Slf4j
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    // 配置键常量定义
    private static final String KEY_COMPANY_NAME = "company_name";
    private static final String KEY_WELCOME_MESSAGE = "welcome_message";
    private static final String KEY_THEME_COLOR = "theme_color";
    private static final String KEY_SERVICE_EMAIL = "service_email";
    private static final String KEY_SERVICE_PHONE = "service_phone";
    private static final String KEY_SERVICE_TIME = "service_time";

    @Override
    public SystemConfigDTO getConfig(Long tenantId) {
        log.info("【获取系统配置】tenantId={}", tenantId);
        
        // 查询租户的所有配置
        List<SystemConfig> configs = this.baseMapper.selectByTenantId(tenantId);
        
        // 转换为 Map 方便处理
        Map<String, String> configMap = new HashMap<>();
        for (SystemConfig config : configs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }
        
        // 组装 DTO
        SystemConfigDTO dto = new SystemConfigDTO();
        dto.setCompanyName(configMap.getOrDefault(KEY_COMPANY_NAME, ""));
        dto.setWelcomeMessage(configMap.getOrDefault(KEY_WELCOME_MESSAGE, ""));
        dto.setThemeColor(configMap.getOrDefault(KEY_THEME_COLOR, "#1890ff"));
        dto.setEmail(configMap.getOrDefault(KEY_SERVICE_EMAIL, ""));
        dto.setPhone(configMap.getOrDefault(KEY_SERVICE_PHONE, ""));
        dto.setServiceTime(configMap.getOrDefault(KEY_SERVICE_TIME, "工作时间：周一至周日 9:00-17:00"));
        
        log.debug("【获取系统配置成功】tenantId={}, config={}", tenantId, dto);
        
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveConfig(Long tenantId, SystemConfigDTO configDTO) {
        log.info("【保存系统配置】tenantId={}, config={}", tenantId, configDTO);
        
        try {
            // 准备配置列表
            List<SystemConfig> configs = Arrays.asList(
                createConfig(tenantId, KEY_COMPANY_NAME, configDTO.getCompanyName(), "string", "企业名称"),
                createConfig(tenantId, KEY_WELCOME_MESSAGE, configDTO.getWelcomeMessage(), "text", "欢迎语"),
                createConfig(tenantId, KEY_THEME_COLOR, configDTO.getThemeColor(), "color", "主题颜色"),
                createConfig(tenantId, KEY_SERVICE_EMAIL, configDTO.getEmail(), "string", "客服邮箱"),
                createConfig(tenantId, KEY_SERVICE_PHONE, configDTO.getPhone(), "string", "客服电话"),
                createConfig(tenantId, KEY_SERVICE_TIME, configDTO.getServiceTime(), "text", "工作时间描述")
            );
            
            // 批量插入或更新
            int result = this.baseMapper.batchInsertOrUpdate(configs);
            
            if (result > 0) {
                log.info("【保存系统配置成功】tenantId={}", tenantId);
                return true;
            } else {
                log.error("【保存系统配置失败】tenantId={}", tenantId);
                return false;
            }
        } catch (Exception e) {
            log.error("【保存系统配置异常】tenantId={}, error={}", tenantId, e.getMessage(), e);
            throw e; // 重新抛出异常，触发事务回滚
        }
    }

    /**
     * 创建配置对象
     */
    private SystemConfig createConfig(Long tenantId, String key, String value, String type, String remark) {
        SystemConfig config = new SystemConfig();
        config.setTenantId(tenantId);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigType(type);
        config.setRemark(remark);
        return config;
    }
}
