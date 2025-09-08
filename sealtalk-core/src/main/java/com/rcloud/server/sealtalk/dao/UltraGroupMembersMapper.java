package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.UltraGroupMembers;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface UltraGroupMembersMapper {

    List<UltraGroupMembers> queryGroupMembersWithGroupByMemberId(@Param("memberId") Integer memberId);

    List<UltraGroupMembers> queryGroupMembersWithUsersByGroupId(@Param("groupId") Integer groupId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    List<UltraGroupMembers> queryByMemberIds(@Param("groupId") Integer groupId,
                                             @Param("memberIds") List<Integer> memberIds);

    // 1. 根据 ultraGroupId和 memberId查询单个的方法
    UltraGroupMembers selectByUltraGroupIdAndMemberId(@Param("ultraGroupId") Integer ultraGroupId, @Param("memberId") Integer memberId);

    // 2. 按照 ultraGroupId查询列表的方法
    List<UltraGroupMembers> selectByUltraGroupId(@Param("ultraGroupId") Integer ultraGroupId);

    // 3. 增加按照memberId查询列表的方法
    List<UltraGroupMembers> selectByMemberId(@Param("memberId") Integer memberId);

    // 4. 增加按照 ultraGroupId删除的方法
    int deleteByUltraGroupId(@Param("ultraGroupId") Integer ultraGroupId);

    // 5. 增加按照memberId删除的方法
    int deleteByMemberId(@Param("memberId") Integer memberId);

    // 6. 增加按照ultraGroupId 和memberId列表删除的方法
    int deleteByUltraGroupIdAndMemberIds(@Param("ultraGroupId") Integer ultraGroupId, @Param("memberIds") List<Integer> memberIds);

    int insertIgnoreBatch(@Param("list") List<UltraGroupMembers> list);

    List<Map<String, Object>> countByMemberIds(@Param("memberIds") List<Integer> memberIds);
}