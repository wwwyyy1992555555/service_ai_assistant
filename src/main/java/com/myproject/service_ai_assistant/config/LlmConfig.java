package com.myproject.service_ai_assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大模型配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.llm")
public class LlmConfig {
    
    /**
     * 是否启用大模型
     */
    private boolean enabled = false;
    
    /**
     * 大模型服务商：aliyun, baidu, kimi
     */
    private String provider = "aliyun";
    
    /**
     * API Key
     */
    private String apiKey;
    
    /**
     * API Secret
     */
    private String apiSecret;
    
    /**
     * 模型名称
     */
    private String model = "qwen-turbo";
    
    /**
     * 超时时间（秒）
     */
    private int timeout = 30;
    
    /**
     * 是否使用知识库增强
     */
    private boolean knowledgeEnhanced = true;
}