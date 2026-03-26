package com.myproject.service_ai_assistant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.service_ai_assistant.entity.KnowledgeItem;

import java.util.List;

/**
 * 知识条目服务接口
 */
public interface KnowledgeItemService extends IService<KnowledgeItem> {

    /**
     * 搜索知识库 (根据关键词匹配)
     * @param tenantId 租户 ID
     * @param keyword 关键词
     * @return 匹配的知识列表
     */
    List<KnowledgeItem> searchKnowledge(Long tenantId, String keyword);

    /**
     * 搜索知识库 (根据关键词 + 筛选条件) - 分页版本
     * @param tenantId 租户 ID
     * @param keyword 关键词
     * @param publishStatus 发布状态 (null:全部，0:草稿，1:已发布)
     * @param isTop 是否置顶 (null:全部，0:未置顶，1:已置顶)
     * @param page 分页参数
     * @return 分页结果
     */
    Page<KnowledgeItem> searchKnowledgeWithFiltersPage(Long tenantId, String keyword, Integer publishStatus, Integer isTop, Page<KnowledgeItem> page);

    /**
     * 获取热门问题
     * @param tenantId 租户 ID
     * @param limit 数量限制
     * @return 热门问题列表
     */
    List<KnowledgeItem> getHotQuestions(Long tenantId, Integer limit);

    /**
     * 分页查询知识列表
     * @param tenantId 租户 ID
     * @param categoryId 分类 ID(可选)
     * @param page 分页参数
     * @return 分页结果
     */
    Page<KnowledgeItem> queryKnowledgeList(Long tenantId, Long categoryId, Page<KnowledgeItem> page);

    /**
     * 分页查询知识列表 (带筛选条件)
     * @param tenantId 租户 ID
     * @param categoryId 分类 ID(可选)
     * @param publishStatus 发布状态 (null:全部，0:草稿，1:已发布)
     * @param isTop 是否置顶 (null:全部，0:未置顶，1:已置顶)
     * @param page 分页参数
     * @return 分页结果
     */
    Page<KnowledgeItem> queryKnowledgeListWithFilters(Long tenantId, Long categoryId, Integer publishStatus, Integer isTop, Page<KnowledgeItem> page);

    /**
     * 统计知识库总数
     * @param tenantId 租户 ID
     * @return 知识总数
     */
    long countKnowledge(Long tenantId);

    /**
     * 统计已发布知识数量
     * @param tenantId 租户 ID
     * @return 已发布数量
     */
    long countPublished(Long tenantId);
}
