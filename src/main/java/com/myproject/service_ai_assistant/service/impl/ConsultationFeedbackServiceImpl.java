package com.myproject.service_ai_assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myproject.service_ai_assistant.entity.ConsultationFeedback;
import com.myproject.service_ai_assistant.entity.ConsultationRecord;
import com.myproject.service_ai_assistant.exception.BusinessException;
import com.myproject.service_ai_assistant.mapper.ConsultationFeedbackMapper;
import com.myproject.service_ai_assistant.service.ConsultationFeedbackService;
import com.myproject.service_ai_assistant.service.ConsultationRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 咨询反馈服务实现
 */
@Service
public class ConsultationFeedbackServiceImpl extends ServiceImpl<ConsultationFeedbackMapper, ConsultationFeedback> 
        implements ConsultationFeedbackService {

    @Autowired
    private ConsultationRecordService consultationRecordService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFeedback(Long consultationId, Integer satisfaction, List<String> reasons, String suggestion) {
        // 验证咨询记录是否存在
        ConsultationRecord record = consultationRecordService.getById(consultationId);
        if (record == null) {
            throw new BusinessException("咨询记录不存在");
        }

        // 创建反馈记录
        ConsultationFeedback feedback = new ConsultationFeedback();
        feedback.setTenantId(record.getTenantId());
        feedback.setConsultationId(consultationId);
        feedback.setSessionId(record.getSessionId());
        feedback.setUserId(record.getUserId());
        feedback.setSatisfaction(satisfaction);
        feedback.setIsProcessed(0);
        
        // 将原因列表转为 JSON 字符串
        if (reasons != null && !reasons.isEmpty()) {
            try {
                feedback.setFeedbackReasons(objectMapper.writeValueAsString(reasons));
            } catch (JsonProcessingException e) {
                throw new BusinessException("反馈原因格式错误");
            }
        }
        
        feedback.setUserSuggestion(suggestion);
        
        // 保存反馈
        this.save(feedback);

        // 更新咨询记录的满意度
        record.setSatisfaction(satisfaction);
        consultationRecordService.updateById(record);
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总反馈数
        long totalFeedbacks = this.count();
        stats.put("totalFeedbacks", totalFeedbacks);
        
        // 待处理反馈数
        long pendingCount = this.count(new LambdaQueryWrapper<ConsultationFeedback>()
                .eq(ConsultationFeedback::getIsProcessed, 0));
        stats.put("pendingCount", pendingCount);
        
        // 平均满意度
        Double avgSatisfaction = this.getBaseMapper().selectMaps(
            new LambdaQueryWrapper<ConsultationFeedback>()
                .select(ConsultationFeedback::getSatisfaction)
        ).stream()
            .filter(m -> m.get("satisfaction") != null)
            .mapToDouble(m -> ((Number) m.get("satisfaction")).doubleValue())
            .average()
            .orElse(0.0);
        stats.put("avgSatisfaction", String.format("%.2f", avgSatisfaction));
        
        // 满意度分布
        Map<Integer, Long> satisfactionDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            long count = this.count(new LambdaQueryWrapper<ConsultationFeedback>()
                    .eq(ConsultationFeedback::getSatisfaction, i));
            satisfactionDistribution.put(i, count);
        }
        stats.put("satisfactionDistribution", satisfactionDistribution);
        
        // 反馈原因统计
        List<ConsultationFeedback> allFeedbacks = this.list();
        Map<String, Long> reasonStats = new HashMap<>();
        for (ConsultationFeedback feedback : allFeedbacks) {
            if (feedback.getFeedbackReasons() != null) {
                try {
                    List<String> reasons = objectMapper.readValue(
                        feedback.getFeedbackReasons(), 
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}
                    );
                    for (String reason : reasons) {
                        reasonStats.put(reason, reasonStats.getOrDefault(reason, 0L) + 1);
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        }
        
        // 按出现次数排序
        List<Map.Entry<String, Long>> sortedReasons = reasonStats.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());
        
        stats.put("topReasons", sortedReasons);
        
        // 低满意度问题（1-2 星）
        List<ConsultationFeedback> lowSatisfaction = this.list(new LambdaQueryWrapper<ConsultationFeedback>()
                .in(ConsultationFeedback::getSatisfaction, 1, 2)
                .orderByDesc(ConsultationFeedback::getCreatedTime)
                .last("LIMIT 10"));
        stats.put("lowSatisfactionFeedbacks", lowSatisfaction);
        
        return stats;
    }

    @Override
    public List<ConsultationFeedback> getPendingFeedbacks(Integer limit) {
        return this.list(new LambdaQueryWrapper<ConsultationFeedback>()
                .eq(ConsultationFeedback::getIsProcessed, 0)
                .orderByDesc(ConsultationFeedback::getCreatedTime)
                .last("LIMIT " + limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processFeedback(Long id, String remark, String processor) {
        ConsultationFeedback feedback = this.getById(id);
        if (feedback == null) {
            throw new BusinessException("反馈记录不存在");
        }
        
        feedback.setIsProcessed(1);
        feedback.setProcessRemark(remark);
        feedback.setProcessor(processor);
        feedback.setProcessTime(LocalDateTime.now());
        
        this.updateById(feedback);
    }
}
