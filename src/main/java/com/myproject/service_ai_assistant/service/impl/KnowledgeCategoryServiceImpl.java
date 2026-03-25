package com.myproject.service_ai_assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.service_ai_assistant.entity.KnowledgeCategory;
import com.myproject.service_ai_assistant.mapper.KnowledgeCategoryMapper;
import com.myproject.service_ai_assistant.service.KnowledgeCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识库分类服务实现类
 */
@Slf4j
@Service
public class KnowledgeCategoryServiceImpl extends ServiceImpl<KnowledgeCategoryMapper, KnowledgeCategory> implements KnowledgeCategoryService {

    @Override
    public List<KnowledgeCategory> queryCategoryList(Long tenantId) {
        LambdaQueryWrapper<KnowledgeCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeCategory::getTenantId, tenantId)
                .eq(KnowledgeCategory::getStatus, 1)
                .eq(KnowledgeCategory::getDeleted, 0)
                .orderByAsc(KnowledgeCategory::getSortOrder);
        
        List<KnowledgeCategory> categories = this.list(wrapper);
        log.debug("【分类列表查询】tenantId={}, size={}", tenantId, categories.size());
        
        return categories;
    }
}
