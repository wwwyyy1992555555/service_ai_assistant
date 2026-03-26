package com.myproject.service_ai_assistant.common;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 文本相似度计算工具类
 */
public class SimilarityUtil {

    /**
     * 计算两个文本的综合相似度（严格版本）
     * @param text1 文本 1
     * @param text2 文本 2
     * @return 相似度 (0-1)
     */
    public static double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        // 1. 完全匹配
        if (text1.equals(text2)) {
            return 1.0;
        }

        // 2. 检查是否包含核心关键词（必要条件）
        if (!hasCommonKeywords(text1, text2)) {
            return 0.0;  // 没有共同关键词，直接返回 0
        }

        // 3. 包含匹配（权重 0.7）
        double containScore = calculateContainScore(text1, text2);

        // 4. 关键词匹配（权重 0.3）
        double keywordScore = calculateKeywordScore(text1, text2);

        // 5. 严格模式下，不再使用编辑距离（太宽松）
        // double editScore = calculateEditDistanceScore(text1, text2);

        // 加权平均
        double totalScore = containScore * 0.7 + keywordScore * 0.3;

        // 6. 惩罚项：长度差异过大时降低分数
        double lengthPenalty = calculateLengthPenalty(text1, text2);
        totalScore *= lengthPenalty;

        // 保留 2 位小数
        return Math.round(totalScore * 100.0) / 100.0;
    }

    /**
     * 检查是否包含共同关键词
     */
    private static boolean hasCommonKeywords(String text1, String text2) {
        Set<String> keywords1 = extractKeywords(text1);
        Set<String> keywords2 = extractKeywords(text2);
        
        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            // 如果一方没有关键词，检查是否包含数字
            return containsKeyNumbers(text1, text2);
        }
        
        // 检查是否有交集
        for (String kw1 : keywords1) {
            for (String kw2 : keywords2) {
                // 完全相同或相互包含
                if (kw1.equals(kw2) || kw1.contains(kw2) || kw2.contains(kw1)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * 计算长度差异惩罚系数
     */
    private static double calculateLengthPenalty(String text1, String text2) {
        int len1 = text1.length();
        int len2 = text2.length();
        
        if (len1 == 0 || len2 == 0) {
            return 0.0;
        }
        
        double ratio = (double) Math.min(len1, len2) / Math.max(len1, len2);
        
        // 长度差异超过 3 倍，开始惩罚
        if (ratio < 0.33) {
            return 0.5;  // 严重不匹配
        } else if (ratio < 0.5) {
            return 0.7;  // 中等不匹配
        }
        
        return 1.0;  // 正常匹配
    }

    /**
     * 计算包含匹配得分
     */
    private static double calculateContainScore(String text1, String text2) {
        String t1 = text1.toLowerCase();
        String t2 = text2.toLowerCase();

        if (t1.contains(t2) || t2.contains(t1)) {
            return 1.0;
        }

        // 部分包含
        String[] words1 = splitWords(t1);
        String[] words2 = splitWords(t2);

        int matchCount = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.contains(word2) || word2.contains(word1)) {
                    matchCount++;
                    break;
                }
            }
        }

        return words1.length > 0 ? (double) matchCount / words1.length : 0.0;
    }

    /**
     * 计算关键词匹配得分
     */
    private static double calculateKeywordScore(String text1, String text2) {
        // 提取问题关键词
        Set<String> keywords1 = extractKeywords(text1);
        Set<String> keywords2 = extractKeywords(text2);

        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }

        // 计算交集
        Set<String> intersection = new HashSet<>(keywords1);
        intersection.retainAll(keywords2);

        // 计算并集
        Set<String> union = new HashSet<>(keywords1);
        union.addAll(keywords2);

        // Jaccard 相似度
        return (double) intersection.size() / union.size();
    }

    /**
     * 计算编辑距离得分
     */
    private static double calculateEditDistanceScore(String text1, String text2) {
        int distance = calculateLevenshteinDistance(text1, text2);
        int maxLen = Math.max(text1.length(), text2.length());
        
        if (maxLen == 0) {
            return 1.0;
        }

        return 1.0 - (double) distance / maxLen;
    }

    /**
     * 计算编辑距离（Levenshtein Distance）
     */
    private static int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * 分词（简单按标点和空格分割）
     */
    private static String[] splitWords(String text) {
        // 移除标点符号
        text = text.replaceAll("[^\\w\\s\\u4e00-\\u9fa5]", " ");
        return text.trim().split("\\s+");
    }

    /**
     * 提取关键词（严格版本）
     */
    private static Set<String> extractKeywords(String text) {
        // 中文停用词表（更严格）
        Set<String> stopwords = new HashSet<>(Arrays.asList(
            // 助词、介词、连词
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
            "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
            "你", "会", "着", "没有", "看", "好", "自己", "这", "那", "他",
            "她", "它", "们", "这个", "那个", "什么", "怎么", "如何",
            "怎样", "哪里", "哪儿", "何时", "多少", "为啥", "为什么",
            "吗", "呢", "吧", "啊", "呀", "哦", "呃", "啦",
            "请问", "问一下", "想了解", "想知道", "咨询", "问"
        ));

        Set<String> keywords = new HashSet<>();
        String[] words = splitWords(text);

        for (String word : words) {
            // 更严格：至少 2 个字符，且不是停用词
            if (word.length() >= 2 && !stopwords.contains(word)) {
                keywords.add(word);
            }
        }
        
        // 如果提取不到关键词，尝试提取名词性短语（简单处理：保留所有 2 字以上词）
        if (keywords.isEmpty() && words.length > 0) {
            for (String word : words) {
                if (word.length() >= 2) {
                    keywords.add(word);
                }
            }
        }

        return keywords;
    }

    /**
     * 判断是否包含关键数字（如电话号码、身份证号的匹配）
     */
    public static boolean containsKeyNumbers(String text1, String text2) {
        // 提取数字
        String numbers1 = extractNumbers(text1);
        String numbers2 = extractNumbers(text2);

        return numbers1.equals(numbers2) || numbers1.contains(numbers2) || numbers2.contains(numbers1);
    }

    /**
     * 提取文本中的数字
     */
    private static String extractNumbers(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
