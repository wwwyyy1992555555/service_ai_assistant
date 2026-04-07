package com.myproject.service_ai_assistant.controller;

import cn.hutool.core.util.StrUtil;
import com.myproject.service_ai_assistant.annotation.RequireRole;
import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.common.LevelCode;
import com.myproject.service_ai_assistant.common.SimilarityUtil;
import com.myproject.service_ai_assistant.config.LlmConfig;
import com.myproject.service_ai_assistant.dto.ConsultRequest;
import com.myproject.service_ai_assistant.dto.ConsultResponse;
import com.myproject.service_ai_assistant.dto.UserInfoParseRequest;
import com.myproject.service_ai_assistant.entity.ConsultationRecord;
import com.myproject.service_ai_assistant.entity.KnowledgeItem;
import com.myproject.service_ai_assistant.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    private TenantConfigService tenantConfigService;

    @PostMapping("/ask")
    @Operation(summary = "智能问答 - 用户提问", description = "用户提出问题，系统从知识库中匹配答案并返回")
    public Result<ConsultResponse> ask(@Valid @RequestBody ConsultRequest request) {
        log.info("【智能问答】收到咨询请求：sessionId={}, tenantId={}, question={}", 
                request.getSessionId(), request.getTenantId(), request.getQuestion());

        String question = request.getQuestion();
        String answer;
        Long matchedKnowledgeId = null;
        Double matchScore = 0.0;
        String knowledgeTitle = null;
        String categoryName = null;
        Integer viewCount = 0;

        // ========== 第1步：礼貌用语拦截（快速响应）==========
        String politeResponse = generatePoliteResponse(question);
        if (politeResponse != null) {
            log.info("【智能问答】礼貌用语拦截");
            answer = politeResponse;
            
            // 仍需提取用户信息
            String[] userInfo = parseUserInfo(question, request.getSessionId());
            String currentName = userInfo[0];
            String currentPhone = userInfo[1];
            
            // 保存对话记录
            ConsultationRecord record = saveConsultationRecord(request, answer, null, 0.0, null, null, 0);
            record.setUserName(currentName);
            record.setUserPhone(currentPhone);
            consultationRecordService.updateById(record);
            
            return buildResponse(answer, null, 0.0, null, null, 0, null, record.getId());
        }

        // ========== 第2步：用户信息提取与历史比对 ==========
        String[] userInfo = parseUserInfo(question, request.getSessionId());
        String currentName = userInfo[0];
        String currentPhone = userInfo[1];
        log.info("【用户信息】姓名：{}, 手机号：{}", currentName, currentPhone);

        // 获取历史信息用于智能回复
        String lastSavedName = null;
        String lastSavedPhone = null;
        boolean isInfoDuplicate = false;
        
        try {
            var historyWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConsultationRecord>()
                    .eq(ConsultationRecord::getSessionId, request.getSessionId())
                    .isNotNull(ConsultationRecord::getUserName)
                    .or()
                    .isNotNull(ConsultationRecord::getUserPhone)
                    .orderByDesc(ConsultationRecord::getCreatedTime)
                    .last("LIMIT 1");
            
            ConsultationRecord lastRecord = consultationRecordService.getOne(historyWrapper);
            if (lastRecord != null) {
                lastSavedName = lastRecord.getUserName();
                lastSavedPhone = lastRecord.getUserPhone();
                
                // 判断是否是重复提交：当前提取的信息与历史记录完全一致
                if (currentName != null && currentName.equals(lastSavedName) &&
                    currentPhone != null && currentPhone.equals(lastSavedPhone)) {
                    isInfoDuplicate = true;
                    log.info("【智能判断】检测到用户重复提交相同信息");
                }
            }
        } catch (Exception e) {
            log.debug("【智能判断】获取历史记录失败：{}", e.getMessage());
        }

        // ========== 第3步：知识库搜索 ==========
        List<KnowledgeItem> matchedKnowledge = knowledgeItemService.searchKnowledge(
                request.getTenantId(), 
                question
        );
        log.debug("【智能问答】匹配到 {} 条知识", matchedKnowledge.size());

        if (!matchedKnowledge.isEmpty()) {
            // 找到匹配的知识，直接使用知识库答案
            KnowledgeItem item = matchedKnowledge.get(0);
            
            matchScore = calculateMatchScore(question, item);
            log.info("【智能问答】匹配度：{}", matchScore);
            
            answer = item.getAnswer();
            matchedKnowledgeId = item.getId();
            knowledgeTitle = item.getTitle();
            categoryName = getCategoryName(item.getCategoryId());
            viewCount = item.getViewCount() + 1;
            
            log.info("【智能问答】使用知识库答案：id={}, title={}", item.getId(), item.getTitle());
            
            // 增加浏览次数
            item.setViewCount(viewCount);
            knowledgeItemService.updateById(item);
        } else {
            // ========== 第4步：AI 兜底（知识库无结果）==========
            log.info("【智能问答】知识库无结果，AI兜底");
            
            try {
                if (llmConfig.isEnabled()) {
                    // 检测是否需要联网搜索
                    if (needWebSearch(question)) {
                        log.info("【智能问答】联网搜索");
                        String historyContext = getHistoryContext(request.getSessionId(), 3);
                        answer = llmService.generateResponseWithWebSearch(question, historyContext);
                    } else {
                        // 检测是否需要加载上下文
                        String historyContext = null;
                        if (needHistoryContext(question)) {
                            log.info("【智能问答】加载历史上下文");
                            historyContext = getHistoryContext(request.getSessionId(), 3);
                        }
                        log.info("【智能问答】大模型生成回复");
                        answer = llmService.generateResponseWithContext(question, historyContext);
                    }
                } else {
                    // LLM未启用，走智能模板
                    log.info("【智能问答】LLM未启用，使用智能模板");
                    answer = generateSmartResponse(question, currentName, currentPhone, isInfoDuplicate);
                }
            } catch (Exception e) {
                // ========== 第5步：异常兜底（LLM异常）==========
                log.error("【智能问答】LLM调用异常，使用智能模板兜底：{}", e.getMessage());
                answer = generateSmartResponse(question, currentName, currentPhone, isInfoDuplicate);
            }
        }

        // ========== 保存对话记录 ==========
        ConsultationRecord record = saveConsultationRecord(
                request, answer, matchedKnowledgeId, matchScore, 
                knowledgeTitle, categoryName, matchedKnowledge.isEmpty() ? 0 : viewCount
        );
        record.setUserName(currentName);
        record.setUserPhone(currentPhone);
        consultationRecordService.updateById(record);

        // ========== 检查 AI 返回的用户信息标记 ==========
        if (answer.contains("[INFO_COLLECTED]")) {
            String llmName = extractFieldFromTag(answer, "姓名:");
            String llmPhone = extractFieldFromTag(answer, "手机:");
            
            if (llmName != null || llmPhone != null) {
                if (llmName != null) {
                    record.setUserName(llmName);
                    log.info("【AI信息提取】姓名：{}", llmName);
                }
                if (llmPhone != null) {
                    record.setUserPhone(llmPhone);
                    log.info("【AI信息提取】手机号：{}", llmPhone);
                }
                
                // 移除标记并同步更新记录对象，确保数据库不存储标记
                answer = answer.replaceAll("\\[INFO_COLLECTED\\].*?\\[/INFO_COLLECTED\\]", "").trim();
                record.setAnswer(answer);
                consultationRecordService.updateById(record);
            }
        }

        // ========== 返回结果 ==========
        return buildResponse(answer, matchedKnowledgeId, matchScore, knowledgeTitle, 
                categoryName, viewCount, matchedKnowledge, record.getId());
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
    @RequireRole(minLevel = LevelCode.ROLE_LEVEL_ADMIN)
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
    @RequireRole(minLevel = LevelCode.ROLE_LEVEL_ADMIN)
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
    @RequireRole(minLevel = LevelCode.ROLE_LEVEL_ADMIN)
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
    private String generateSmartResponse(String question, String userName, String userPhone, boolean isInfoDuplicate) {
        // 1. 如果检测到用户重复提交了相同的信息，给予简短确认
        if (isInfoDuplicate) {
            return "您的信息我们已经记下了，请问还有其他可以帮您的吗？😊";
        }
        
        // 从系统配置中读取服务热线和工作时间
        String servicePhone = "12345";
        String serviceTime = "工作时间：周一至周日 9:00-17:00";
        
        try {
            var config = tenantConfigService.getConfig(1L);
            if (config != null) {
                servicePhone = config.getServicePhone() != null && !config.getServicePhone().isEmpty() ? config.getServicePhone() : "12345";
                serviceTime = config.getServiceTime() != null && !config.getServiceTime().isEmpty() ? config.getServiceTime() : "工作时间：周一至周日 9:00-17:00";
            }
        } catch (Exception e) {
            log.warn("【获取租户配置失败】使用默认值，error={}", e.getMessage());
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
        
        // 如果输入较长（超过10个字符），说明不只是礼貌用语，可能包含业务问题
        // 跳过礼貌用语处理，继续走知识库搜索或 AI 回复流程
        if (cleanText.length() > 10) {
            return null;
        }
        
        // === 问候语 ===
        if (cleanText.matches(".*(你好|您好|hello|hi|Hi|HI|早上好|中午好|晚上好|晚安).*")) {
            return "您好！👋 很高兴为您服务！请问有什么可以帮您？";
        }
        
        // === 感谢语 ===
        if (cleanText.matches(".*(谢谢|感谢|谢谢你|感谢您|非常感谢|太感谢了).*")) {
            return "不客气！😊 这是我应该做的，还有其他问题吗？";
        }
        
        // === 再见语 ===
        if (cleanText.matches(".*(再见|拜拜|bye|Bye|BYE|下次见|回见).*")) {
            return "再见！祝您生活愉快！👋";
        }
                
        // === 道歉语 ===
        if (cleanText.matches(".*(对不起|抱歉|不好意思|请原谅).*")) {
            return "没关系的！😊 请问有什么可以帮您？";
        }
                
        // === 肯定语 ===
        if (cleanText.matches(".*(好的|好的谢谢|明白了|知道了|了解|懂了|ok|OK|Ok).*")) {
            return "好的！😊 如果有其他问题，随时都可以问我哦~";
        }
                
        // === 否定语 ===
        if (cleanText.matches(".*(不用了|不需要|算了|没关系|没事).*")) {
            return "好的，如果您有其他问题，欢迎随时咨询！😊";
        }
        
        return null;  // 不是礼貌用语，返回 null 继续后续处理
    }
    
    /**
     * 获取历史对话上下文（用于 AI 记忆）
     * @param sessionId 会话 ID
     * @param limit 最近 N 轮对话
     * @return 格式化的历史对话字符串
     */
    private String getHistoryContext(String sessionId, int limit) {
        if (sessionId == null || sessionId.isEmpty()) {
            return "";
        }
        
        try {
            var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConsultationRecord>()
                    .eq(ConsultationRecord::getSessionId, sessionId)
                    .orderByDesc(ConsultationRecord::getCreatedTime)
                    .last("LIMIT " + limit);
            
            List<ConsultationRecord> records = consultationRecordService.list(queryWrapper);
            if (records.isEmpty()) {
                return "";
            }
            
            // 反转列表，按时间正序排列
            java.util.Collections.reverse(records);
            
            StringBuilder sb = new StringBuilder();
            for (ConsultationRecord record : records) {
                sb.append("用户：").append(record.getQuestion()).append("\n");
                sb.append("AI：").append(record.getAnswer()).append("\n\n");
            }
            
            log.debug("【历史上下文】加载 {} 条历史对话", records.size());
            return sb.toString();
            
        } catch (Exception e) {
            log.warn("【获取历史上下文】失败：{}", e.getMessage());
            return "";
        }
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
        
        // 过滤占位符数据，防止浪费数据库空间
        currentName = filterPlaceholder(currentName);
        currentPhone = filterPlaceholder(currentPhone);
        
        return new String[] { currentName, currentPhone };
    }
    
    /**
     * 过滤示例/占位符数据（防止浪费数据库空间）
     * @param value 待过滤的值
     * @return 过滤后的值（如果是占位符则返回 null）
     */
    private String filterPlaceholder(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = value.trim();
        
        // 过滤姓名占位符：XXX、某某、测试、test 等
        if (trimmed.matches("(?i)^(xxx|test|demo|测试|示例|某某|某人|无名|匿名|unknown)$")) {
            log.debug("【过滤占位符】姓名占位符：{}", trimmed);
            return null;
        }
        
        // 过滤电话占位符：138XXXXXXXX、13800000000、13812345678 等规律数字
        if (trimmed.matches("^1[3-9]\\d{9}$")) {
            // 检查是否是规律数字（占位符）
            String digits = trimmed.replaceAll("\\D", "");
            // 全相同数字：13800000000
            if (digits.matches("^1[3-9]0{9}$")) {
                log.debug("【过滤占位符】电话占位符（全0）：{}", trimmed);
                return null;
            }
            // 连续递增/递减：13812345678
            if (digits.matches("^1[3-9]12345678$")) {
                log.debug("【过滤占位符】电话占位符（递增）：{}", trimmed);
                return null;
            }
            // X 占位：138XXXXXXXX
            if (trimmed.toUpperCase().contains("X")) {
                log.debug("【过滤占位符】电话占位符（X占位）：{}", trimmed);
                return null;
            }
        }
        
        return value;
    }
    
    /**
     * 检测是否需要联网搜索
     * @param question 用户问题
     * @return true=需要联网，false=不需要
     */
    private boolean needWebSearch(String question) {
        // 检查功能是否启用
        if (!llmConfig.isWebSearchEnabled()) {
            return false;
        }
        
        if (question == null || question.trim().isEmpty()) {
            return false;
        }
        
        // 从配置中读取联网搜索触发关键词
        for (String keyword : llmConfig.getWebSearchKeywords()) {
            if (question.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检测是否需要加载历史上下文（追问场景）
     * @param question 用户问题
     * @return true=需要上下文，false=不需要
     */
    private boolean needHistoryContext(String question) {
        // 检查功能是否启用
        if (!llmConfig.isHistoryContextEnabled()) {
            return false;
        }
        
        if (question == null || question.trim().isEmpty()) {
            return false;
        }
        
        // 从配置中读取追问触发关键词
        for (String keyword : llmConfig.getFollowUpKeywords()) {
            if (question.contains(keyword)) {
                return true;
            }
        }
        
        // 问题过短（可能是追问）
        if (question.length() <= llmConfig.getShortQuestionLength()) {
            return true;
        }
        
        return false;
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
    
    /**
     * 保存对话记录
     */
    private ConsultationRecord saveConsultationRecord(ConsultRequest request, String answer, 
            Long matchedKnowledgeId, Double matchScore, String knowledgeTitle, 
            String categoryName, Integer viewCount) {
        ConsultationRecord record = new ConsultationRecord();
        record.setTenantId(request.getTenantId());
        record.setSessionId(request.getSessionId());
        record.setUserId(request.getUserId());
        record.setQuestion(request.getQuestion());
        record.setAnswer(answer);
        record.setMatchedKnowledgeId(matchedKnowledgeId);
        record.setMatchScore(matchScore);
        record.setIsSolved(matchedKnowledgeId != null ? 1 : 0);
        record.setDeviceType(request.getDeviceType());
        record.setIpAddress(request.getIpAddress());
        
        consultationRecordService.save(record);
        log.info("【智能问答】保存对话记录：recordId={}", record.getId());
        return record;
    }
    
    /**
     * 构建响应对象
     */
    private Result<ConsultResponse> buildResponse(String answer, Long matchedKnowledgeId, 
            Double matchScore, String knowledgeTitle, String categoryName, 
            Integer viewCount, List<KnowledgeItem> matchedKnowledge, Long consultationId) {
        ConsultResponse response = new ConsultResponse();
        response.setAnswer(answer);
        response.setMatchedKnowledgeId(matchedKnowledgeId);
        response.setMatchScore(matchScore);
        response.setKnowledgeTitle(knowledgeTitle);
        response.setCategoryName(categoryName);
        response.setViewCount(viewCount);
        response.setConsultationId(consultationId);
        
        if (matchedKnowledge != null && !matchedKnowledge.isEmpty()) {
            response.setSuggestedQuestions(matchedKnowledge.stream()
                    .skip(1)
                    .limit(3)
                    .map(KnowledgeItem::getQuestion)
                    .toList());
        }
        
        log.info("【智能问答】返回答案：answerLength={}, consultationId={}", answer.length(), consultationId);
        return Result.success(response);
    }
    
    /**
     * 从标记中提取字段值
     * 例如：从 "[INFO_COLLECTED]姓名:张三,手机:13800138000[/INFO_COLLECTED]" 中提取 "姓名:" 后面的值
     */
    private String extractFieldFromTag(String text, String fieldName) {
        try {
            // 找到标记内容
            int startIdx = text.indexOf("[INFO_COLLECTED]");
            int endIdx = text.indexOf("[/INFO_COLLECTED]");
            
            if (startIdx == -1 || endIdx == -1) {
                return null;
            }
            
            // 提取标记内的内容
            String content = text.substring(startIdx + 14, endIdx);
            
            // 查找字段
            int fieldIdx = content.indexOf(fieldName);
            if (fieldIdx == -1) {
                return null;
            }
            
            // 提取字段值（从字段名后到逗号或结束）
            int valueStart = fieldIdx + fieldName.length();
            int valueEnd = content.indexOf(",", valueStart);
            if (valueEnd == -1) {
                valueEnd = content.length();
            }
            
            String value = content.substring(valueStart, valueEnd).trim();
            return value.isEmpty() ? null : value;
            
        } catch (Exception e) {
            log.warn("【提取标记字段】失败：{}", e.getMessage());
            return null;
        }
    }
}
