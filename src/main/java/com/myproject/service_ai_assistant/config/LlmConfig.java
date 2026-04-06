 package com.myproject.service_ai_assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

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
     * 大模型服务商:aliyun, baidu, kimi, zhipu
     */
    private String provider = "zhipu";
    
    /**
     * API Key
     */
    private String apiKey;
    
    /**
     * API Secret
     */
    private String apiSecret;
    
    /**
     * 模型名称(默认GLM-4-Flash免费模型)
     */
    private String model = "glm-4-flash";
    
    /**
     * API地址
     */
    private String apiUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    
    /**
     * 超时时间（秒）
     */
    private int timeout = 30;
    
    /**
     * 是否使用知识库增强
     */
    private boolean knowledgeEnhanced = true;
    
    /**
     * 是否启用联网搜索功能
     */
    private boolean webSearchEnabled = false;
    
    /**
     * 是否启用历史上下文加载
     */
    private boolean historyContextEnabled = true;
    
    /**
     * 联网搜索触发关键词
     */
    private List<String> webSearchKeywords = List.of(
        "今天", "今日", "昨天", "最近", "最新", "当前", "现在",
        "天气", "气温", "温度",
        "新闻", "消息", "动态", "资讯",
        "股价", "股票", "行情", "汇率",
        "比赛", "比分", "赛事","地址","在哪"
    );
    
    /**
     * 追问触发关键词（用于加载历史上下文）
     */
    private List<String> followUpKeywords = List.of(
        "刚才", "上面", "那个", "之前", "你说的",
        "它", "那个问题", "这件事",
        "继续", "还有呢", "然后呢",
        "不是", "不对", "错了", "换个",
        "详细", "展开", "具体"
    );
    
    /**
     * 触发上下文的最短问题长度（小于等于此值认为是追问）
     */
    private int shortQuestionLength = 6;
}