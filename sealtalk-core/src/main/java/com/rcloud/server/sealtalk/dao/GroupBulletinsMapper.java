package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.GroupBulletins;
import org.apache.ibatis.annotations.Param;

public interface GroupBulletinsMapper {
    int insert(GroupBulletins record);
    GroupBulletins getLastestGroupBulletin(@Param("groupId") Integer groupId);
    int deleteByGroupId(@Param("groupId") Integer groupId);
}