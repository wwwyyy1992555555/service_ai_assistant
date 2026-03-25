package com.myproject.service_ai_assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproject.service_ai_assistant.entity.ConsultationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 咨询对话记录 Mapper 接口
 */
@Mapper
public interface ConsultationRecordMapper extends BaseMapper<ConsultationRecord> {

    /**
     * 按会话分组查询对话记录（只显示最新的一条）
     */
    List<ConsultationRecord> queryRecordsBySession(@Param("tenantId") Long tenantId, 
                                                    @Param("offset") long offset, 
                                                    @Param("limit") long limit);

    /**
     * 统计会话总数
     */
    long countSessions(@Param("tenantId") Long tenantId);

    /**
     * 搜索按会话分组的对话记录
     */
    List<ConsultationRecord> searchRecordsBySession(@Param("tenantId") Long tenantId,
                                                     @Param("keyword1") String keyword1,
                                                     @Param("keyword2") String keyword2,
                                                     @Param("offset") long offset,
                                                     @Param("limit") long limit);

    /**
     * 统计搜索符合条件的会话总数
     */
    long countSearchedSessions(@Param("tenantId") Long tenantId,
                               @Param("keyword1") String keyword1,
                               @Param("keyword2") String keyword2);
}
