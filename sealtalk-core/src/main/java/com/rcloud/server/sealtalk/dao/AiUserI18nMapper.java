package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.AiUserI18n;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiUserI18nMapper {

    int insert(AiUserI18n record);

    int batchInsert(@Param("list") List<AiUserI18n> list);

    List<AiUserI18n> selectByAiUserId(@Param("aiUserId") String aiUserId);

    List<AiUserI18n> selectByAiUserIds(@Param("aiUserIds") List<String> aiUserIds);

    int deleteByAiUserId(@Param("aiUserId") String aiUserId);
}


