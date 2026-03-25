package com.myproject.service_ai_assistant.controller;

import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.entity.ConsultationRecord;
import com.myproject.service_ai_assistant.entity.KnowledgeItem;
import com.myproject.service_ai_assistant.service.ConsultationRecordService;
import com.myproject.service_ai_assistant.service.KnowledgeItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 智能问答控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/consult")
@Tag(name = "智能问答接口")
public class ConsultController {

    @Autowired
    private KnowledgeItemService knowledgeItemService;

    @Autowired
    private ConsultationRecordService consultationRecordService;

    @PostMapping("/ask")
    @Operation(summary = "智能问答 - 用户提问", description = "用户提出问题，系统从知识库中匹配答案并返回")
    public Result<ConsultResponse> ask(@Valid @RequestBody ConsultRequest request) {
        log.info("【智能问答】收到咨询请求：sessionId={}, tenantId={}, question={}", 
                request.getSessionId(), request.getTenantId(), request.getQuestion());

        // 1. 搜索知识库匹配答案 - 优化匹配逻辑
        List<KnowledgeItem> matchedKnowledge = knowledgeItemService.searchKnowledge(
                request.getTenantId(), 
                request.getQuestion()
        );
        
        log.debug("【智能问答】匹配到 {} 条知识", matchedKnowledge.size());

        String answer;
        Long matchedKnowledgeId = null;
        Double matchScore = 0.0;

        if (!matchedKnowledge.isEmpty()) {
            // 找到匹配的知识
            KnowledgeItem item = matchedKnowledge.get(0);
            answer = item.getAnswer();
            matchedKnowledgeId = item.getId();
            matchScore = 0.9; // TODO: 实际项目中应计算精确匹配度
            
            log.info("【智能问答】匹配到知识：id={}, title={}, score={}", item.getId(), item.getTitle(), matchScore);
            
            // 增加浏览次数
            item.setViewCount(item.getViewCount() + 1);
            knowledgeItemService.updateById(item);
        } else {
            // 未找到匹配，使用通用回答
            log.warn("【智能问答】未找到匹配的知识，question={}", request.getQuestion());
            answer = "抱歉，我暂时没有找到相关信息。您可以留下联系方式，我们会尽快安排专人为您解答。";
        }

        // 2. 保存对话记录
        ConsultationRecord record = new ConsultationRecord();
        record.setTenantId(request.getTenantId());
        record.setSessionId(request.getSessionId());
        record.setUserId(request.getUserId());
        record.setUserName(request.getUserName());
        record.setUserPhone(request.getUserPhone());
        record.setQuestion(request.getQuestion());
        record.setAnswer(answer);
        record.setMatchedKnowledgeId(matchedKnowledgeId);
        record.setMatchScore(matchScore);
        record.setIsSolved(matchedKnowledgeId != null ? 1 : 0);
        record.setDeviceType(request.getDeviceType());
        record.setIpAddress(request.getIpAddress());
        
        consultationRecordService.save(record);
        log.info("【智能问答】保存对话记录：recordId={}", record.getId());

        // 3. 返回结果
        ConsultResponse response = new ConsultResponse();
        response.setAnswer(answer);
        response.setMatchedKnowledgeId(matchedKnowledgeId);
        response.setMatchScore(matchScore);
        response.setSuggestedQuestions(matchedKnowledge.stream()
                .skip(1)
                .limit(3)
                .map(KnowledgeItem::getQuestion)
                .toList());
        
        log.info("【智能问答】返回答案：answerLength={}, suggestedQuestions={}", 
                answer.length(), response.getSuggestedQuestions().size());

        return Result.success(response);
    }

    @GetMapping("/hot-questions")
    @Operation(summary = "获取热门问题", description = "返回指定数量的热门问题列表")
    public Result<List<KnowledgeItem>> getHotQuestions(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "10") Integer limit
    ) {
        log.info("【热门问题】开始查询，tenantId={}, limit={}", tenantId, limit);
        List<KnowledgeItem> hotQuestions = knowledgeItemService.getHotQuestions(tenantId, limit);
        log.info("【热门问题】查询结果，count={}", hotQuestions.size());
        return Result.success(hotQuestions);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索对话记录", description = "根据关键词搜索问题和答案")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<ConsultationRecord>> searchRecords(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("【搜索对话】tenantId={}, keyword={}, current={}, size={}", tenantId, keyword, current, size);
        
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<ConsultationRecord>(current, size);
        var result = consultationRecordService.searchRecords(tenantId, keyword, page);
        
        log.info("【搜索对话】匹配到 {} 条记录", result.getTotal());
        return Result.success(result);
    }
    
    @GetMapping("/list")
    @Operation(summary = "分页查询对话记录", description = "分页查询对话记录列表")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<ConsultationRecord>> listRecords(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("【对话列表】tenantId={}, current={}, size={}", tenantId, current, size);
        
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<ConsultationRecord>(current, size);
        var result = consultationRecordService.queryRecords(tenantId, page);
        
        log.info("【对话列表】查询成功：total={}", result.getTotal());
        return Result.success(result);
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "删除会话", description = "删除整个会话的所有对话记录")
    public Result<Void> deleteSession(@Parameter(description = "会话 ID", required = true) @PathVariable String sessionId) {
        log.info("【删除会话】sessionId={}", sessionId);
        
        // 查询该会话的所有记录
        var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConsultationRecord>()
                .eq(ConsultationRecord::getSessionId, sessionId);
        
        List<ConsultationRecord> records = consultationRecordService.list(queryWrapper);
        
        if (records.isEmpty()) {
            log.warn("【删除会话】会话不存在：sessionId={}", sessionId);
            return Result.error(404, "会话不存在");
        }
        
        // 批量删除（逻辑删除）
        boolean success = consultationRecordService.remove(queryWrapper);
        
        if (success) {
            log.info("【删除会话】删除成功：sessionId={}, 删除记录数={}", sessionId, records.size());
            return Result.success(null);
        } else {
            log.error("【删除会话】删除失败：sessionId={}", sessionId);
            return Result.error(500, "删除失败");
        }
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "查询会话详情", description = "根据会话 ID 查询完整的对话历史")
    public Result<List<ConsultationRecord>> getSessionDetail(@Parameter(description = "会话 ID", required = true) @PathVariable String sessionId) {
        log.info("【会话详情】sessionId={}", sessionId);
        
        var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConsultationRecord>()
                .eq(ConsultationRecord::getSessionId, sessionId)
                .orderByAsc(ConsultationRecord::getCreatedTime);
        
        var records = consultationRecordService.list(queryWrapper);
        
        log.info("【会话详情】查询到 {} 条记录", records.size());
        return Result.success(records);
    }

    /**
     * 问答请求参数
     */
    @Data
    public static class ConsultRequest {
        
        @NotBlank(message = "会话 ID 不能为空")
        private String sessionId;
        
        @NotBlank(message = "问题不能为空")
        private String question;
        
        private Long tenantId = 1L; // 默认租户
        
        private String userId;
        
        private String userName;
        
        private String userPhone;
        
        private String deviceType = "web";
        
        private String ipAddress;
    }

    /**
     * 问答响应数据
     */
    @Data
    public static class ConsultResponse {
        
        private String answer;
        
        private Long matchedKnowledgeId;
        
        private Double matchScore;
        
        private List<String> suggestedQuestions;
    }
}
