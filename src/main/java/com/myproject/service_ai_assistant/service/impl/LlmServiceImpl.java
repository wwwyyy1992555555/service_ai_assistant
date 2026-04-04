package com.myproject.service_ai_assistant.service.impl;

import com.myproject.service_ai_assistant.config.LlmConfig;
import com.myproject.service_ai_assistant.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 大模型服务实现类 - 接入阿里云通义千问
 */
@Slf4j
@Service
public class LlmServiceImpl implements LlmService {
    
    private final LlmConfig llmConfig;
    private RestTemplate restTemplate;
    
    public LlmServiceImpl(LlmConfig llmConfig) {
        this.llmConfig = llmConfig;
    }
    
    @PostConstruct
    public void init() {
        // 初始化单例 RestTemplate，避免重复创建导致内存泄漏
        restTemplate = new RestTemplate();
        log.info("【LLM服务】RestTemplate 初始化完成");
    }
    
    @Override
    public String generateResponse(String question) {
        if (!llmConfig.isEnabled()) {
            log.debug("大模型未启用，返回默认回复");
            return getDefaultResponse(question);
        }
        
        log.info("调用大模型生成回复：{}", question);
        
        // 调用通义千问 API
        return callQwenApi(question, null);
    }
    
    @Override
    public String generateResponseWithKnowledge(String question, String knowledgeContext) {
        if (!llmConfig.isEnabled()) {
            log.debug("大模型未启用，返回默认回复");
            return getDefaultResponse(question);
        }
        
        log.info("基于知识库调用大模型：{}", question);
        log.debug("知识库上下文：{}", knowledgeContext);
        
        // 调用通义千问 API，带上知识库上下文
        return callQwenApi(question, knowledgeContext);
    }
    
    @Override
    public boolean isAvailable() {
        return llmConfig.isEnabled();
    }
    
    /**
     * 调用通义千问 API
     */
    private String callQwenApi(String question, String knowledgeContext) {
        try {
            // 构建请求
            String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
            
            // 构建提示词
            String prompt = buildPrompt(question, knowledgeContext);
            
            // 请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + llmConfig.getApiKey());
            
            // 请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", llmConfig.getModel());
            
            Map<String, Object> input = new HashMap<>();
            input.put("prompt", prompt);
            requestBody.put("input", input);
            
            // 参数配置
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("result_format", "text");
            requestBody.put("parameters", parameters);
            
            // 发送请求（使用单例 RestTemplate）
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            // 解析响应
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
                if (output.containsKey("text")) {
                    String answer = (String) output.get("text");
                    log.info("大模型回复成功：{}", answer);
                    return answer;
                }
            }
            
            log.warn("大模型返回格式异常：{}", responseBody);
            return getSimulatedResponse(question);
            
        } catch (Exception e) {
            log.error("调用大模型失败：{}", e.getMessage());
            return getSimulatedResponse(question);
        }
    }
    
    /**
     * 构建提示词
     */
    private String buildPrompt(String question, String knowledgeContext) {
        StringBuilder sb = new StringBuilder();
        
        if (knowledgeContext != null && !knowledgeContext.trim().isEmpty()) {
            sb.append("请基于以下参考资料，用专业、友好、简洁的语言回答用户问题。\n\n");
            sb.append("【参考资料】\n");
            sb.append(knowledgeContext);
            sb.append("\n\n");
            sb.append("【要求】\n");
            sb.append("1. 基于参考资料回答，不要编造信息\n");
            sb.append("2. 如果参考资料不足，请礼貌说明\n");
            sb.append("3. 使用中文回答\n");
            sb.append("4. 适当使用 emoji 表情，让回复更亲切\n\n");
        } else {
            sb.append("你是一个专业的政企 AI 助手，请用友好、专业、简洁的语言回答用户问题。\n\n");
        }
        
        sb.append("【用户问题】\n");
        sb.append(question);
        
        return sb.toString();
    }
    
    /**
     * 获取默认回复（大模型未启用时）
     */
    private String getDefaultResponse(String question) {
        return "抱歉，我暂时没有找到相关信息。您可以尝试换个方式描述问题，或者留下联系方式，我们会安排专人为您解答。";
    }
    
    /**
     * 模拟回复（API 调用失败时使用）
     */
    private String getSimulatedResponse(String question) {
        // API 调用失败时的降级回复
        log.warn("使用模拟回复（可能是 API Key 无效或网络问题）");
        
        if (question.contains("电话") || question.contains("手机") || question.contains("联系")) {
            return "您好！如果您需要人工服务，可以留下您的联系方式（手机号），我们会安排专人与您联系。您看方便吗？😊";
        }
        
        if (question.contains("姓名") || question.contains("姓") || question.contains("名")) {
            return "您好！请问您怎么称呼呢？如果方便的话，可以告诉我们您的姓氏和联系方式，这样能更好地为您服务。😊";
        }
        
        // 通用回复
        return "您好！感谢您的咨询。我已经理解了您的问题：" + question + "\n\n请问您还有其他需要了解的吗？或者您可以留下联系方式，我们会安排专人为您提供详细解答。😊";
    }
}
