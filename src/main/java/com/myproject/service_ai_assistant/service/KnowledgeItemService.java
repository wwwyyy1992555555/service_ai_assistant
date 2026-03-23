package com.myproject.service_ai_assistant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.service_ai_assistant.entity.KnowledgeItem;

import java.util.List;

/**
 * 知识条目服务接口
 */
public interface KnowledgeItemService extends IService<KnowledgeItem> {

    /**
     * 搜索知识库（根据关键词匹配）
     * @param tenantId 租户 ID
     * @param keyword 关键词
     * @return 匹配的知识列表
     */
    List<KnowledgeItem> searchKnowledge(Long tenantId, String keyword);

    /**
     * 获取热门问题
     * @param tenantId 租户 ID
     * @param limit 数量限制
     * @return 热门问题列表
     */
    List<KnowledgeItem> getHotQuestions(Long tenantId, Integer limit);
}
