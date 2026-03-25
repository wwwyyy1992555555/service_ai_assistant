   package com.myproject.service_ai_assistant.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.entity.KnowledgeCategory;
import com.myproject.service_ai_assistant.entity.KnowledgeItem;
import com.myproject.service_ai_assistant.service.KnowledgeCategoryService;
import com.myproject.service_ai_assistant.service.KnowledgeItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 知识库管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@Tag(name = "知识库管理接口")
public class KnowledgeController {

    @Autowired
    private KnowledgeItemService knowledgeItemService;
    
    @Autowired
    private KnowledgeCategoryService knowledgeCategoryService;

    @GetMapping("/list")
    @Operation(summary = "分页查询知识列表")
    public Result<Page<KnowledgeItem>> list(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "分类 ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("【知识列表】查询参数：tenantId={}, categoryId={}, keyword={}, current={}, size={}", 
                tenantId, categoryId, keyword, current, size);
        
        Page<KnowledgeItem> page = new Page<>(current, size);
        Page<KnowledgeItem> result;
        
        // 如果有搜索关键词，使用搜索接口
        if (StrUtil.isNotBlank(keyword)) {
            List<KnowledgeItem> searchResults = knowledgeItemService.searchKnowledge(tenantId, keyword);
            // 手动实现分页
            long total = searchResults.size();
            int fromIndex = (current - 1) * size;
            int toIndex = Math.min(fromIndex + size, (int)total);
            List<KnowledgeItem> pagedResults = fromIndex < total ? searchResults.subList(fromIndex, toIndex) : List.of();
            result = new Page<>(current, size, total);
            result.setRecords(pagedResults);
        } else {
            result = knowledgeItemService.queryKnowledgeList(tenantId, categoryId, page);
        }
        
        log.info("【知识列表】查询成功：total={}", result.getTotal());
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识详情")
    public Result<KnowledgeItem> getById(@Parameter(description = "知识 ID") @PathVariable Long id) {
        log.info("【知识详情】查询 ID: {}", id);
        KnowledgeItem item = knowledgeItemService.getById(id);
        if (item != null) {
            log.debug("【知识详情】查询成功：title={}", item.getTitle());
        } else {
            log.warn("【知识详情】未找到：id={}", id);
        }
        return Result.success(item);
    }

    @PostMapping
    @Operation(summary = "新增知识")
    public Result<Boolean> save(@Valid @RequestBody KnowledgeDTO dto) {
        log.info("【新增知识】title={}, question={}", dto.getTitle(), dto.getQuestion());
        
        // 强制设置租户 ID 为 1（后续应从用户会话中获取）
        if (dto.getTenantId() == null) {
            dto.setTenantId(1L);
        }
        
        KnowledgeItem item = new KnowledgeItem();
        beanCopy(dto, item);
        knowledgeItemService.save(item);
        log.info("【新增知识】保存成功：id={}", item.getId());
        return Result.success(true);
    }

    @PutMapping
    @Operation(summary = "更新知识")
    public Result<Boolean> update(@Valid @RequestBody KnowledgeDTO dto) {
        log.info("【更新知识】id={}, title={}", dto.getId(), dto.getTitle());
        
        // 强制设置租户 ID 为 1（后续应从用户会话中获取）
        if (dto.getTenantId() == null) {
            dto.setTenantId(1L);
        }
        
        KnowledgeItem item = new KnowledgeItem();
        beanCopy(dto, item);
        knowledgeItemService.updateById(item);
        log.info("【更新知识】更新成功：id={}", dto.getId());
        return Result.success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识")
    public Result<Boolean> delete(@Parameter(description = "知识 ID") @PathVariable Long id) {
        log.info("【删除知识】id={}", id);
        knowledgeItemService.removeById(id);
        log.info("【删除知识】删除成功：id={}", id);
        return Result.success(true);
    }

    private void beanCopy(KnowledgeDTO dto, KnowledgeItem item) {
        item.setId(dto.getId());
        item.setTenantId(dto.getTenantId());
        item.setCategoryId(dto.getCategoryId());
        item.setTitle(dto.getTitle());
        item.setKeywords(dto.getKeywords());
        item.setQuestion(dto.getQuestion());
        item.setAnswer(dto.getAnswer());
        item.setContentType(dto.getContentType());
        item.setAttachments(dto.getAttachments());
        item.setPublishStatus(dto.getPublishStatus());
        item.setAuthor(dto.getAuthor());
    }

    @Data
    public static class KnowledgeDTO {
        
        private Long id;
        
        private Long tenantId;
        
        private Long categoryId;
        
        private String title;
        
        private String keywords;
        
        private String question;
        
        private String answer;
        
        private String contentType;
        
        private String attachments;
        
        private Integer publishStatus;
        
        private String author;
    }
    
    @GetMapping("/categories")
    @Operation(summary = "查询分类列表")
    public Result<List<KnowledgeCategory>> getCategories() {
        log.info("【查询分类列表】tenantId=1");
        List<KnowledgeCategory> categories = knowledgeCategoryService.queryCategoryList(1L);
        return Result.success(categories);
    }
    
    @PostMapping("/category")
    @Operation(summary = "新增/编辑分类")
    public Result<Boolean> saveCategory(@RequestBody CategoryDTO dto) {
        log.info("【保存分类】name={}", dto.getCategoryName());
        
        if (dto.getTenantId() == null) {
            dto.setTenantId(1L);
        }
        if (dto.getSortOrder() == null) {
            dto.setSortOrder(0);
        }
        if (dto.getStatus() == null) {
            dto.setStatus(1);
        }
        
        KnowledgeCategory category = new KnowledgeCategory();
        category.setId(dto.getId());
        category.setTenantId(dto.getTenantId());
        category.setCategoryName(dto.getCategoryName());
        category.setSortOrder(dto.getSortOrder());
        category.setStatus(dto.getStatus());
        
        knowledgeCategoryService.saveOrUpdate(category);
        log.info("【保存分类】成功：id={}", category.getId());
        return Result.success(true);
    }
    
    @DeleteMapping("/category/{id}")
    @Operation(summary = "删除分类")
    public Result<Boolean> deleteCategory(@Parameter(description = "分类 ID") @PathVariable Long id) {
        log.info("【删除分类】id={}", id);
        knowledgeCategoryService.removeById(id);
        return Result.success(true);
    }
    
    @Data
    public static class CategoryDTO {
        private Long id;
        private Long tenantId;
        private String categoryName;
        private Integer sortOrder;
        private Integer status;
    }
}
