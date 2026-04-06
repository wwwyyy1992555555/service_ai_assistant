package com.myproject.service_ai_assistant.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用户信息解析服务
 * 智能识别用户输入中的姓名、手机号等信息
 */
@Slf4j
@Service
public class UserInfoParserService {

    /**
     * 用户信息解析结果
     */
    @Data
    public static class UserInfo {
        private String name;
        private String phone;
        private boolean isQuestion;  // 是否是提问
        private String rawText;      // 原始输入
        
        public UserInfo() {
        }
        
        public UserInfo(String name, String phone) {
            this.name = name;
            this.phone = phone;
            this.isQuestion = false;
        }
        
        public static UserInfo question() {
            UserInfo info = new UserInfo();
            info.isQuestion = true;
            return info;
        }
    }

    // 疑问句检测模式
    private static final List<Pattern> QUESTION_PATTERNS = Arrays.asList(
        Pattern.compile("^(为什么|怎么|如何|什么|哪里|何时|什么时候|谁|多少|吗|呢|么)"),
        Pattern.compile("(^|[^\\u4e00-\\u9fa5])(为什么|怎么|如何|什么|哪里|何时|谁|多少|吗|呢|么)($|[^\\u4e00-\\u9fa5])"),
        Pattern.compile(".*\\?.*"),
        Pattern.compile(".*(办理|处理|解决|弄|搞).*"),
        Pattern.compile(".*(是|在|有).*(吗|么|？|\\?).*"),
        Pattern.compile(".*(哪里|哪儿).*"),
        Pattern.compile(".*(多久|几天|几次).*"),
        Pattern.compile(".*(能不能|可不可以|可以吗).*"),
        Pattern.compile(".*(谁|什么|哪个|哪些).*"),
        Pattern.compile("^([\\u4e00-\\u9fa5]{1,2})(\\?|？|呢|吗|么)$"),
        Pattern.compile(".*(居住证|社保卡|公积金|医保|身份证|户口|护照|证件|证书|卡片).*(怎么|如何|办理|申请|领取|补办|查询).*"),
        Pattern.compile("^(居住证|社保卡|公积金|医保|身份证|户口|护照|证件|证书|卡片).*(呢|吗|啊|呀|哦)$")
    );

    // 手机号检测模式
    private static final List<Pattern> PHONE_PATTERNS = Arrays.asList(
        Pattern.compile("^(1[3-9]\\d{9})$"),
        Pattern.compile("(?:电话|手机|号码|号|联系方式|微信)[：:\\s]?(1[3-9]\\d{9})"),
        Pattern.compile("(?:是|为|1)[：:\\s]?(1[3-9]\\d{9})"),
        Pattern.compile("(1[3-9]\\d{9})(?:号|手机|电话|的)"),
        Pattern.compile("(1[3-9]\\d{2})\\s?(\\d{3,4})\\s?(\\d{4})"),
        Pattern.compile("(1[3-9]\\d{2})-(\\d{3,4})-(\\d{4})")
    );

    // 姓名检测模式
    private static final List<Pattern> NAME_PATTERNS = Arrays.asList(
        Pattern.compile("^(?:叫我|我是|本人是|我叫)([\\u4e00-\\u9fa5]{2,10}|[a-zA-Z\\s·]{2,20})(?:先生|小姐|女士|哥|姐|叔|姨)?$"),
        Pattern.compile("^(?:本人)(?:叫|是)([\\u4e00-\\u9fa5]{2,10}|[a-zA-Z\\s·]{2,20})$"),
        Pattern.compile("^我(?:姓|姓名)([\\u4e00-\\u9fa5]{2,10}|[a-zA-Z\\s·]{2,20})$"),
        Pattern.compile("^(?:称呼|喊我)([\\u4e00-\\u9fa5]{2,10}|[a-zA-Z\\s·]{2,20})$"),
        Pattern.compile("^姓([\\u4e00-\\u9fa5]{1,5}|[a-zA-Z]{1,10})$"),
                
        Pattern.compile("(?:我|本人)(?:姓|叫|是)([\\u4e00-\\u9fa5]{2,10}|[a-zA-Z\\s·]{2,20})"),
        Pattern.compile("(?:贵|免)姓([\\u4e00-\\u9fa5]{1,5}|[a-zA-Z]{1,10})"),
                
        Pattern.compile("^([\\u4e00-\\u9fa5]{2,10}|[a-zA-Z]+)(?:先生|小姐|女士|哥|姐|叔|姨)$"),
        Pattern.compile("([\\u4e00-\\u9fa5]{2,10}|[a-zA-Z]+)(?:先生|小姐|女士|同志)[，,]?"),
        
        Pattern.compile("^小([\\u4e00-\\u9fa5]{1,5}|[a-zA-Z]+)$"),
        Pattern.compile("^老([\\u4e00-\\u9fa5]{1,5}|[a-zA-Z]+)$"),
        Pattern.compile("^([姓姓名])([\\u4e00-\\u9fa5]{1,5}|[a-zA-Z]+)$")
    );

    // 无效词列表
    private static final List<String> INVALID_WORDS = Arrays.asList(
        "你好", "您好", "谢谢", "感谢", "再见", "拜拜", "抱歉", "对不起", "好的", "明白", "知道", "不用了", "没关系",
        "我叫", "我是", "本人是", "称呼我", "喊我", "我姓",
        "怎么", "如何", "什么", "哪里", "何时", "为何", "为啥", "怎样", "怎么样",
        "学校", "公司", "单位", "问题", "办理", "电话", "手机", "地址", "时间", "处理", "解决",
        "居住证", "社保卡", "公积金", "医保", "身份证", "户口", "护照",
        "证件", "证书", "卡片", "文件", "资料", "档案", "证明",
        "号码", "号码是", "手机号", "手机号是", "电话是", "联系方式"
    );

    // 常见复姓
    private static final List<String> COMMON_DOUBLE_SURNAMES = Arrays.asList(
        "欧阳", "司马", "上官", "诸葛", "夏侯", "东方", "皇甫", "尉迟", "公羊",
        "澹台", "公冶", "宗政", "濮阳", "淳于", "太叔", "申屠", "公孙", "仲孙",
        "轩辕", "令狐", "钟离", "宇文", "长孙", "慕容", "鲜于", "闾丘", "司徒",
        "司空", "亓官", "司寇", "仉督", "子车", "颛孙", "端木", "巫马", "公西",
        "漆雕", "乐正", "壤驷", "公良", "拓跋", "夹谷", "宰父", "谷梁", "段干",
        "百里", "东郭", "南门", "呼延", "羊舌", "梁丘", "左丘", "东门", "西门",
        "南宫", "第五", "公仪", "公乘", "公山", "公坚", "公伯", "公仲", "公叔",
        "公祖", "公晰", "公冀", "公夏", "公良", "公绪", "公续", "公访", "公绍",
        "公统", "公绪", "公续", "公访", "公绍", "公统", "长孙", "慕容", "司徒",
        "司空", "申屠", "将闾", "令狐", "徐离", "尉迟", "宇文", "呼延", "东郭",
        "南门", "西门", "南宫", "第五", "羊舌", "梁丘", "左丘", "东门"
    );
    // 常见姓氏（简化显示）
    private static final List<String> COMMON_SURNAMES = Arrays.asList(
        "赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈", "褚", "卫", "蒋", "沈", "韩", "杨", "朱", "秦", "尤", "许", "何", "吕", "施", "张"
    );

    /**
     * 解析用户输入
     * @param text 用户输入文本
     * @return 用户信息解析结果
     */
    public UserInfo parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new UserInfo();
        }

        String cleanText = text.trim()
                .replaceAll("[，,。！？?!]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        log.debug("【用户信息解析】原始文本：{}, 清理后：{}", text, cleanText);

        UserInfo result = new UserInfo();
        result.setRawText(text);

        // 1. 判断是否是提问
        if (isQuestion(cleanText)) {
            log.debug("【用户信息解析】识别为提问");
            return UserInfo.question();
        }

        // 2. 识别手机号
        result.setPhone(extractPhone(cleanText));

        // 3. 识别姓名（如果有手机号，降低识别门槛）
        String name = extractName(cleanText, result.getPhone() != null);
        result.setName(name);

        log.info("【用户信息解析】结果：name={}, phone={}", result.getName(), result.getPhone());

        return result;
    }
    
    /**
     * 提取关键词（用于知识库搜索）
     * @param text 用户输入文本
     * @return 关键词
     */
    public String extractKeyword(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        String cleanText = text.trim()
                .replaceAll("[，,。！？?!]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        
        log.debug("【关键词提取】原始文本：{}, 清理后：{}", text, cleanText);
        
        // 检查是否是礼貌用语
        if (isPoliteGreeting(cleanText)) {
            log.info("【关键词提取】检测到礼貌用语，不提取关键词");
            return null;
        }
        
        // 1. 检查是否包含证件/业务关键词
        for (String keyword : KEYWORD_LIST) {
            if (cleanText.contains(keyword)) {
                log.info("【关键词提取】提取到关键词：{}", keyword);
                return keyword;
            }
        }
        
        // 2. 去除语气词和无意义词
        String refinedText = cleanText
                .replaceAll("(打错了|我想|请问|问一下|咨询)", "")
                .trim();
        
        if (!refinedText.isEmpty() && refinedText.length() <= 10 && !isPoliteGreeting(refinedText)) {
            log.info("【关键词提取】使用精简文本：{}", refinedText);
            return refinedText;
        }
        
        log.info("【关键词提取】未提取到关键信息，返回 null");
        return null;
    }
    
    /**
     * 检查是否是礼貌问候语（增加长度限制，防误判）
     * @param text 清理后的文本
     * @return true-是礼貌用语，false-不是
     */
    private boolean isPoliteGreeting(String text) {
        // 如果输入较长（超过15个字符），说明不只是礼貌用语，可能包含业务问题
        if (text.length() > 15) {
            return false;
        }
        
        if (text.matches(".*(你好|您好|hello|hi|Hi|HI|早上好|中午好|晚上好|晚安).*")) {
            return true;
        }
        
        if (text.matches(".*(谢谢|感谢|谢谢你|感谢您|非常感谢|太感谢了).*")) {
            return true;
        }
        
        if (text.matches(".*(再见|拜拜|bye|Bye|BYE|下次见|回见).*")) {
            return true;
        }
        
        if (text.matches(".*(对不起|抱歉|不好意思|请原谅).*")) {
            return true;
        }
        
        if (text.matches(".*(好的|好的谢谢|明白了|知道了|了解|懂了|ok|OK|Ok).*")) {
            return true;
        }
        
        if (text.matches(".*(不用了|不需要|算了|没关系|没事).*")) {
            return true;
        }
        
        return false;
    }

    // 关键词列表（优先匹配）
    private static final List<String> KEYWORD_LIST = Arrays.asList(
        "居住证", "社保卡", "公积金", "医保", "身份证", "户口", "护照",
        "证件", "证书", "卡片", "文件", "资料", "档案", "证明",
        "办理", "申请", "领取", "补办", "查询", "注销", "变更",
        "学校", "公司", "单位", "地址", "电话", "手机", "时间"
    );

    /**
     * 判断是否是提问
     */
    private boolean isQuestion(String text) {
        for (Pattern pattern : QUESTION_PATTERNS) {
            if (pattern.matcher(text).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 提取手机号
     */
    private String extractPhone(String text) {
        for (Pattern pattern : PHONE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                if (matcher.groupCount() >= 3) {
                    return matcher.group(1) + matcher.group(2) + matcher.group(3);
                } else {
                    return matcher.group(1) != null ? matcher.group(1) : matcher.group(0);
                }
            }
        }
        return null;
    }

    /**
     * 提取姓名（严格版 - 支持中文、英文、少数民族文字）
     * @param text 清理后的文本
     * @param hasPhone 是否同时包含手机号
     * @return 姓名
     */
    private String extractName(String text, boolean hasPhone) {
        if (hasPhone) {
            String chineseOnly = text.replaceAll("[^\\u4e00-\\u9fa5]", "").trim();
            
            if (chineseOnly.length() >= 2 && chineseOnly.length() <= 10) {
                if (isValidName(chineseOnly)) {
                    log.debug("【姓名识别】从混合文本中提取中文姓名：{}", chineseOnly);
                    return chineseOnly;
                }
                
                // 尝试从长文本中提取 2-10 个字的姓名
                for (int len = 2; len <= Math.min(10, chineseOnly.length()); len++) {
                    // 从前缀开始尝试
                    String prefix = chineseOnly.substring(0, len);
                    if (isValidName(prefix)) {
                        log.debug("【姓名识别】从混合文本中提取中文姓名（前缀{}字）：{}", len, prefix);
                        return prefix;
                    }
                    
                    // 从后缀开始尝试
                    if (chineseOnly.length() >= len) {
                        String suffix = chineseOnly.substring(chineseOnly.length() - len);
                        if (isValidName(suffix)) {
                            log.debug("【姓名识别】从混合文本中提取中文姓名（后缀{}字）：{}", len, suffix);
                            return suffix;
                        }
                    }
                }
            }
            
            String englishOnly = text.replaceAll("[^a-zA-Z]", "").trim();
            if (englishOnly.length() >= 2 && englishOnly.length() <= 20) {
                if (englishOnly.matches("^[a-zA-Z]+$")) {
                    log.debug("【姓名识别】从混合文本中提取英文姓名：{}", englishOnly);
                    return englishOnly;
                }
            }
            
            String mixedText = text.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z·]", "").trim();
            if (mixedText.length() >= 2 && mixedText.length() <= 20) {
                if (isValidName(mixedText)) {
                    log.debug("【姓名识别】从混合文本中提取混合姓名：{}", mixedText);
                    return mixedText;
                }
            }
        }
        
        for (int i = 0; i < NAME_PATTERNS.size(); i++) {
            Pattern pattern = NAME_PATTERNS.get(i);
            Matcher matcher = pattern.matcher(text);
            
            if (matcher.find()) {
                String name = matcher.groupCount() >= 2 ? matcher.group(2) : matcher.group(1);
                
                if (i == 5 || i == 6) {
                    name = matcher.group(1);
                } else if (i == 8 || i == 9) {
                    name = matcher.group(1);
                }
                
                name = name.replaceFirst("^[姓姓名]", "");
                
                if (isValidName(name)) {
                    log.debug("【姓名识别】匹配到姓名：{} (模式索引：{})", name, i);
                    return name;
                }
            }
        }
        
        return null;
    }

    /**
     * 验证姓名是否有效（优化版 - 支持多民族和外国人）
     */
    private boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        int length = name.length();

        if (length == 1) {
            return COMMON_SURNAMES.contains(name);
        }
        
        if (length == 2) {
            if (INVALID_WORDS.stream().anyMatch(name::contains)) {
                return false;
            }
            if (COMMON_DOUBLE_SURNAMES.contains(name)) {
                return true;
            }
            String surname = name.substring(0, 1);
            if (COMMON_SURNAMES.contains(surname)) {
                return true;
            }
            String secondChar = name.substring(1, 2);
            if (!COMMON_SURNAMES.contains(secondChar)) {
                return true;
            }
            return false;
        }
        
        if (length == 3) {
            if (INVALID_WORDS.stream().anyMatch(name::contains)) {
                return false;
            }
            String surname = name.substring(0, 1);
            if (COMMON_SURNAMES.contains(surname)) {
                return true;
            }
            String secondChar = name.substring(1, 2);
            String thirdChar = name.substring(2, 3);
            if (!COMMON_SURNAMES.contains(secondChar) && !COMMON_SURNAMES.contains(thirdChar)) {
                return true;
            }
            return false;
        }
        
        if (length >= 4 && length <= 10) {
            if (INVALID_WORDS.stream().anyMatch(name::contains)) {
                return false;
            }
            
            String doubleSurname = name.substring(0, 2);
            if (COMMON_DOUBLE_SURNAMES.contains(doubleSurname)) {
                return true;
            }
            
            String surname = name.substring(0, 1);
            if (COMMON_SURNAMES.contains(surname)) {
                return true;
            }
            
            return true;
        }
        
        return false;
    }
}
