package com.myproject.service_ai_assistant.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myproject.service_ai_assistant.common.ExcelUtil;
import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.dto.CategoryDTO;
import com.myproject.service_ai_assistant.dto.KnowledgeDTO;
import com.myproject.service_ai_assistant.entity.KnowledgeCategory;
import com.myproject.service_ai_assistant.entity.KnowledgeItem;
import com.myproject.service_ai_assistant.service.KnowledgeCategoryService;
import com.myproject.service_ai_assistant.service.KnowledgeItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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
            @Parameter(description = "发布状态 (0:草稿，1:已发布)") @RequestParam(required = false) Integer publishStatus,
            @Parameter(description = "是否置顶 (0:未置顶，1:已置顶)") @RequestParam(required = false) Integer isTop,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("【知识列表】查询参数：tenantId={}, categoryId={}, keyword={}, publishStatus={}, isTop={}, current={}, size={}", 
                tenantId, categoryId, keyword, publishStatus, isTop, current, size);
        
        Page<KnowledgeItem> page = new Page<>(current, size);
        Page<KnowledgeItem> result;
        
        // 如果有搜索关键词，使用搜索接口（带筛选）
        if (StrUtil.isNotBlank(keyword)) {
            // 使用原有的搜索方法，筛选逻辑在 SQL 中处理
            result = knowledgeItemService.searchKnowledgeWithFiltersPage(tenantId, keyword, publishStatus, isTop, page);
        } else {
            // 使用带筛选的分页查询
            result = knowledgeItemService.queryKnowledgeListWithFilters(tenantId, categoryId, publishStatus, isTop, page);
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
    public Result<Boolean> save(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Valid @RequestBody KnowledgeDTO dto
    ) {
        log.info("【新增知识】title={}, tenantId={}", dto.getTitle(), tenantId);
        
        // 强制使用路径参数中的租户 ID，防止前端篡改
        dto.setTenantId(tenantId);
        
        KnowledgeItem item = new KnowledgeItem();
        beanCopy(dto, item);
        knowledgeItemService.save(item);
        log.info("【新增知识】保存成功：id={}", item.getId());
        return Result.success(true);
    }

    @PutMapping
    @Operation(summary = "更新知识")
    public Result<Boolean> update(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Valid @RequestBody KnowledgeDTO dto
    ) {
        log.info("【更新知识】id={}, title={}, tenantId={}", dto.getId(), dto.getTitle(), tenantId);
        
        // 校验知识条目是否属于该租户
        KnowledgeItem existing = knowledgeItemService.getById(dto.getId());
        if (existing == null || !existing.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("无权操作该知识条目");
        }
        
        KnowledgeItem item = new KnowledgeItem();
        beanCopy(dto, item);
        knowledgeItemService.updateById(item);
        log.info("【更新知识】更新成功：id={}", dto.getId());
        return Result.success(true);
    }
    
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除知识")
    public Result<Boolean> batchDelete(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "知识 ID 列表", required = true) @RequestBody List<Long> ids
    ) {
        log.info("【批量删除知识】ids={}, tenantId={}", ids, tenantId);
        
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("请选择要删除的知识");
        }
        
        // 校验所有知识条目是否属于该租户
        for (Long id : ids) {
            KnowledgeItem existing = knowledgeItemService.getById(id);
            if (existing == null || !existing.getTenantId().equals(tenantId)) {
                throw new IllegalArgumentException("无权删除ID为" + id + "的知识条目");
            }
        }
        
        // 批量删除
        knowledgeItemService.removeByIds(ids);
        log.info("【批量删除知识】删除成功：count={}", ids.size());
        return Result.success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识")
    public Result<Boolean> delete(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "知识 ID") @PathVariable Long id
    ) {
        log.info("【删除知识】id={}, tenantId={}", id, tenantId);
        
        // 校验知识条目是否属于该租户
        KnowledgeItem existing = knowledgeItemService.getById(id);
        if (existing == null || !existing.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("无权删除该知识条目");
        }
        
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
        item.setIsTop(dto.getIsTop());
    }

    @GetMapping("/categories")
    @Operation(summary = "查询分类列表")
    public Result<List<KnowledgeCategory>> getCategories(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId
    ) {
        log.info("【查询分类列表】tenantId={}", tenantId);
        List<KnowledgeCategory> categories = knowledgeCategoryService.queryCategoryList(tenantId);
        return Result.success(categories);
    }
    
    @PostMapping("/category")
    @Operation(summary = "新增/编辑分类")
    public Result<Boolean> saveCategory(@RequestBody CategoryDTO dto) {
        log.info("【保存分类】name={}, tenantId={}", dto.getCategoryName(), dto.getTenantId());
        
        if (dto.getTenantId() == null) {
            throw new IllegalArgumentException("租户 ID 不能为空");
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

    @GetMapping("/template")
    @Operation(summary = "下载知识库导入模板")
    public ResponseEntity<byte[]> downloadTemplate() {
        log.info("【下载模板】开始生成知识库导入模板");
        
        try {
            // 生成模板并返回字节数组
            byte[] bytes = ExcelUtil.generateKnowledgeTemplate();
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String fileName = URLEncoder.encode("知识库导入模板.xlsx", StandardCharsets.UTF_8.toString());
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(bytes.length);
            
            log.info("【下载模板】模板生成成功，大小: {} bytes", bytes.length);
            return new ResponseEntity<>(bytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            log.error("【下载模板】生成模板失败", e);
            throw new RuntimeException("生成模板失败: " + e.getMessage());
        }
    }

    @PostMapping("/import")
    @Operation(summary = "批量导入知识条目")
    public Result<Map<String, Object>> importKnowledge(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "Excel 文件", required = true) @RequestParam("file") MultipartFile file
    ) {
        log.info("【批量导入】开始导入知识条目，tenantId={}, fileName={}, fileSize={}KB", 
                tenantId, file.getOriginalFilename(), file.getSize() / 1024);
        
        // 验证文件
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        
        // 验证文件大小（限制为5MB，防止大文件占用过多内存）
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(String.format("文件大小超过限制（最大%dMB），当前文件%.2fMB", 
                    maxSize / 1024 / 1024, file.getSize() / 1024.0 / 1024.0));
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            throw new IllegalArgumentException("只支持 Excel 文件格式(.xlsx, .xls)");
        }
        
        try {
            // 调用服务层进行导入
            Map<String, Object> result = knowledgeItemService.importKnowledgeFromExcel(file.getInputStream(), tenantId);
            
            // 验证导入数量限制（单次最多200条，防止一次性处理过多数据）
            int totalCount = (int) result.getOrDefault("totalCount", 0);
            if (totalCount > 200) {
                throw new IllegalArgumentException("单次导入数量不能超过200条，当前文件包含" + totalCount + "条数据。请分批导入。");
            }
            
            log.info("【批量导入】导入完成，总数: {}, 成功: {}, 失败: {}", 
                    totalCount, result.get("successCount"), result.get("failCount"));
            return Result.success(result);
        } catch (IOException e) {
            log.error("【批量导入】读取文件失败", e);
            throw new RuntimeException("读取文件失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("【批量导入】导入失败", e);
            throw new RuntimeException("导入失败: " + e.getMessage());
        }
    }
}
