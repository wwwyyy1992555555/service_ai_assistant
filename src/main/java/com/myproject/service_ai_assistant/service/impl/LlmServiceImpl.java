package com.myproject.service_ai_assistant.service.impl;

import com.myproject.service_ai_assistant.config.LlmConfig;
import com.myproject.service_ai_assistant.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 大模型服务实现类 - 支持阿里云通义千问、智谱GLM等
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
    public String generateResponseWithContext(String question, String historyContext) {
        if (!llmConfig.isEnabled()) {
            log.debug("大模型未启用,返回默认回复");
            return getDefaultResponse(question);
        }
            
        log.info("调用大模型生成回复（带上下文）:{}", question);
            
        // 根据服务商调用不同API
        if ("zhipu".equalsIgnoreCase(llmConfig.getProvider())) {
            return callZhipuApiWithContext(question, historyContext, null);
        } else {
            return callQwenApiWithContext(question, historyContext, null);
        }
    }
    

    
    @Override
    public boolean isAvailable() {
        return llmConfig.isEnabled();
    }
    
    @Override
    public String generateResponseWithWebSearch(String question, String historyContext) {
        if (!llmConfig.isEnabled()) {
            log.debug("大模型未启用,返回默认回复");
            return getDefaultResponse(question);
        }
        
        log.info("调用大模型联网搜索:{}", question);
        
        // 目前仅智谱GLM支持联网搜索
        if ("zhipu".equalsIgnoreCase(llmConfig.getProvider())) {
            return callZhipuApiWithWebSearch(question, historyContext);
        } else {
            // 其他模型暂不支持联网，降级为普通调用
            log.warn("当前模型不支持联网搜索，降级为普通调用");
            return generateResponseWithContext(question, historyContext);
        }
    }
    
    /**
     * 调用智谱GLM API（带联网搜索）
     */
    private String callZhipuApiWithWebSearch(String question, String historyContext) {
        try {
            String url = llmConfig.getApiUrl();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + llmConfig.getApiKey());
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", llmConfig.getModel());
            
            // 构建messages数组
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 系统提示词
            messages.add(Map.of("role", "system", "content", 
                "你是一个智能客服助手。请根据搜索结果回答用户问题，要求：\n" +
                "1. 优先使用搜索结果中的最新信息\n" +
                "2. 如果用户提供了姓名和手机号，在末尾添加：[INFO_COLLECTED]姓名:XXX,手机:138XXXXXXXX[/INFO_COLLECTED]\n" +
                "3. 使用中文，语气友好专业"));
            
            // 如果有历史上下文
            if (historyContext != null && !historyContext.isEmpty()) {
                messages.add(Map.of("role", "user", "content", "历史对话：\n" + historyContext));
            }
            
            // 当前问题
            messages.add(Map.of("role", "user", "content", question));
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            
            // 开启联网搜索
            Map<String, Object> webSearchTool = new HashMap<>();
            webSearchTool.put("type", "web_search");
            webSearchTool.put("web_search", Map.of(
                "enable", true,
                "search_result", true  // 返回搜索结果
            ));
            requestBody.put("tools", List.of(webSearchTool));
            
            log.info("【联网搜索】请求智谱GLM: model={}", llmConfig.getModel());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    if (message != null && message.containsKey("content")) {
                        String answer = (String) message.get("content");
                        
                        // 检查是否有搜索结果
                        if (message.containsKey("tool_calls")) {
                            log.info("【联网搜索】已使用联网搜索功能");
                        }
                        
                        log.info("GLM联网回复成功:{}", answer);
                        return answer;
                    }
                }
            }
            
            log.warn("GLM联网返回格式异常:{}", responseBody);
            return getSimulatedResponse(question);
            
        } catch (Exception e) {
            log.error("调用GLM联网搜索失败:{}", e.getMessage(), e);
            return getSimulatedResponse(question);
        }
    }
    
    /**
     * 调用通义千问 API
     */
    private String callQwenApi(String question, String knowledgeContext) {
        try {
            // 构建请求
            String url = llmConfig.getApiUrl();
                
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
                
            // 发送请求(使用单例 RestTemplate)
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
                    log.info("大模型回复成功:{}", answer);
                    return answer;
                }
            }
                
            log.warn("大模型返回格式异常:{}", responseBody);
            return getSimulatedResponse(question);
                
        } catch (Exception e) {
            log.error("调用大模型失败:{}", e.getMessage());
            return getSimulatedResponse(question);
        }
    }
        
    /**
     * 调用智谱GLM API
     */
    private String callZhipuApi(String question, String knowledgeContext) {
        try {
            // 智谱API地址
            String url = llmConfig.getApiUrl();
                
            // 构建提示词
            String prompt = buildPrompt(question, knowledgeContext);
                
            // 请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + llmConfig.getApiKey());
                
            // 请求体(GLM使用messages数组格式)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", llmConfig.getModel());
                
            // 构建messages数组
            List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", prompt)
            );
            requestBody.put("messages", messages);
                
            // 参数配置
            requestBody.put("temperature", 0.7);  // 温度参数,控制随机性
            requestBody.put("max_tokens", 2000);   // 最大token数
                
            // 发送请求
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
                
            // 解析响应
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    if (message != null && message.containsKey("content")) {
                        String answer = (String) message.get("content");
                        log.info("GLM回复成功:{}", answer);
                        return answer;
                    }
                }
            }
                
            log.warn("GLM返回格式异常:{}", responseBody);
            return getSimulatedResponse(question);
                
        } catch (Exception e) {
            log.error("调用GLM失败:{}", e.getMessage(), e);
            return getSimulatedResponse(question);
        }
    }
    
    /**
     * 调用通义千问 API（带上下文）
     */
    private String callQwenApiWithContext(String question, String historyContext, String knowledgeContext) {
        try {
            String url = llmConfig.getApiUrl();
            String prompt = buildPromptWithContext(question, historyContext, knowledgeContext);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + llmConfig.getApiKey());
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", llmConfig.getModel());
            
            Map<String, Object> input = new HashMap<>();
            input.put("prompt", prompt);
            requestBody.put("input", input);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("result_format", "text");
            requestBody.put("parameters", parameters);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
                if (output.containsKey("text")) {
                    String answer = (String) output.get("text");
                    log.info("大模型回复成功:{}", answer);
                    return answer;
                }
            }
            
            log.warn("大模型返回格式异常:{}", responseBody);
            return getSimulatedResponse(question);
            
        } catch (Exception e) {
            log.error("调用大模型失败:{}", e.getMessage());
            return getSimulatedResponse(question);
        }
    }
    
    /**
     * 调用智谱GLM API（带上下文）
     */
    private String callZhipuApiWithContext(String question, String historyContext, String knowledgeContext) {
        try {
            String url = llmConfig.getApiUrl();
            String prompt = buildPromptWithContext(question, historyContext, knowledgeContext);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + llmConfig.getApiKey());
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", llmConfig.getModel());
            
            // GLM 支持多轮对话，构建 messages 数组
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 如果有历史上下文，先添加系统提示词
            if (historyContext != null && !historyContext.isEmpty()) {
                messages.add(Map.of("role", "system", "content", "以下是历史对话记录，请结合上下文理解用户当前问题。"));
                messages.add(Map.of("role", "user", "content", historyContext));
            }
            
            // 添加当前问题
            messages.add(Map.of("role", "user", "content", prompt));
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    if (message != null && message.containsKey("content")) {
                        String answer = (String) message.get("content");
                        log.info("GLM回复成功:{}", answer);
                        return answer;
                    }
                }
            }
            
            log.warn("GLM返回格式异常:{}", responseBody);
            return getSimulatedResponse(question);
            
        } catch (Exception e) {
            log.error("调用GLM失败:{}", e.getMessage(), e);
            return getSimulatedResponse(question);
        }
    }
    
    /**
     * 构建带上下文的提示词
     */
    private String buildPromptWithContext(String question, String historyContext, String knowledgeContext) {
        StringBuilder sb = new StringBuilder();
        
        // 添加历史上下文
        if (historyContext != null && !historyContext.trim().isEmpty()) {
            sb.append("【历史对话】\n");
            sb.append(historyContext);
            sb.append("\n\n");
        }
        
        // 添加知识库上下文
        if (knowledgeContext != null && !knowledgeContext.trim().isEmpty()) {
            sb.append("【参考资料】\n");
            sb.append(knowledgeContext);
            sb.append("\n\n");
        }
        
        sb.append("【用户当前问题】\n");
        sb.append(question);
        
        // 添加信息收集规则
        sb.append("\n\n【回复要求】\n");
        sb.append("1. 结合历史对话和参考资料回答\n");
        sb.append("2. 如果用户提供了姓名和手机号，在末尾添加：[INFO_COLLECTED]姓名:XXX,手机:138XXXXXXXX[/INFO_COLLECTED]\n");
        sb.append("3. 使用中文，语气友好专业\n");
        
        return sb.toString();
    }
    
    /**
     * 构建提示词（向后兼容）
     */
    private String buildPrompt(String question, String knowledgeContext) {
        return buildPromptWithContext(question, null, knowledgeContext);
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
