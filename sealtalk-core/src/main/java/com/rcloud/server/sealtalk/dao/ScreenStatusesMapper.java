package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.ScreenStatuses;
import org.apache.ibatis.annotations.Param;

public interface ScreenStatusesMapper {
    int upsert(ScreenStatuses record);

    ScreenStatuses selectByOperateIdAndConversationType(@Param("operateId") String operateId, @Param("conversationType") Integer conversationType);
}