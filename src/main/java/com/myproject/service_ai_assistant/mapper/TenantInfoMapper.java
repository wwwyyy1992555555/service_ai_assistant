package com.myproject.service_ai_assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproject.service_ai_assistant.entity.TenantInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户信息 Mapper 接口
 */
@Mapper
public interface TenantInfoMapper extends BaseMapper<TenantInfo> {

}
