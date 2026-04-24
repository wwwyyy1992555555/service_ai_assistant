package com.myproject.service_ai_assistant.common;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Excel 工具类 - 使用 EasyExcel
 */
@Slf4j
public class ExcelUtil {

    /**
     * 生成知识库导入模板
     * @return Excel 文件的字节数组
     */
    public static byte[] generateKnowledgeTemplate() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // 创建模板数据
            List<List<String>> dataList = new ArrayList<>();
            
            // 添加示例数据行
            dataList.add(Arrays.asList(
                "如何重置密码？",
                "忘记密码怎么办？",
                "您可以通过点击登录页面的'忘记密码'链接，输入注册时的邮箱地址，系统会发送重置密码的邮件到您的邮箱。",
                "密码,重置,忘记",
                "账户管理",
                "已发布",
                "否"
            ));
            
            dataList.add(Arrays.asList(
                "如何联系客服？",
                "客服电话是多少？",
                "您可以拨打客服热线：400-123-4567，服务时间为工作日9:00-18:00。",
                "客服,电话,联系",
                "售后服务",
                "已发布",
                "是"
            ));
            
            // 写入 Excel
            EasyExcel.write(outputStream)
                .head(getKnowledgeHeaders())
                .sheet("知识库导入模板")
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .doWrite(dataList);
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("【生成模板】生成失败", e);
            throw new RuntimeException("生成模板失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取知识库导入表头
     */
    private static List<List<String>> getKnowledgeHeaders() {
        List<List<String>> headers = new ArrayList<>();
        headers.add(Collections.singletonList("标题*"));
        headers.add(Collections.singletonList("问题*"));
        headers.add(Collections.singletonList("答案*"));
        headers.add(Collections.singletonList("关键词"));
        headers.add(Collections.singletonList("分类名称"));
        headers.add(Collections.singletonList("发布状态"));
        headers.add(Collections.singletonList("是否置顶"));
        return headers;
    }

    /**
     * 从 Excel 文件导入知识条目
     * @param inputStream Excel 文件输入流
     * @param tenantId 租户ID
     * @return 导入的知识条目列表
     */
    public static List<Map<String, Object>> importKnowledgeFromExcel(InputStream inputStream, Long tenantId) {
        List<Map<String, Object>> knowledgeList = new ArrayList<>();
        
        try {
            // 使用监听器模式读取，更稳定
            EasyExcel.read(inputStream, new ReadListener<Map<Integer, String>>() {
                private int rowNum = 0;
                
                @Override
                public void invoke(Map<Integer, String> row, AnalysisContext context) {
                    rowNum++;
                    
                    // 注意：EasyExcel 会自动跳过表头行，这里处理的是数据行
                    
                    // 跳过空行
                    if (row == null || row.isEmpty()) {
                        return;
                    }
                    
                    Map<String, Object> knowledge = new HashMap<>();
                    knowledge.put("tenantId", tenantId);
                    
                    // 读取各列数据（0-标题, 1-问题, 2-答案, 3-关键词, 4-分类名称, 5-发布状态, 6-是否置顶）
                    String title = row.get(0);
                    String question = row.get(1);
                    String answer = row.get(2);
                    String keywords = row.get(3);
                    String categoryName = row.get(4);
                    String publishStatusStr = row.get(5);
                    String isTopStr = row.get(6);
                    
                    // 跳过全空的行
                    if (StrUtil.isBlank(title) && StrUtil.isBlank(question) && StrUtil.isBlank(answer)) {
                        return;
                    }
                    
                    knowledge.put("title", title);
                    knowledge.put("question", question);
                    knowledge.put("answer", answer);
                    knowledge.put("keywords", keywords);
                    knowledge.put("categoryName", categoryName);
                    
                    // 处理发布状态
                    if (StrUtil.isNotBlank(publishStatusStr)) {
                        knowledge.put("publishStatus", "已发布".equals(publishStatusStr.trim()) ? 1 : 0);
                    } else {
                        knowledge.put("publishStatus", 1); // 默认已发布
                    }
                    
                    // 处理是否置顶
                    if (StrUtil.isNotBlank(isTopStr)) {
                        knowledge.put("isTop", "是".equals(isTopStr.trim()) ? 1 : 0);
                    } else {
                        knowledge.put("isTop", 0); // 默认不置顶
                    }
                    
                    // 验证必填字段
                    if (StrUtil.isBlank(title) || StrUtil.isBlank(question) || StrUtil.isBlank(answer)) {
                        log.info("【导入 Excel】第 {} 行数据不完整（标题={}, 问题={}, 答案={}），跳过", rowNum, title, question, answer);
                        return;
                    }
                    
                    knowledgeList.add(knowledge);
                }
                
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("【导入 Excel】共读取 {} 行，成功解析 {} 条有效数据", rowNum, knowledgeList.size());
                }
            }).sheet(0).doRead();
            
        } catch (Exception e) {
            log.error("【导入 Excel】解析失败，错误信息: {}", e.getMessage(), e);
            throw new IllegalArgumentException("导入 Excel 文件失败：" + e.getMessage());
        }
        
        return knowledgeList;
    }
}
