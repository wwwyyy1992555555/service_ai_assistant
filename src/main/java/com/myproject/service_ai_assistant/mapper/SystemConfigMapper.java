package com.myproject.service_ai_assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproject.service_ai_assistant.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置 Mapper 接口
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 根据租户 ID 和配置键查询配置
     */
    SystemConfig selectByTenantAndKey(@Param("tenantId") Long tenantId, 
                                      @Param("configKey") String configKey);

    /**
     * 查询租户的所有配置
     */
    List<SystemConfig> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 批量插入或更新配置
     */
    int batchInsertOrUpdate(@Param("configs") List<SystemConfig> configs);
}
