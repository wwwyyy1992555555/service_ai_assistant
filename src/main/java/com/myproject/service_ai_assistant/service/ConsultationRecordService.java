package com.myproject.service_ai_assistant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.service_ai_assistant.entity.ConsultationRecord;

import java.time.LocalDate;
import java.util.List;

/**
 * 咨询对话记录服务接口
 */
public interface ConsultationRecordService extends IService<ConsultationRecord> {

    /**
     * 分页查询对话记录
     * @param tenantId 租户 ID
     * @param page 分页参数
     * @return 分页结果
     */
    Page<ConsultationRecord> queryRecords(Long tenantId, Page<ConsultationRecord> page);

    /**
     * 按问题搜索对话记录
     * @param tenantId 租户 ID
     * @param keyword 关键词
     * @param page 分页参数
     * @return 分页结果
     */
    Page<ConsultationRecord> searchRecords(Long tenantId, String keyword, Page<ConsultationRecord> page);

    /**
     * 统计今日咨询量
     * @param tenantId 租户 ID
     * @param date 日期
     * @return 咨询数量
     */
    long countTodayConsultations(Long tenantId, LocalDate date);

    /**
     * 统计解决率
     * @param tenantId 租户 ID
     * @param date 日期
     * @return 解决率 (0-100)
     */
    double calculateSolveRate(Long tenantId, LocalDate date);

    /**
     * 获取最近对话记录
     * @param tenantId 租户 ID
     * @param limit 数量限制
     * @return 对话记录列表
     */
    List<ConsultationRecord> getRecentRecords(Long tenantId, long limit);

    /**
     * 计算平均满意度
     * @param tenantId 租户 ID
     * @param date 日期
     * @return 平均满意度（1-5 分），如果没有评价则返回 0
     */
    double calculateAvgSatisfaction(Long tenantId, LocalDate date);
}
