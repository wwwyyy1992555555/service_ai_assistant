package com.myproject.service_ai_assistant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据清理任务启动器
 * 应用启动时自动执行历史数据清理
 */
@Slf4j
@Component
public class DataCleanupInitializer implements CommandLineRunner {
    
    @Value("${data.cleanup.enabled:false}")
    private boolean cleanupEnabled;
    
    @Value("${data.cleanup.retention-months:6}")
    private int retentionMonths;
    
    @Override
    public void run(String... args) throws Exception {
        if (!cleanupEnabled) {
            log.info("【数据清理任务】未启用，跳过初始化");
            return;
        }
        
        log.info("【数据清理任务】已启用，保留最近 {} 个月的数据", retentionMonths);
        log.info("【数据清理任务】将在应用启动后 10 分钟执行首次清理");
    }
}
