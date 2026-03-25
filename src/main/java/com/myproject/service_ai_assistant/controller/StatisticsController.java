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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 数据统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@Tag(name = "数据统计接口")
public class StatisticsController {

    @Autowired
    private ConsultationRecordService consultationRecordService;

    @Autowired
    private KnowledgeItemService knowledgeItemService;

    @GetMapping("/dashboard")
    @Operation(summary = "获取看板数据", description = "返回管理后台首页的统计数据")
    public Result<DashboardData> getDashboardData(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "统计日期，默认为今天") 
            @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        if (date == null) {
            date = LocalDate.now();
        }

        log.info("【数据统计】查询看板数据：tenantId={}, date={}", tenantId, date);

        // 1. 今日咨询量
        long todayConsultations = consultationRecordService.countTodayConsultations(tenantId, date);

        // 2. 解决率
        double solveRate = consultationRecordService.calculateSolveRate(tenantId, date);

        // 3. 知识库总数
        long knowledgeCount = knowledgeItemService.countKnowledge(tenantId);

        // 4. 已发布知识数
        long publishedCount = knowledgeItemService.countPublished(tenantId);

        // 5. 计算增长率（对比昨天的数据）
        LocalDate yesterday = date.minusDays(1);
        int consultationGrowth = calculateConsultationGrowth(tenantId, date, yesterday);
        int solveRateGrowth = calculateSolveRateGrowth(tenantId, date, yesterday);

        // 6. 平均满意度（从数据库统计今天的平均值）
        double avgSatisfaction = consultationRecordService.calculateAvgSatisfaction(tenantId, date);

        DashboardData data = new DashboardData();
        data.setTodayConsultations(todayConsultations);
        data.setConsultationGrowth(consultationGrowth);
        data.setSolveRate(Math.round(solveRate * 100) / 100.0);
        data.setSolveRateGrowth(solveRateGrowth);
        data.setAvgSatisfaction(avgSatisfaction);
        data.setKnowledgeCount(knowledgeCount);
        data.setPublishedCount(publishedCount);  // 已发布知识数量

        log.info("【数据统计】看板数据：todayConsultations={}, solveRate={}%, knowledgeCount={}", 
                todayConsultations, solveRate, knowledgeCount);

        return Result.success(data);
    }

    @GetMapping("/hot-questions")
    @Operation(summary = "获取热门问题 TOP10", description = "返回浏览次数最高的问题列表")
    public Result<List<KnowledgeItem>> getHotQuestions(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "10") Integer limit
    ) {
        log.info("【数据统计】查询热门问题：tenantId={}, limit={}", tenantId, limit);
        
        List<KnowledgeItem> hotQuestions = knowledgeItemService.getHotQuestions(tenantId, limit);
        
        log.info("【数据统计】热门问题数量：{}", hotQuestions.size());
        return Result.success(hotQuestions);
    }

    @GetMapping("/recent-records")
    @Operation(summary = "获取最近对话记录", description = "返回最近的对话记录")
    public Result<List<ConsultationRecord>> getRecentRecords(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "10") Integer limit
    ) {
        log.info("【数据统计】查询最近对话：tenantId={}, limit={}", tenantId, limit);
        
        List<ConsultationRecord> records = consultationRecordService.getRecentRecords(tenantId, limit);
        
        log.info("【数据统计】最近对话数量：{}", records.size());
        return Result.success(records);
    }

    /**
     * 计算咨询量增长率（对比昨天）
     */
    private int calculateConsultationGrowth(Long tenantId, LocalDate today, LocalDate yesterday) {
        long todayCount = consultationRecordService.countTodayConsultations(tenantId, today);
        long yesterdayCount = consultationRecordService.countTodayConsultations(tenantId, yesterday);
        
        if (yesterdayCount == 0) {
            return todayCount > 0 ? 100 : 0;
        }
        
        // 计算增长率百分比
        double growth = ((double)(todayCount - yesterdayCount) / yesterdayCount) * 100;
        return (int)Math.round(growth);
    }

    /**
     * 计算解决率增长率（对比昨天）
     */
    private int calculateSolveRateGrowth(Long tenantId, LocalDate today, LocalDate yesterday) {
        double todayRate = consultationRecordService.calculateSolveRate(tenantId, today);
        double yesterdayRate = consultationRecordService.calculateSolveRate(tenantId, yesterday);
        
        // 计算百分点变化（直接相减，不需要乘以 100）
        double growth = todayRate - yesterdayRate;
        return (int)Math.round(growth);
    }

    /**
     * 看板数据结构
     */
    @Data
    public static class DashboardData {
        
        private Long todayConsultations;      // 今日咨询量
        private Integer consultationGrowth;   // 咨询量增长率 (%)
        private Double solveRate;             // 问题解决率 (%)
        private Integer solveRateGrowth;      // 解决率增长率 (%)
        private Double avgSatisfaction;       // 平均满意度
        private Long knowledgeCount;          // 知识库条目总数
        private Long publishedCount;          // 已发布知识数量
    }
}
