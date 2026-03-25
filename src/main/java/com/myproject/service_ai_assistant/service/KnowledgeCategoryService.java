package com.myproject.service_ai_assistant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.service_ai_assistant.entity.KnowledgeCategory;

import java.util.List;

/**
 * 知识库分类服务接口
 */
public interface KnowledgeCategoryService extends IService<KnowledgeCategory> {

    /**
     * 查询分类列表
     * @param tenantId 租户 ID
     * @return 分类列表
     */
    List<KnowledgeCategory> queryCategoryList(Long tenantId);
}
