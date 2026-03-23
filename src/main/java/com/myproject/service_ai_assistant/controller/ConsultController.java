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
@RequestMapping("/consult")
@Tag(name = "智能问答接口")
public class ConsultController {

    @Autowired
    private KnowledgeItemService knowledgeItemService;

    @Autowired
    private ConsultationRecordService consultationRecordService;

    @PostMapping("/ask")
    @Operation(summary = "智能问答 - 用户提问", description = "用户提出问题，系统从知识库中匹配答案并返回")
    public Result<ConsultResponse> ask(@Valid @RequestBody ConsultRequest request) {
        log.info("收到咨询请求：sessionId={}, question={}", request.getSessionId(), request.getQuestion());

        // 1. 搜索知识库匹配答案
        List<KnowledgeItem> matchedKnowledge = knowledgeItemService.searchKnowledge(
                request.getTenantId(), 
                request.getQuestion()
        );

        String answer;
        Long matchedKnowledgeId = null;
        Double matchScore = 0.0;

        if (!matchedKnowledge.isEmpty()) {
            // 找到匹配的知识
            KnowledgeItem item = matchedKnowledge.get(0);
            answer = item.getAnswer();
            matchedKnowledgeId = item.getId();
            matchScore = 0.9; // TODO: 实际项目中应计算精确匹配度
            
            // 增加浏览次数
            item.setViewCount(item.getViewCount() + 1);
            knowledgeItemService.updateById(item);
        } else {
            // 未找到匹配，使用通用回答
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

        return Result.success(response);
    }

    @GetMapping("/hot-questions")
    @Operation(summary = "获取热门问题", description = "返回指定数量的热门问题列表")
    public Result<List<KnowledgeItem>> getHotQuestions(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<KnowledgeItem> hotQuestions = knowledgeItemService.getHotQuestions(tenantId, limit);
        return Result.success(hotQuestions);
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
