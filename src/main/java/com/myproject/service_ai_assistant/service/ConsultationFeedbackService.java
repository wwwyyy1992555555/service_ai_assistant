package com.myproject.service_ai_assistant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.service_ai_assistant.entity.ConsultationFeedback;

import java.util.List;
import java.util.Map;

/**
 * 咨询反馈服务接口
 */
public interface ConsultationFeedbackService extends IService<ConsultationFeedback> {

    /**
     * 提交反馈
     */
    void submitFeedback(Long consultationId, Integer satisfaction, List<String> reasons, String suggestion);

    /**
     * 获取反馈统计
     */
    Map<String, Object> getStatistics();

    /**
     * 获取待处理反馈列表
     */
    List<ConsultationFeedback> getPendingFeedbacks(Integer limit);

    /**
     * 获取所有反馈列表（分页）
     */
    Map<String, Object> getAllFeedbacks(Integer page, Integer size, Integer status, Integer satisfaction, String keyword);

    /**
     * 处理反馈
     */
    void processFeedback(Long id, String remark, String processor);
}
