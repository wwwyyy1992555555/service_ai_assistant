package com.myproject.service_ai_assistant.controller;

import cn.hutool.core.util.StrUtil;
import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.common.SimilarityUtil;
import com.myproject.service_ai_assistant.config.LlmConfig;
import com.myproject.service_ai_assistant.dto.ConsultRequest;
import com.myproject.service_ai_assistant.dto.ConsultResponse;
import com.myproject.service_ai_assistant.dto.UserInfoParseRequest;
import com.myproject.service_ai_assistant.entity.ConsultationRecord;
import com.myproject.service_ai_assistant.entity.KnowledgeItem;
import com.myproject.service_ai_assistant.service.ConsultationRecordService;
import com.myproject.service_ai_assistant.service.KnowledgeItemService;
import com.myproject.service_ai_assistant.service.LlmService;
import com.myproject.service_ai_assistant.service.SystemConfigService;
import com.myproject.service_ai_assistant.service.UserInfoParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 智能问答控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/consult")
@Tag(name = "智能问答接口")
public class ConsultController {

    @Autowired
    private KnowledgeItemService knowledgeItemService;

    @Autowired
    private ConsultationRecordService consultationRecordService;
    
    @Autowired
    private LlmService llmService;
    
    @Autowired
    private LlmConfig llmConfig;
    
    @Autowired
    private UserInfoParserService userInfoParser;
    
    @Autowired
    private SystemConfigService systemConfigService;

    @PostMapping("/ask")
    @Operation(summary = "智能问答 - 用户提问", description = "用户提出问题，系统从知识库中匹配答案并返回")
    public Result<ConsultResponse> ask(@Valid @RequestBody ConsultRequest request) {
        log.info("【智能问答】收到咨询请求：sessionId={}, tenantId={}, question={}", 
                request.getSessionId(), request.getTenantId(), request.getQuestion());

        // 1. 提取关键词用于搜索
        String searchKeyword = userInfoParser.extractKeyword(request.getQuestion());
        log.info("【智能问答】搜索关键词：{}", searchKeyword);
        
        // 2. 搜索知识库匹配答案 - 优化匹配逻辑
        List<KnowledgeItem> matchedKnowledge = knowledgeItemService.searchKnowledge(
                request.getTenantId(), 
                searchKeyword != null ? searchKeyword : request.getQuestion()
        );
        
        log.debug("【智能问答】匹配到 {} 条知识", matchedKnowledge.size());

        String answer;
        Long matchedKnowledgeId = null;
        Double matchScore = 0.0;
        String knowledgeTitle = null;
        String categoryName = null;
        Integer viewCount = 0;

        if (!matchedKnowledge.isEmpty()) {
            // 找到匹配的知识
            KnowledgeItem item = matchedKnowledge.get(0);
            
            // 使用智能计算的匹配度
            matchScore = calculateMatchScore(request.getQuestion(), item);
            log.info("【智能问答】智能匹配度：{}", matchScore);
            
            // 混合模式：根据匹配度决定是否使用大模型
            if (matchScore >= 0.8) {
                // 高匹配度：直接使用知识库答案（快速、准确）
                log.info("【智能问答】高匹配度，直接使用知识库答案");
                answer = item.getAnswer();
            } else if (llmConfig.isEnabled() && llmConfig.isKnowledgeEnhanced()) {
                // 中匹配度：大模型基于知识库润色（准确 + 自然）
                log.info("【智能问答】中匹配度，使用大模型润色答案");
                String knowledgeContext = buildKnowledgeContext(matchedKnowledge);
                answer = llmService.generateResponseWithKnowledge(request.getQuestion(), knowledgeContext);
            } else {
                // 未启用大模型或不需要增强：使用知识库答案
                log.info("【智能问答】使用知识库答案");
                answer = item.getAnswer();
            }
            
            matchedKnowledgeId = item.getId();
            knowledgeTitle = item.getTitle();
            categoryName = getCategoryName(item.getCategoryId());
            viewCount = item.getViewCount() + 1;
            
            log.info("【智能问答】匹配到知识：id={}, title={}, score={}", item.getId(), item.getTitle(), matchScore);
            
            // 增加浏览次数
            item.setViewCount(viewCount);
            knowledgeItemService.updateById(item);
        } else {
            // 未找到匹配，先检查是否有关键信息，再检查礼貌用语
            log.warn("【智能问答】未找到匹配的知识，question={}", request.getQuestion());
            
            // 1. 优先检查是否包含关键业务信息（证件、业务等）
            String keyword = userInfoParser.extractKeyword(request.getQuestion());
            boolean hasKeyInfo = keyword != null && !keyword.equals(request.getQuestion());
            
            if (hasKeyInfo) {
                // 有明确的关键信息但没匹配到知识，使用大模型或智能回复
                log.info("【智能问答】检测到关键信息：{}，使用大模型/智能回复", keyword);
                if (llmConfig.isEnabled()) {
                    answer = llmService.generateResponse(request.getQuestion());
                } else {
                    // 先解析用户当前输入，提取姓名和手机号（以最新输入为准）
                    String[] userInfo = parseUserInfo(request.getQuestion(), request.getSessionId());
                    answer = generateSmartResponse(request.getQuestion(), userInfo[0], userInfo[1]);
                }
            } else {
                // 2. 没有关键信息，检查是否是礼貌用语
                String politeResponse = generatePoliteResponse(request.getQuestion());
                if (politeResponse != null) {
                    log.info("【智能问答】使用礼貌用语回复");
                    answer = politeResponse;
                } else if (llmConfig.isEnabled()) {
                    // 3. 启用大模型：调用大模型生成回复
                    log.info("【智能问答】使用大模型生成回复");
                    answer = llmService.generateResponse(request.getQuestion());
                } else {
                    // 4. 未启用大模型：使用智能回复（根据用户信息动态调整）
                    log.info("【智能问答】使用智能回复模板");
                    // 先解析用户当前输入，提取姓名和手机号（以最新输入为准）
                    String[] userInfo = parseUserInfo(request.getQuestion(), request.getSessionId());
                    answer = generateSmartResponse(request.getQuestion(), userInfo[0], userInfo[1]);
                }
            }
        }

        // === 新增：解析用户当前输入，提取姓名和手机号（以最新输入为准）===
        String[] userInfo = parseUserInfo(request.getQuestion(), request.getSessionId());
        String currentName = userInfo[0];
        String currentPhone = userInfo[1];
        
        log.info("【用户信息更新】最终使用的姓名：{}, 手机号：{}", currentName, currentPhone);

        // 2. 保存对话记录（使用更新后的姓名和手机号）
        ConsultationRecord record = new ConsultationRecord();
        record.setTenantId(request.getTenantId());
        record.setSessionId(request.getSessionId());
        record.setUserId(request.getUserId());
        record.setUserName(currentName);  // 使用更新后的姓名
        record.setUserPhone(currentPhone);  // 使用更新后的手机号
        record.setQuestion(request.getQuestion());
        record.setAnswer(answer);
        record.setMatchedKnowledgeId(matchedKnowledgeId);
        record.setMatchScore(matchScore);
        record.setIsSolved(matchedKnowledgeId != null ? 1 : 0);
        record.setDeviceType(request.getDeviceType());
        record.setIpAddress(request.getIpAddress());
        
        consultationRecordService.save(record);
        log.info("【智能问答】保存对话记录：recordId={}, name={}, phone={}", 
                record.getId(), currentName, currentPhone);

        // 3. 返回结果
        ConsultResponse response = new ConsultResponse();
        response.setAnswer(answer);
        response.setMatchedKnowledgeId(matchedKnowledgeId);
        response.setMatchScore(matchScore);
        response.setKnowledgeTitle(knowledgeTitle);
        response.setCategoryName(categoryName);
        response.setViewCount(viewCount);
        response.setConsultationId(record.getId()); // 添加咨询记录 ID
        response.setSuggestedQuestions(matchedKnowledge.stream()
                .skip(1)
                .limit(3)
                .map(KnowledgeItem::getQuestion)
                .toList());
        
        log.info("【智能问答】返回答案：answerLength={}, suggestedQuestions={}", 
                answer.length(), response.getSuggestedQuestions().size());

        return Result.success(response);
    }

    @PostMapping("/parse-user-input")
    @Operation(summary = "解析用户输入", description = "智能识别用户输入中的姓名、手机号等信息")
    public Result<UserInfoParserService.UserInfo> parseUserInput(@Valid @RequestBody UserInfoParseRequest request) {
        log.info("【解析用户输入】sessionId={}, text={}", request.getSessionId(), request.getText());
        
        UserInfoParserService.UserInfo userInfo = userInfoParser.parse(request.getText());
        
        log.info("【解析用户输入】结果：name={}, phone={}, isQuestion={}", 
                userInfo.getName(), userInfo.getPhone(), userInfo.isQuestion());
        
        return Result.success(userInfo);
    }

    @GetMapping("/hot-questions")
    @Operation(summary = "获取热门问题", description = "返回指定数量的热门问题列表")
    public Result<List<KnowledgeItem>> getHotQuestions(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "10") Integer limit
    ) {
        log.info("【热门问题】开始查询，tenantId={}, limit={}", tenantId, limit);
        List<KnowledgeItem> hotQuestions = knowledgeItemService.getHotQuestions(tenantId, limit);
        log.info("【热门问题】查询结果，count={}", hotQuestions.size());
        return Result.success(hotQuestions);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索对话记录", description = "根据关键词搜索问题和答案，或根据用户信息（姓名/手机号）搜索")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<ConsultationRecord>> searchRecords(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "关键词（问题/答案/姓名/手机号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("【搜索对话】tenantId={}, keyword={}, current={}, size={}", tenantId, keyword, current, size);
        
        // 智能识别关键词类型
        String userName = null;
        String userPhone = null;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            String cleanKeyword = keyword.trim();
            
            // 判断是否为手机号格式
            if (cleanKeyword.matches("1[3-9]\\d{9}")) {
                userPhone = cleanKeyword;
            } else {
                // 否则作为问题/答案/姓名的关键词搜索
                userName = cleanKeyword;
            }
        }
        
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<ConsultationRecord>(current, size);
        var result = consultationRecordService.searchRecords(tenantId, keyword, userName, userPhone, page);
        
        log.info("【搜索对话】匹配到 {} 条记录", result.getTotal());
        return Result.success(result);
    }
    
    @GetMapping("/list")
    @Operation(summary = "分页查询对话记录", description = "分页查询对话记录列表")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<ConsultationRecord>> listRecords(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("【对话列表】tenantId={}, current={}, size={}", tenantId, current, size);
        
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<ConsultationRecord>(current, size);
        var result = consultationRecordService.queryRecords(tenantId, page);
        
        log.info("【对话列表】查询成功：total={}", result.getTotal());
        return Result.success(result);
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "删除会话", description = "删除整个会话的所有对话记录")
    public Result<Void> deleteSession(@Parameter(description = "会话 ID", required = true) @PathVariable String sessionId) {
        log.info("【删除会话】sessionId={}", sessionId);
        
        // 查询该会话的所有记录
        var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConsultationRecord>()
                .eq(ConsultationRecord::getSessionId, sessionId);
        
        List<ConsultationRecord> records = consultationRecordService.list(queryWrapper);
        
        if (records.isEmpty()) {
            log.warn("【删除会话】会话不存在：sessionId={}", sessionId);
            return Result.error(404, "会话不存在");
        }
        
        // 批量删除（逻辑删除）
        boolean success = consultationRecordService.remove(queryWrapper);
        
        if (success) {
            log.info("【删除会话】删除成功：sessionId={}, 删除记录数={}", sessionId, records.size());
            return Result.success(null);
        } else {
            log.error("【删除会话】删除失败：sessionId={}", sessionId);
            return Result.error(500, "删除失败");
        }
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "查询会话详情", description = "根据会话 ID 查询完整的对话历史")
    public Result<List<ConsultationRecord>> getSessionDetail(@Parameter(description = "会话 ID", required = true) @PathVariable String sessionId) {
        log.info("【会话详情】sessionId={}", sessionId);
        
        var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConsultationRecord>()
                .eq(ConsultationRecord::getSessionId, sessionId)
                .orderByAsc(ConsultationRecord::getCreatedTime);
        
        var records = consultationRecordService.list(queryWrapper);
        
        log.info("【会话详情】查询到 {} 条记录", records.size());
        return Result.success(records);
    }

    /**
     * 生成智能回复（根据用户信息动态调整）
     */
    private String generateSmartResponse(String question, String userName, String userPhone) {
        // 从系统配置中读取服务热线和工作时间
        String servicePhone = "12345";
        String serviceTime = "工作时间：周一至周日 9:00-17:00";
        
        try {
            var config = systemConfigService.getConfig(1L);
            if (config != null) {
                servicePhone = config.getPhone() != null && !config.getPhone().isEmpty() ? config.getPhone() : "12345";
                serviceTime = config.getServiceTime() != null && !config.getServiceTime().isEmpty() ? config.getServiceTime() : "工作时间：周一至周日 9:00-17:00";
            }
        } catch (Exception e) {
            log.warn("【获取系统配置失败】使用默认值，error={}", e.getMessage());
        }
        
        StringBuilder sb = new StringBuilder();
        
        // 1. 检查用户是否只发了姓名
        if (userName != null && userPhone == null) {
            sb.append(userName).append("先生/女士，您好！👋\n\n");
            sb.append("很高兴为您服务！请问您想咨询什么问题呢？\n\n");
            sb.append("💡 小提示：\n");
            sb.append("您可以直接描述遇到的问题，\n");
            sb.append("如果需要人工客服，请留下手机号哦~\n");
            sb.append("📱 示例：13800138000");
            return sb.toString();
        }
        
        // 2. 检查用户是否只发了手机号
        if (userName == null && userPhone != null) {
            sb.append("您好！👋\n\n");
            sb.append("已经记下您的联系方式啦：").append(formatPhone(userPhone)).append("\n\n");
            sb.append("请问您想咨询什么问题呢？\n\n");
            sb.append("💡 小提示：\n");
            sb.append("您可以直接描述遇到的问题，\n");
            sb.append("如果方便的话，也可以告诉我们您怎么称呼~");
            return sb.toString();
        }
        
        // 3. 用户同时提供了姓名和电话
        if (userName != null && userPhone != null) {
            sb.append(userName).append("先生/女士，您好！👋\n\n");
            sb.append("非常感谢您的信任！\n\n");
            sb.append("📝 您的信息我们已经记下啦：\n");
            sb.append("   • 姓名：").append(userName).append("\n");
            sb.append("   • 电话：").append(formatPhone(userPhone)).append("\n\n");
            sb.append("稍后会有专业客服与您联系，请保持电话畅通哦！\n\n");
            sb.append("📞 如果有紧急问题，也可以直接拨打我们的服务热线：").append(servicePhone).append("\n");
            sb.append("（").append(serviceTime).append("）");
            return sb.toString();
        }
        
        // 4. 用户既没留姓名也没留电话
        sb.append("您好！🤔\n\n");
        sb.append("我暂时没有找到相关信息，不过别着急，您可以试试以下方法：\n\n");
        sb.append("💡 解决方案：\n\n");
        sb.append("1️⃣ 留下联系方式\n");
        sb.append("   专业客服会尽快联系您\n");
        sb.append("   📱 格式：姓王，13800138000\n\n");
        sb.append("2️⃣ 换个方式描述问题\n");
        sb.append("   比如加上具体地点、时间等\n\n");
        sb.append("3️⃣ 直接拨打服务热线\n");
        sb.append("   📞 ").append(servicePhone).append("（").append(serviceTime).append("）\n\n");
        sb.append("请问您想选择哪种方式呢？😊");
        
        return sb.toString();
    }
    
    /**
     * 格式化手机号（中间加空格）
     */
    private String formatPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.replaceAll("(\\d{3})(\\d{4})(\\d{4})", "$1 $2 $3");
    }
    
    /**
     * 生成礼貌用语回复
     * @param question 用户输入
     * @return 礼貌用语回复，如果不是礼貌用语则返回 null
     */
    private String generatePoliteResponse(String question) {
        if (question == null || question.trim().isEmpty()) {
            return null;
        }
        
        String cleanText = question.trim();
        
        // === 问候语 ===
        if (cleanText.matches(".*(你好|您好|hello|hi|Hi|HI|早上好|中午好|晚上好|晚安).*")) {
            return "您好！👋 很高兴为您服务！请问有什么可以帮您？";
        }
        
        // === 感谢语 ===
        if (cleanText.matches(".*(谢谢 | 感谢 | 谢谢你 | 感谢您 | 非常感谢 | 太感谢了).*")) {
            return "不客气！😊 这是我应该做的，还有其他问题吗？";
        }
        
        // === 再见语 ===
        if (cleanText.matches(".*(再见 | 拜拜 | bye|Bye|BYE|下次见 | 回见).*")) {
            return "再见！祝您生活愉快！👋";
        }
                
        // === 道歉语 ===
        if (cleanText.matches(".*(对不起 | 抱歉 | 不好意思 | 请原谅).*")) {
            return "没关系的！😊 请问有什么可以帮您？";
        }
                
        // === 肯定语 ===
        if (cleanText.matches(".*(好的 | 好的谢谢 | 明白了 | 知道了 | 了解 | 懂了|ok|OK|Ok).*")) {
            return "好的！😊 如果有其他问题，随时都可以问我哦~";
        }
                
        // === 否定语 ===
        if (cleanText.matches(".*(不用了 | 不需要 | 算了 | 没关系 | 没事).*")) {
            return "好的，如果您有其他问题，欢迎随时咨询！😊";
        }
        
        return null;  // 不是礼貌用语，返回 null 继续后续处理
    }
    
    /**
     * 解析用户输入中的姓名和手机号（以最新输入为准）
     * @param question 用户输入
     * @param sessionId 会话 ID
     * @return String[0]=姓名，String[1]=手机号
     */
    private String[] parseUserInfo(String question, String sessionId) {
        String currentName = null;
        String currentPhone = null;
        
        // 1. 解析当前输入
        UserInfoParserService.UserInfo parsedInfo = userInfoParser.parse(question);
        if (parsedInfo.getName() != null) {
            currentName = parsedInfo.getName();
            log.info("【用户信息更新】从当前输入中提取到姓名：{}", currentName);
        }
        if (parsedInfo.getPhone() != null) {
            currentPhone = parsedInfo.getPhone();
            log.info("【用户信息更新】从当前输入中提取到手机号：{}", currentPhone);
        }
        
        // 2. 查询该会话的最新记录，获取之前保存的信息
        if (currentName == null || currentPhone == null) {
            var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConsultationRecord>()
                    .eq(ConsultationRecord::getSessionId, sessionId)
                    .orderByDesc(ConsultationRecord::getCreatedTime)
                    .last("LIMIT 1");
            
            ConsultationRecord lastRecord = consultationRecordService.getOne(queryWrapper);
            if (lastRecord != null) {
                // 如果当前输入没有提取到，使用之前的值
                if (currentName == null && lastRecord.getUserName() != null) {
                    currentName = lastRecord.getUserName();
                    log.debug("【用户信息更新】使用之前保存的姓名：{}", currentName);
                }
                if (currentPhone == null && lastRecord.getUserPhone() != null) {
                    currentPhone = lastRecord.getUserPhone();
                    log.debug("【用户信息更新】使用之前保存的手机号：{}", currentPhone);
                }
            }
        }
        
        return new String[] { currentName, currentPhone };
    }
    
    /**
     * 获取分类名称
     */
    private String getCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        // TODO: 从缓存或服务中获取分类名称
        return "分类" + categoryId;
    }
    
    /**
     * 构建知识库上下文（用于大模型增强）
     */
    private String buildKnowledgeContext(List<KnowledgeItem> knowledgeItems) {
        if (knowledgeItems == null || knowledgeItems.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(knowledgeItems.size(), 3); i++) {
            KnowledgeItem item = knowledgeItems.get(i);
            sb.append("【相关知识 ").append(i + 1).append("】\n");
            sb.append("标题：").append(item.getTitle()).append("\n");
            sb.append("问题：").append(item.getQuestion()).append("\n");
            sb.append("答案：").append(item.getAnswer()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 计算匹配度分数（与 searchKnowledge 中的算法保持一致）
     */
    private double calculateMatchScore(String question, KnowledgeItem item) {
        // 1. 问题与问题匹配（权重 0.5）
        double questionScore = SimilarityUtil.calculateSimilarity(question, item.getQuestion());

        // 2. 问题与关键词匹配（权重 0.3）
        double keywordScore = 0.0;
        if (StrUtil.isNotBlank(item.getKeywords())) {
            String[] keywords = item.getKeywords().split("[,，;；\\s]+");
            int matchCount = 0;
            for (String kw : keywords) {
                if (question.contains(kw.trim())) {
                    matchCount++;
                }
            }
            keywordScore = (double) matchCount / keywords.length;
        }

        // 3. 问题与标题匹配（权重 0.2）
        double titleScore = 0.0;
        if (StrUtil.isNotBlank(item.getTitle())) {
            titleScore = SimilarityUtil.calculateSimilarity(question, item.getTitle());
        }

        // 4. 数字匹配加成（如果包含数字且匹配）
        double numberBonus = 0.0;
        if (SimilarityUtil.containsKeyNumbers(question, item.getQuestion())) {
            numberBonus = 0.2;
        }

        // 综合计算
        double totalScore = questionScore * 0.5 + keywordScore * 0.3 + titleScore * 0.2 + numberBonus;

        // 5. 浏览量微调（热门问题略微加分）
        double viewBonus = Math.min(item.getViewCount() * 0.001, 0.1);  // 最多加 0.1

        return Math.min(totalScore + viewBonus, 1.0);  // 不超过 1.0
    }
}
