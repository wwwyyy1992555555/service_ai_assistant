package com.myproject.service_ai_assistant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.entity.KnowledgeItem;
import com.myproject.service_ai_assistant.service.KnowledgeItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 知识库管理控制器
 */
@RestController
@RequestMapping("/knowledge")
@Tag(name = "知识库管理接口")
public class KnowledgeController {

    @Autowired
    private KnowledgeItemService knowledgeItemService;

    @GetMapping("/list")
    @Operation(summary = "分页查询知识列表")
    public Result<Page<KnowledgeItem>> list(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "分类 ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size
    ) {
        Page<KnowledgeItem> page = new Page<>(current, size);
        
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeItem>();
        wrapper.eq(KnowledgeItem::getTenantId, tenantId);
        if (categoryId != null) {
            wrapper.eq(KnowledgeItem::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(KnowledgeItem::getCreatedTime);
        
        Page<KnowledgeItem> result = knowledgeItemService.page(page, wrapper);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识详情")
    public Result<KnowledgeItem> getById(@Parameter(description = "知识 ID") @PathVariable Long id) {
        KnowledgeItem item = knowledgeItemService.getById(id);
        return Result.success(item);
    }

    @PostMapping
    @Operation(summary = "新增知识")
    public Result<Boolean> save(@Valid @RequestBody KnowledgeDTO dto) {
        KnowledgeItem item = new KnowledgeItem();
        beanCopy(dto, item);
        knowledgeItemService.save(item);
        return Result.success(true);
    }

    @PutMapping
    @Operation(summary = "更新知识")
    public Result<Boolean> update(@Valid @RequestBody KnowledgeDTO dto) {
        KnowledgeItem item = new KnowledgeItem();
        beanCopy(dto, item);
        knowledgeItemService.updateById(item);
        return Result.success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识")
    public Result<Boolean> delete(@Parameter(description = "知识 ID") @PathVariable Long id) {
        knowledgeItemService.removeById(id);
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
}
