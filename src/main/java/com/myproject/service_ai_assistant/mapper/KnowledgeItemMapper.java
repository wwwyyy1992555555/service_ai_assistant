package com.myproject.service_ai_assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproject.service_ai_assistant.entity.KnowledgeItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识条目 Mapper 接口
 */
@Mapper
public interface KnowledgeItemMapper extends BaseMapper<KnowledgeItem> {

}
