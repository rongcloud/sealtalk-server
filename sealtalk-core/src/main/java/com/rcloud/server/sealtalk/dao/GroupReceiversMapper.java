package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.GroupReceivers;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GroupReceiversMapper {


    int insertBatch(@Param("groupReceiverList") List<GroupReceivers> groupReceiverList);

    List<GroupReceivers> selectGroupReceiversWithIncludes(@Param("userId") Integer userId);

    int deleteByGroupIdAndUserIds(@Param("groupId") Integer groupId, @Param("userIds") List<Integer> userIds);

    int deleteByGroupIdAndUserIdsAndReceiverIds(@Param("groupId") Integer groupId, @Param("userIds") List<Integer> userIds, @Param("receiverIds") List<Integer> receiverIds);

    GroupReceivers selectLatest(@Param("userId") Integer userId, @Param("groupId") Integer groupId, @Param("receiverId") Integer receiverId, @Param("type") Integer type, @Param("status") Integer status);

    int updateStatus(@Param("groupId") Integer groupId, @Param("receiverId") Integer receiverId, @Param("type") Integer type, @Param("currentStatus") Integer currentStatus, @Param("newStatus") Integer newStatus);

    int updateStatusByReceiverIds(@Param("groupId") Integer groupId, @Param("receiverIds") List<Integer> receiverIds, @Param("currentStatus") Integer currentStatus, @Param("newStatus") Integer newStatus);

    int deleteByUserId(@Param("userId") Integer userId);

    int deleteByGroupId(@Param("groupId") Integer groupId);
}