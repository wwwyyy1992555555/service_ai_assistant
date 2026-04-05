package com.myproject.service_ai_assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.service_ai_assistant.entity.IndustryType;
import com.myproject.service_ai_assistant.mapper.IndustryTypeMapper;
import com.myproject.service_ai_assistant.service.IndustryTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 行业类型 Service 实现类
 */
@Slf4j
@Service
public class IndustryTypeServiceImpl extends ServiceImpl<IndustryTypeMapper, IndustryType> implements IndustryTypeService {

    @Override
    public List<IndustryType> getActiveTypes() {
        LambdaQueryWrapper<IndustryType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IndustryType::getStatus, 1)
               .orderByAsc(IndustryType::getSortOrder);
        return this.list(wrapper);
    }
}
