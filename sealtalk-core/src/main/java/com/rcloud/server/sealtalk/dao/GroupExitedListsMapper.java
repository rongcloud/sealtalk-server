package com.rcloud.server.sealtalk.dao;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import com.rcloud.server.sealtalk.entity.GroupExitedLists;


public interface GroupExitedListsMapper {
    int insertBatch(@Param("list") List<GroupExitedLists> list);
    int deleteByGroupIdAndQuitUserIds(@Param("groupId") Integer groupId, @Param("quitUserIds") List<Integer> quitUserIds);
    int deleteByGroupId(@Param("groupId") Integer groupId);
    List<GroupExitedLists> selectByGroupId(@Param("groupId") Integer groupId);
}