package com.myproject.service_ai_assistant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.service_ai_assistant.common.ExcelUtil;
import com.myproject.service_ai_assistant.common.ResultCode;
import com.myproject.service_ai_assistant.common.SimilarityUtil;
import com.myproject.service_ai_assistant.context.UserContext;
import com.myproject.service_ai_assistant.entity.KnowledgeCategory;
import com.myproject.service_ai_assistant.entity.KnowledgeItem;
import com.myproject.service_ai_assistant.exception.BusinessException;
import com.myproject.service_ai_assistant.mapper.KnowledgeItemMapper;
import com.myproject.service_ai_assistant.service.KnowledgeCategoryService;
import com.myproject.service_ai_assistant.service.KnowledgeItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识条目服务实现类
 */
@Slf4j
@Service
public class KnowledgeItemServiceImpl extends ServiceImpl<KnowledgeItemMapper, KnowledgeItem> implements KnowledgeItemService {

    private final KnowledgeCategoryService knowledgeCategoryService;
    
    public KnowledgeItemServiceImpl(KnowledgeCategoryService knowledgeCategoryService) {
        this.knowledgeCategoryService = knowledgeCategoryService;
    }

    @Override
    public List<KnowledgeItem> searchKnowledge(Long tenantId, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return List.of();
        }

        log.info("【知识搜索】keyword={}", keyword);

        // 1. 先进行数据库模糊查询，获取候选集（限制数量避免内存溢出）
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeItem::getTenantId, tenantId)
                .eq(KnowledgeItem::getPublishStatus, 1)  // 只查询已发布的知识
                .and(w -> w.like(KnowledgeItem::getQuestion, keyword)
                        .or()
                        .like(KnowledgeItem::getKeywords, keyword))
                .last("LIMIT 100");  // 限制最大返回数量

        List<KnowledgeItem> candidates = this.list(wrapper);
        log.debug("【知识搜索】初步候选集数量：{}", candidates.size());

        // 2. 如果没有精确匹配，扩大搜索范围（但仍需包含关键词）
        if (candidates.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(KnowledgeItem::getTenantId, tenantId)
                    .eq(KnowledgeItem::getPublishStatus, 1)
                    .and(w -> w.like(KnowledgeItem::getQuestion, keyword)
                            .or()
                            .like(KnowledgeItem::getTitle, keyword))
                    .last("LIMIT 100");  // 限制最大返回数量
            candidates = this.list(wrapper);
            log.debug("【知识搜索】扩大搜索后数量：{}", candidates.size());
        }

        // 3. 对候选集进行智能匹配度计算
        List<KnowledgeItemWithScore> scoredList = new ArrayList<>();
        for (KnowledgeItem item : candidates) {
            double score = calculateMatchScore(keyword, item);
            
            // 严格模式：只保留匹配度大于 0.5 的结果
            if (score >= 0.5) {
                scoredList.add(new KnowledgeItemWithScore(item, score));
                log.debug("【知识搜索】匹配成功：{} - 分数：{}", item.getQuestion(), score);
            } else {
                log.debug("【知识搜索】匹配失败：{} - 分数：{}", item.getQuestion(), score);
            }
        }

        // 4. 按匹配度排序
        scoredList.sort((a, b) -> Double.compare(b.score, a.score));
        log.debug("【知识搜索】最终返回数量：{}", scoredList.size());

        // 5. 批量填充分类名称
        fillCategoryNames(scoredList.stream()
                .map(KnowledgeItemWithScore::getItem)
                .collect(Collectors.toList()));
        
        // 6. 返回排序后的结果
        return scoredList.stream()
                .map(KnowledgeItemWithScore::getItem)
                .collect(Collectors.toList());
    }

    @Override
    public Page<KnowledgeItem> searchKnowledgeWithFiltersPage(Long tenantId, String keyword, Integer publishStatus, Integer isTop, Page<KnowledgeItem> page) {
        if (StrUtil.isBlank(keyword)) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        log.info("【知识搜索（带筛选分页）】keyword={}, publishStatus={}, isTop={}", keyword, publishStatus, isTop);

        // 构建查询条件
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeItem::getTenantId, tenantId);
        
        // 添加发布状态筛选
        if (publishStatus != null) {
            wrapper.eq(KnowledgeItem::getPublishStatus, publishStatus);
        }
        
        // 添加置顶状态筛选
        if (isTop != null) {
            wrapper.eq(KnowledgeItem::getIsTop, isTop);
        }
        
        // 关键词模糊查询
        wrapper.and(w -> w.like(KnowledgeItem::getQuestion, keyword)
                .or()
                .like(KnowledgeItem::getKeywords, keyword)
                .or()
                .like(KnowledgeItem::getTitle, keyword));

        // 执行分页查询
        Page<KnowledgeItem> result = this.page(page, wrapper);
        log.debug("【知识搜索（带筛选分页）】total={}", result.getTotal());

        return result;
    }

    /**
     * 批量填充分类名称
     */
    private void fillCategoryNames(List<KnowledgeItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        
        // 收集所有分类 ID
        Set<Long> categoryIds = items.stream()
                .map(KnowledgeItem::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        if (categoryIds.isEmpty()) {
            return;
        }
        
        // 批量查询分类
        List<KnowledgeCategory> categories = knowledgeCategoryService.listByIds(categoryIds);
        Map<Long, String> categoryMap = categories.stream()
                .collect(Collectors.toMap(KnowledgeCategory::getId, KnowledgeCategory::getCategoryName));
        
        // 填充分类名称
        for (KnowledgeItem item : items) {
            if (item.getCategoryId() != null && categoryMap.containsKey(item.getCategoryId())) {
                item.setCategoryName(categoryMap.get(item.getCategoryId()));
            }
        }
        
        log.debug("【填充分类名称】填充数量：{}", items.size());
    }

    /**
     * 计算综合匹配分数
     */
    private double calculateMatchScore(String question, KnowledgeItem item) {
        // 1. 问题与问题匹配（权重 0.5）
        double questionScore = SimilarityUtil.calculateSimilarity(question, item.getQuestion());

        // 2. 问题与关键词匹配（权重 0.3）
        double keywordScore = 0.0;
        if (StrUtil.isNotBlank(item.getKeywords())) {
            String[] keywords = item.getKeywords().split("[,，;；\\s]+");
            int matchCount = 0;
            for (String kw : keywords) {
                if (question.contains(kw.trim())) {
                    matchCount++;
                }
            }
            keywordScore = (double) matchCount / keywords.length;
        }

        // 3. 问题与标题匹配（权重 0.2）
        double titleScore = 0.0;
        if (StrUtil.isNotBlank(item.getTitle())) {
            titleScore = SimilarityUtil.calculateSimilarity(question, item.getTitle());
        }

        // 4. 数字匹配加成（如果包含数字且匹配）
        double numberBonus = 0.0;
        if (SimilarityUtil.containsKeyNumbers(question, item.getQuestion())) {
            numberBonus = 0.2;
        }

        // 综合计算
        double totalScore = questionScore * 0.5 + keywordScore * 0.3 + titleScore * 0.2 + numberBonus;

        // 5. 完全匹配奖励：如果问题完全相同，直接返回 1.0
        if (question.trim().equalsIgnoreCase(item.getQuestion().trim())) {
            return 1.0;
        }

        // 6. 浏览量微调（热门问题略微加分）
        double viewBonus = Math.min(item.getViewCount() * 0.001, 0.1);  // 最多加 0.1

        return Math.min(totalScore + viewBonus, 1.0);  // 不超过 1.0
    }

    /**
     * 带分数的知识条目包装类
     */
    private static class KnowledgeItemWithScore {
        private final KnowledgeItem item;
        private final double score;

        public KnowledgeItemWithScore(KnowledgeItem item, double score) {
            this.item = item;
            this.score = score;
        }

        public KnowledgeItem getItem() {
            return item;
        }

        public double getScore() {
            return score;
        }
    }

    @Override
    public List<KnowledgeItem> getHotQuestions(Long tenantId, Integer limit) {
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeItem::getTenantId, tenantId)
                .eq(KnowledgeItem::getPublishStatus, 1)
                .orderByDesc(KnowledgeItem::getIsTop)  // 置顶的排前面
                .orderByDesc(KnowledgeItem::getViewCount)  // 再按浏览量排序
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
    public Page<KnowledgeItem> queryKnowledgeListWithFilters(Long tenantId, Long categoryId, Integer publishStatus, Integer isTop, Page<KnowledgeItem> page) {
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeItem::getTenantId, tenantId);
        
        // 添加分类筛选
        if (categoryId != null) {
            if (categoryId == -1) {
                // 特殊值：筛选没有分类的数据
                wrapper.isNull(KnowledgeItem::getCategoryId);
            } else {
                // 筛选指定分类
                wrapper.eq(KnowledgeItem::getCategoryId, categoryId);
            }
        }
        
        // 添加发布状态筛选
        if (publishStatus != null) {
            wrapper.eq(KnowledgeItem::getPublishStatus, publishStatus);
        }
        
        // 添加置顶状态筛选
        if (isTop != null) {
            wrapper.eq(KnowledgeItem::getIsTop, isTop);
        }
        
        wrapper.orderByDesc(KnowledgeItem::getCreatedTime);
        
        Page<KnowledgeItem> result = this.page(page, wrapper);
        log.debug("【知识列表查询（带筛选）】tenantId={}, categoryId={}, publishStatus={}, isTop={}, total={}", 
                tenantId, categoryId, publishStatus, isTop, result.getTotal());
        
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(KnowledgeItem entity) {
        // ✅ 水平越权校验：确保修改的是当前租户的知识（运营商 tenant_id=0 可跨租户操作）
        UserContext.validateHorizontalPermission(entity.getTenantId());
        return super.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        // ✅ 水平越权校验：确保删除的是当前租户的知识（运营商 tenant_id=0 可跨租户操作）
        KnowledgeItem item = this.getById(id);
        if (item == null) {
            return false;
        }
        
        UserContext.validateHorizontalPermission(item.getTenantId());
        
        return super.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importKnowledgeFromExcel(InputStream inputStream, Long tenantId) {
        log.info("【批量导入】开始从 Excel 导入知识条目，tenantId={}", tenantId);
        
        // 使用工具类解析 Excel 文件
        List<Map<String, Object>> knowledgeDataList = ExcelUtil.importKnowledgeFromExcel(inputStream, tenantId);
        
        int successCount = 0;
        int failCount = 0;
        List<String> errorMessages = new ArrayList<>();
        List<KnowledgeItem> batchToSave = new ArrayList<>();
        
        // 第一阶段：解析和验证所有数据
        for (int i = 0; i < knowledgeDataList.size(); i++) {
            try {
                Map<String, Object> knowledgeData = knowledgeDataList.get(i);
                
                // 创建知识条目实体
                KnowledgeItem knowledgeItem = new KnowledgeItem();
                knowledgeItem.setTenantId(tenantId);
                knowledgeItem.setTitle((String) knowledgeData.get("title"));
                knowledgeItem.setQuestion((String) knowledgeData.get("question"));
                knowledgeItem.setAnswer((String) knowledgeData.get("answer"));
                knowledgeItem.setKeywords((String) knowledgeData.get("keywords"));
                knowledgeItem.setPublishStatus((Integer) knowledgeData.get("publishStatus"));
                knowledgeItem.setIsTop((Integer) knowledgeData.get("isTop"));
                
                // 处理分类名称到分类ID的转换
                String categoryName = (String) knowledgeData.get("categoryName");
                if (StrUtil.isNotBlank(categoryName)) {
                    Long categoryId = findCategoryIdByName(tenantId, categoryName);
                    if (categoryId != null) {
                        knowledgeItem.setCategoryId(categoryId);
                    } else {
                        log.warn("【批量导入】第 {} 条：未找到分类 '{}'，将不设置分类", i + 1, categoryName);
                    }
                }
                
                // 添加到批量保存列表
                batchToSave.add(knowledgeItem);
                successCount++;
                log.debug("【批量导入】第 {} 条验证通过：{}", i + 1, knowledgeItem.getTitle());
                
            } catch (Exception e) {
                failCount++;
                String errorMsg = String.format("第 %d 条验证失败: %s", i + 1, e.getMessage());
                errorMessages.add(errorMsg);
                log.error("【批量导入】{}", errorMsg, e);
            }
        }
        
        // 第二阶段：批量保存（要么全部成功，要么全部回滚）
        if (!batchToSave.isEmpty()) {
            try {
                this.saveBatch(batchToSave);
                log.info("【批量导入】批量保存成功，共 {} 条", batchToSave.size());
            } catch (Exception e) {
                log.error("【批量导入】批量保存失败，将回滚所有数据", e);
                // 抛出异常触发事务回滚
                throw new RuntimeException("批量保存失败：" + e.getMessage(), e);
            }
        }
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("totalCount", knowledgeDataList.size());
        result.put("errorMessages", errorMessages);
        
        log.info("【批量导入】导入完成，总数: {}, 成功: {}, 失败: {}", 
                knowledgeDataList.size(), successCount, failCount);
        
        return result;
    }
    
    /**
     * 根据分类名称查找分类ID
     */
    private Long findCategoryIdByName(Long tenantId, String categoryName) {
        if (StrUtil.isBlank(categoryName)) {
            return null;
        }
        
        LambdaQueryWrapper<KnowledgeCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeCategory::getTenantId, tenantId)
               .eq(KnowledgeCategory::getCategoryName, categoryName.trim())
               .last("LIMIT 1");
        
        KnowledgeCategory category = knowledgeCategoryService.getOne(wrapper);
        return category != null ? category.getId() : null;
    }
}
