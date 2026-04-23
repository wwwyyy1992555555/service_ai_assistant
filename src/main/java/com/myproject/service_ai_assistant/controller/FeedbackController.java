package com.myproject.service_ai_assistant.controller;

import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.common.ResultCode;
import com.myproject.service_ai_assistant.common.LevelCode;
import com.myproject.service_ai_assistant.entity.ConsultationFeedback;
import com.myproject.service_ai_assistant.service.ConsultationFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 咨询反馈控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/consult/feedback")
@Tag(name = "咨询反馈管理")
@CrossOrigin(origins = "*")
public class FeedbackController {

    @Autowired
    private ConsultationFeedbackService feedbackService;

    @PostMapping("/submit")
    @Operation(summary = "提交反馈", description = "用户对咨询结果进行满意度评价并提交反馈意见")
    public Result<Void> submitFeedback(@RequestBody Map<String, Object> request) {
        
        Long consultationId = Long.valueOf(request.get("consultationId").toString());
        Integer satisfaction = (Integer) request.get("satisfaction");
        
        @SuppressWarnings("unchecked")
        List<String> reasons = (List<String>) request.get("reasons");
        
        String suggestion = (String) request.get("suggestion");
        
        feedbackService.submitFeedback(consultationId, satisfaction, reasons, suggestion);
        return Result.success();
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取反馈统计", description = "获取所有反馈的统计数据，包括总数、待处理数、平均满意度等")
    public Result<Map<String, Object>> getStatistics(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId) {
        return Result.success(feedbackService.getStatistics(tenantId));
    }
    
    @GetMapping("/pending")
    @Operation(summary = "获取待处理反馈", description = "获取待处理的反馈列表，按创建时间倒序排列")
    public Result<List<ConsultationFeedback>> getPendingFeedbacks(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(feedbackService.getPendingFeedbacks(tenantId, limit));
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有反馈列表", description = "获取所有反馈记录，用于后台管理")
    public Result<Map<String, Object>> getAllFeedbacks(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer satisfaction,
            @RequestParam(required = false) String keyword) {
        return Result.success(feedbackService.getAllFeedbacks(tenantId, page, size, status, satisfaction, keyword));
    }

    @PostMapping("/process/{id}")
    @Operation(summary = "处理反馈", description = "管理员处理用户反馈，填写处理备注并标记为已处理")
    public Result<Void> processFeedback(
            @PathVariable Long id,
            @RequestParam String remark,
            @RequestParam(required = false) String processor) {
        
        feedbackService.processFeedback(id, remark, processor);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除反馈", description = "批量删除指定的反馈记录")
    public Result<Void> batchDeleteFeedbacks(@RequestBody List<Long> feedbackIds) {
        // 从 UserContext 获取当前用户的 tenantId
        Long tenantId = com.myproject.service_ai_assistant.context.UserContext.getTenantId();
        
        if (feedbackIds == null || feedbackIds.isEmpty()) {
            return Result.error(ResultCode.FEEDBACK_IDS_REQUIRED.getMessage());
        }
        
        // 验证这些反馈是否都属于该租户
        var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.myproject.service_ai_assistant.entity.ConsultationFeedback>()
                .in(com.myproject.service_ai_assistant.entity.ConsultationFeedback::getId, feedbackIds)
                .ne(com.myproject.service_ai_assistant.entity.ConsultationFeedback::getTenantId, tenantId);
        long count = feedbackService.count(queryWrapper);
        if (count > 0) {
            return Result.error(ResultCode.CROSS_TENANT_OPERATION_FORBIDDEN.getMessage());
        }
        
        feedbackService.removeByIds(feedbackIds);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除反馈", description = "删除指定的反馈记录")
    public Result<Void> deleteFeedback(@PathVariable Long id) {
        // 从 UserContext 获取当前用户的 tenantId
        Long tenantId = com.myproject.service_ai_assistant.context.UserContext.getTenantId();
        log.info("【删除反馈】id={}, tenantId={}", id, tenantId);
        
        try {
            // 校验反馈是否属于该租户
            var existing = feedbackService.getById(id);
            if (existing == null || !existing.getTenantId().equals(tenantId)) {
                return Result.error(ResultCode.PERMISSION_DENIED.getMessage());
            }
            
            feedbackService.removeById(id);
            log.info("【删除反馈】删除成功：id={}", id);
            return Result.success();
        } catch (Exception e) {
            log.error("【删除反馈】失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新反馈", description = "更新反馈记录信息")
    public Result<Void> updateFeedback(
            @PathVariable Long id,
            @RequestBody ConsultationFeedback feedback) {
        
        feedback.setId(id);
        feedbackService.updateById(feedback);
        return Result.success();
    }
}
