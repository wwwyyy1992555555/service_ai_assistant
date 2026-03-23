package com.myproject.service_ai_assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproject.service_ai_assistant.entity.ConsultationRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 咨询对话记录 Mapper 接口
 */
@Mapper
public interface ConsultationRecordMapper extends BaseMapper<ConsultationRecord> {

}
