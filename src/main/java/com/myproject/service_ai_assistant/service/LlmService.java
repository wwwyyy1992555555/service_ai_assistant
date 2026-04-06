package com.myproject.service_ai_assistant.service;

/**
 * 大模型服务接口
 */
public interface LlmService {
    
    /**
     * 生成回复
     * @param question 用户问题
     * @return 回复内容
     */
    String generateResponse(String question);
    
    /**
     * 生成回复（带历史上下文）
     * @param question 用户当前问题
     * @param historyContext 历史对话上下文
     * @return 回复内容
     */
    String generateResponseWithContext(String question, String historyContext);
    
    /**
     * 基于知识库生成回复
     * @param question 用户问题
     * @param knowledgeContext 知识库上下文
     * @return 回复内容
     */
    String generateResponseWithKnowledge(String question, String knowledgeContext);
    
    /**
     * 检查服务是否可用
     * @return true=可用，false=不可用
     */
    boolean isAvailable();
    
    /**
     * 生成回复（带联网搜索）
     * @param question 用户问题
     * @param historyContext 历史对话上下文
     * @return 回复内容
     */
    String generateResponseWithWebSearch(String question, String historyContext);
}
