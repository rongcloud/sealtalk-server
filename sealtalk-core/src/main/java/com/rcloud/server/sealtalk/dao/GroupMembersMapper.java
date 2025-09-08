package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.GroupMembers;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface GroupMembersMapper {

    int deleteByGroupId(@Param("groupId") Integer groupId);

    int deleteByMemberId(@Param("memberId") Integer memberId);

    int updateByPrimaryKeySelective(GroupMembers record);

    List<GroupMembers> queryGroupMembersWithGroupByMemberId(@Param("memberId") Integer memberId);

    List<GroupMembers> queryGroupMembersWithUsersByGroupId(@Param("groupId") Integer groupId);

    GroupMembers queryGroupMembersWithGroupByGroupIdAndMemberId(@Param("groupId") Integer groupId, @Param("memberId") Integer memberId);

    List<GroupMembers> selectByMemberId(@Param("memberId") Integer memberId);

    List<Map<String, Object>> countByMemberIds(@Param("memberIds") List<Integer> memberIds);

    int insertIgnoreBatch(@Param("groupMemberList") List<GroupMembers> groupMemberList);

    GroupMembers selectByGroupIdAndMemberId(@Param("groupId") Integer groupId, @Param("memberId") Integer memberId);

    List<GroupMembers> selectByGroupIdAndRoles(@Param("groupId") Integer groupId, @Param("roles") List<Integer> roles);

    List<GroupMembers> selectByGroupId(@Param("groupId") Integer groupId);

    int groupMemberCnt(@Param("groupId") Integer groupId);

    List<GroupMembers> selectByGroupIdAndMemberIds(@Param("groupId") Integer groupId, @Param("memberIds") List<Integer> memberIds);

    int updateRoleByGroupIdAndMemberIds(@Param("groupId") Integer groupId, @Param("memberIds") List<Integer> memberIds, @Param("role") Integer role);

    int deleteByGroupIdAndMemberIds(@Param("groupId") Integer groupId, @Param("memberIds") List<Integer> memberIds);
}