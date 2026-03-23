package com.myproject.service_ai_assistant.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.service_ai_assistant.entity.ConsultationRecord;
import com.myproject.service_ai_assistant.mapper.ConsultationRecordMapper;
import com.myproject.service_ai_assistant.service.ConsultationRecordService;
import org.springframework.stereotype.Service;

/**
 * 咨询对话记录服务实现类
 */
@Service
public class ConsultationRecordServiceImpl extends ServiceImpl<ConsultationRecordMapper, ConsultationRecord> implements ConsultationRecordService {

}
