package com.myproject.service_ai_assistant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.service_ai_assistant.entity.KnowledgeItem;
import com.myproject.service_ai_assistant.mapper.KnowledgeItemMapper;
import com.myproject.service_ai_assistant.service.KnowledgeItemService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识条目服务实现类
 */
@Service
public class KnowledgeItemServiceImpl extends ServiceImpl<KnowledgeItemMapper, KnowledgeItem> implements KnowledgeItemService {

    @Override
    public List<KnowledgeItem> searchKnowledge(Long tenantId, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return List.of();
        }

        // 构建查询条件：租户隔离 + 已发布 + 关键词匹配
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeItem::getTenantId, tenantId)
                .eq(KnowledgeItem::getPublishStatus, 1)
                .and(w -> w.like(KnowledgeItem::getQuestion, keyword)
                        .or()
                        .like(KnowledgeItem::getKeywords, keyword)
                        .or()
                        .like(KnowledgeItem::getTitle, keyword)
                        .or()
                        .like(KnowledgeItem::getAnswer, keyword))
                .orderByDesc(KnowledgeItem::getIsTop)
                .orderByDesc(KnowledgeItem::getViewCount)
                .last("LIMIT 10");

        return this.list(wrapper);
    }

    @Override
    public List<KnowledgeItem> getHotQuestions(Long tenantId, Integer limit) {
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeItem::getTenantId, tenantId)
                .eq(KnowledgeItem::getPublishStatus, 1)
                .eq(KnowledgeItem::getIsHot, 1)
                .orderByDesc(KnowledgeItem::getViewCount)
                .last("LIMIT " + limit);

        return this.list(wrapper);
    }
}
