package com.myproject.service_ai_assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproject.service_ai_assistant.entity.KnowledgeCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库分类 Mapper 接口
 */
@Mapper
public interface KnowledgeCategoryMapper extends BaseMapper<KnowledgeCategory> {

}
