package com.myproject.service_ai_assistant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.service_ai_assistant.entity.IndustryType;

import java.util.List;

/**
 * 行业类型 Service
 */
public interface IndustryTypeService extends IService<IndustryType> {

    /**
     * 获取所有启用的行业类型列表
     */
    List<IndustryType> getActiveTypes();
}
