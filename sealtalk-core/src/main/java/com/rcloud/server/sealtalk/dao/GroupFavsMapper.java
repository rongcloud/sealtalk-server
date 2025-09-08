package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.GroupFavs;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GroupFavsMapper {

    int insert(GroupFavs record);
    int deleteByGroupId(@Param("groupId") Integer groupId);
    int deleteByUserId(@Param("userId") Integer userId);
    int deleteByGroupIdAndUserIds(@Param("groupId") Integer groupId, @Param("userIds") List<Integer> userIds);
    List<GroupFavs> queryGroupFavsWithGroupByUserId(@Param("userId") Integer userId, @Param("limit") Integer limit, @Param("offset") Integer offset);

    Integer queryCountGroupFavsWithGroupByUserId(@Param("userId") Integer userId);
}