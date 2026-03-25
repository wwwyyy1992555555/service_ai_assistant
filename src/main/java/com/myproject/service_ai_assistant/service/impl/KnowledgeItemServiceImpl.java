package com.myproject.service_ai_assistant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.service_ai_assistant.entity.KnowledgeItem;
import com.myproject.service_ai_assistant.mapper.KnowledgeItemMapper;
import com.myproject.service_ai_assistant.service.KnowledgeItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识条目服务实现类
 */
@Slf4j
@Service
public class KnowledgeItemServiceImpl extends ServiceImpl<KnowledgeItemMapper, KnowledgeItem> implements KnowledgeItemService {

    @Override
    public List<KnowledgeItem> searchKnowledge(Long tenantId, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return List.of();
        }

        // 构建查询条件：租户隔离 + 关键词匹配（包含已发布和未发布）
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeItem::getTenantId, tenantId)
                .and(w -> w.like(KnowledgeItem::getQuestion, keyword)
                        .or()
                        .like(KnowledgeItem::getKeywords, keyword)
                        .or()
                        .like(KnowledgeItem::getTitle, keyword)
                        .or()
                        .like(KnowledgeItem::getAnswer, keyword))
                .orderByDesc(KnowledgeItem::getIsTop)
                .orderByDesc(KnowledgeItem::getViewCount);

        return this.list(wrapper);
    }

    @Override
    public List<KnowledgeItem> getHotQuestions(Long tenantId, Integer limit) {
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeItem::getTenantId, tenantId)
                .eq(KnowledgeItem::getPublishStatus, 1)
                // 不再限制 is_hot=1，直接按浏览量排序
                .orderByDesc(KnowledgeItem::getViewCount)
                .last("LIMIT " + limit);

        return this.list(wrapper);
    }

    @Override
    public Page<KnowledgeItem> queryKnowledgeList(Long tenantId, Long categoryId, Page<KnowledgeItem> page) {
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeItem::getTenantId, tenantId);
        
        if (categoryId != null) {
            wrapper.eq(KnowledgeItem::getCategoryId, categoryId);
        }
        
        wrapper.orderByDesc(KnowledgeItem::getCreatedTime);
        
        Page<KnowledgeItem> result = this.page(page, wrapper);
        log.debug("【知识列表查询】tenantId={}, categoryId={}, total={}", tenantId, categoryId, result.getTotal());
        
        return result;
    }

    @Override
    public long countKnowledge(Long tenantId) {
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeItem::getTenantId, tenantId);
        return this.count(wrapper);
    }

    @Override
    public long countPublished(Long tenantId) {
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeItem::getTenantId, tenantId)
                .eq(KnowledgeItem::getPublishStatus, 1);
        return this.count(wrapper);
    }
}
