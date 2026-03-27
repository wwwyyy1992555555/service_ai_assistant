package com.myproject.service_ai_assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.service_ai_assistant.entity.ConsultationRecord;
import com.myproject.service_ai_assistant.mapper.ConsultationRecordMapper;
import com.myproject.service_ai_assistant.service.ConsultationRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 咨询对话记录服务实现类
 */
@Slf4j
@Service
public class ConsultationRecordServiceImpl extends ServiceImpl<ConsultationRecordMapper, ConsultationRecord> implements ConsultationRecordService {

    @Override
    public Page<ConsultationRecord> queryRecords(Long tenantId, Page<ConsultationRecord> page) {
        // 使用子查询按会话分组，每个会话显示第一次提问的记录
        long offset = (page.getCurrent() - 1) * page.getSize();
        long limit = page.getSize();
        List<ConsultationRecord> records = this.baseMapper.queryRecordsBySession(tenantId, offset, limit);
        
        // 查询总数（按会话分组）
        long total = this.baseMapper.countSessions(tenantId);
        
        // 为每个会话补充最新的用户信息
        if (!records.isEmpty()) {
            List<ConsultationRecord> processedRecords = new ArrayList<>();
            for (ConsultationRecord record : records) {
                // 查询该会话的最新用户信息
                LambdaQueryWrapper<ConsultationRecord> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ConsultationRecord::getSessionId, record.getSessionId())
                        .eq(ConsultationRecord::getTenantId, tenantId)
                        .orderByDesc(ConsultationRecord::getCreatedTime)
                        .last("LIMIT 1");
                
                ConsultationRecord latestRecord = this.getOne(wrapper);
                if (latestRecord != null) {
                    // 补充最新用户信息
                    record.setUserName(latestRecord.getUserName());
                    record.setUserPhone(latestRecord.getUserPhone());
                    record.setUserId(latestRecord.getUserId());
                }
                processedRecords.add(record);
            }
            records = processedRecords;
        }
        
        Page<ConsultationRecord> result = new Page<>(page.getCurrent(), page.getSize(), total);
        result.setRecords(records);
        
        log.debug("【对话记录查询】tenantId={}, current={}, size={}, total={}", tenantId, page.getCurrent(), page.getSize(), total);
        
        return result;
    }

    @Override
    public Page<ConsultationRecord> searchRecords(Long tenantId, String keyword, String userName, String userPhone, Page<ConsultationRecord> page) {
        // 如果没有搜索条件，返回空结果
        if ((keyword == null || keyword.trim().isEmpty()) && 
            (userName == null || userName.trim().isEmpty()) && 
            (userPhone == null || userPhone.trim().isEmpty())) {
            return queryRecords(tenantId, page);
        }
        
        // 构建查询条件
        LambdaQueryWrapper<ConsultationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationRecord::getTenantId, tenantId);
        
        // 智能识别：如果是手机号格式，优先按手机号精确匹配（高性能）
        if (userPhone != null && !userPhone.trim().isEmpty()) {
            String cleanPhone = userPhone.replaceAll("\\s+", "");
            // 先精确匹配完整手机号
            wrapper.eq(ConsultationRecord::getUserPhone, cleanPhone);
        }
        // 如果是姓名，按姓名模糊匹配
        else if (userName != null && !userName.trim().isEmpty()) {
            wrapper.like(ConsultationRecord::getUserName, userName);
        }
        // 否则按关键词搜索（只搜索问题字段，保证性能）
        else if (keyword != null && !keyword.trim().isEmpty()) {
            // 只搜索问题字段，避免双字段 LIKE 导致性能下降
            // 答案通常较长，且用户更可能搜索问题标题
            wrapper.like(ConsultationRecord::getQuestion, keyword);
        }
        
        // 按会话分组，获取每个会话的第一条记录
        wrapper.orderByDesc(ConsultationRecord::getCreatedTime);
        
        // 执行分页查询
        Page<ConsultationRecord> result = this.page(page, wrapper);
        
        // 为每个会话补充最新的用户信息
        if (!result.getRecords().isEmpty()) {
            List<ConsultationRecord> processedRecords = new ArrayList<>();
            for (ConsultationRecord record : result.getRecords()) {
                // 查询该会话的最新用户信息
                LambdaQueryWrapper<ConsultationRecord> userWrapper = new LambdaQueryWrapper<>();
                userWrapper.eq(ConsultationRecord::getSessionId, record.getSessionId())
                        .eq(ConsultationRecord::getTenantId, tenantId)
                        .orderByDesc(ConsultationRecord::getCreatedTime)
                        .last("LIMIT 1");
                
                ConsultationRecord latestRecord = this.getOne(userWrapper);
                if (latestRecord != null) {
                    // 补充最新用户信息
                    record.setUserName(latestRecord.getUserName());
                    record.setUserPhone(latestRecord.getUserPhone());
                    record.setUserId(latestRecord.getUserId());
                }
                processedRecords.add(record);
            }
            result.setRecords(processedRecords);
        }
        
        log.debug("【对话记录搜索（支持用户信息）】tenantId={}, keyword={}, userName={}, userPhone={}, current={}, size={}, total={}", 
                tenantId, keyword, userName, userPhone, page.getCurrent(), page.getSize(), result.getTotal());
        
        return result;
    }

    @Override
    public long countTodayConsultations(Long tenantId, LocalDate date) {
        LambdaQueryWrapper<ConsultationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationRecord::getTenantId, tenantId);
        
        // 查询当天的记录
        String startDate = date.toString() + " 00:00:00";
        String endDate = date.toString() + " 23:59:59";
        wrapper.between(ConsultationRecord::getCreatedTime, startDate, endDate);
        
        return this.count(wrapper);
    }

    @Override
    public double calculateSolveRate(Long tenantId, LocalDate date) {
        LambdaQueryWrapper<ConsultationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationRecord::getTenantId, tenantId);
        
        // 查询当天的记录
        String startDate = date.toString() + " 00:00:00";
        String endDate = date.toString() + " 23:59:59";
        wrapper.between(ConsultationRecord::getCreatedTime, startDate, endDate);
        
        long total = this.count(wrapper);
        if (total == 0) {
            return 0.0;
        }
        
        // 统计已解决的数量
        wrapper.eq(ConsultationRecord::getIsSolved, 1);
        long solved = this.count(wrapper);
        
        double rate = (double) solved / total * 100;
        log.debug("【解决率统计】date={}, total={}, solved={}, rate={:.2f}%", date, total, solved, rate);
        
        return Math.round(rate * 100) / 100.0; // 保留两位小数
    }

    @Override
    public List<ConsultationRecord> getRecentRecords(Long tenantId, long limit) {
        LambdaQueryWrapper<ConsultationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationRecord::getTenantId, tenantId)
                .orderByDesc(ConsultationRecord::getCreatedTime)
                .last("LIMIT " + limit);
        
        return this.list(wrapper);
    }

    @Override
    public double calculateAvgSatisfaction(Long tenantId, LocalDate date) {
        // 使用 MyBatis-Plus 的聚合查询
        LambdaQueryWrapper<ConsultationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationRecord::getTenantId, tenantId)
                .isNotNull(ConsultationRecord::getSatisfaction);  // 只统计有评价的记录
        
        // 查询当天的记录
        String startDate = date.toString() + " 00:00:00";
        String endDate = date.toString() + " 23:59:59";
        wrapper.between(ConsultationRecord::getCreatedTime, startDate, endDate);
        
        // 查询所有符合条件的记录
        List<ConsultationRecord> records = this.list(wrapper);
        
        if (records.isEmpty()) {
            return 0.0;  // 没有评价时返回 0
        }
        
        // 计算平均满意度
        double totalSatisfaction = records.stream()
                .mapToDouble(ConsultationRecord::getSatisfaction)
                .sum();
        
        double avg = totalSatisfaction / records.size();
        log.debug("【满意度统计】date={}, count={}, avg={:.2f}", date, records.size(), avg);
        
        return Math.round(avg * 100) / 100.0;  // 保留两位小数
    }
}
