package com.myproject.service_ai_assistant.controller;

import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.entity.IndustryType;
import com.myproject.service_ai_assistant.service.IndustryTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 行业类型控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/industry-type")
@Tag(name = "行业类型管理")
public class IndustryTypeController {

    @Autowired
    private IndustryTypeService industryTypeService;

    /**
     * 获取所有启用的行业类型列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取行业类型列表")
    public Result<List<IndustryType>> getIndustryTypeList() {
        List<IndustryType> types = industryTypeService.getActiveTypes();
        return Result.success(types);
    }
}
