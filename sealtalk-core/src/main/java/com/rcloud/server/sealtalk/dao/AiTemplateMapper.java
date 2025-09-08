package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.AiTemplate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiTemplateMapper {

    int insert(AiTemplate record);

    int batchInsert(@Param("list") List<AiTemplate> records);

    int updateByTemplateId(AiTemplate record);

    int updateByTemplateIdAndLanguage(AiTemplate record);

    int deleteByTemplateId(@Param("templateId") String templateId);

    List<AiTemplate> selectByTemplateId(@Param("templateId") String templateId);

    AiTemplate selectByTemplateIdAndLanguage(@Param("templateId") String templateId,
                                             @Param("language") String language);

    List<AiTemplate> pageQuery(@Param("search") String search,
                               @Param("limit") int limit);

    int count(@Param("search") String search);

    List<AiTemplate> selectByTemplateIds(@Param("templateIds") List<String> templateIds);
}


