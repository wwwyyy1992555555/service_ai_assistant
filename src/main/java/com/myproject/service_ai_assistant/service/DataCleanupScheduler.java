package com.myproject.service_ai_assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myproject.service_ai_assistant.entity.ConsultationRecord;
import com.myproject.service_ai_assistant.mapper.ConsultationRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 数据清理定时任务
 * 定期清理 N 个月前的对话记录
 */
@Slf4j
@Service
public class DataCleanupScheduler {
    
    @Autowired
    private ConsultationRecordMapper consultationRecordMapper;
    
    @Value("${data.cleanup.enabled:false}")
    private boolean cleanupEnabled;
    
    @Value("${data.cleanup.retention-months:6}")
    private int retentionMonths;
    
    @Value("${data.cleanup.cron:0 0 2 * * ?}")
    private String cleanupCron;
    
    /**
     * 定期清理任务
     * 默认每天凌晨 2 点执行
     */
    @Scheduled(cron = "${data.cleanup.cron:0 0 2 * * ?}")
    public void cleanupExpiredRecords() {
        if (!cleanupEnabled) {
            log.debug("【数据清理】任务未启用，跳过执行");
            return;
        }
        
        try {
            // 计算过期时间（N 个月前的日期）
            LocalDateTime expireTime = LocalDateTime.now().minusMonths(retentionMonths);
            String expireTimeStr = expireTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            log.info("【数据清理】开始清理 {} 个月前的数据，过期时间：{}", retentionMonths, expireTimeStr);
            
            // 构建查询条件
            LambdaQueryWrapper<ConsultationRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.lt(ConsultationRecord::getCreatedTime, expireTimeStr);
            
            // 查询过期记录数量
            long count = consultationRecordMapper.selectCount(wrapper);
            if (count == 0) {
                log.info("【数据清理】没有需要清理的数据");
                return;
            }
            
            log.info("【数据清理】发现 {} 条过期记录，开始删除...", count);
            
            // 删除过期记录（逻辑删除）
            int deleted = consultationRecordMapper.delete(wrapper);
            
            log.info("【数据清理】清理完成，共删除 {} 条记录", deleted);
            
        } catch (Exception e) {
            log.error("【数据清理】执行失败", e);
        }
    }
    
    /**
     * 启动后立即执行一次（延迟 10 分钟）
     * 使用 fixedDelay 确保只执行一次
     */
    @Scheduled(initialDelay = 600000, fixedDelay = Long.MAX_VALUE)
    public void initialCleanup() {
        if (!cleanupEnabled) {
            return;
        }
        
        log.info("【数据清理】执行启动后首次清理任务");
        cleanupExpiredRecords();
    }
}
