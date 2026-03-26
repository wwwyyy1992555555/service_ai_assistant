package com.myproject.service_ai_assistant.controller;

import com.myproject.service_ai_assistant.entity.ConsultationFeedback;
import com.myproject.service_ai_assistant.service.ConsultationFeedbackService;
import com.myproject.service_ai_assistant.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 咨询反馈控制器
 */
@RestController
@RequestMapping("/consult/feedback")
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
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(feedbackService.getStatistics());
    }

    @GetMapping("/pending")
    @Operation(summary = "获取待处理反馈", description = "获取待处理的反馈列表，按创建时间倒序排列")
    public Result<List<ConsultationFeedback>> getPendingFeedbacks(
            @RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(feedbackService.getPendingFeedbacks(limit));
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
}
