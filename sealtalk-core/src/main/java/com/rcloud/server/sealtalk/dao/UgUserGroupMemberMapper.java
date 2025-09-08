package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.UgUserGroupMember;
import com.rcloud.server.sealtalk.entity.Users;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

/**
 * @author jianzheng.li
 */
public interface UgUserGroupMemberMapper {


    int insertBatch(List<UgUserGroupMember> list);

    List<Integer> queryMembers(@Param("userGroupId") Long userGroupId,
        @Param("memberIds") List<Integer> memberIds);

    int delBatch(@Param("userGroupId") Long userGroupId,
        @Param("memberIds") List<Integer> memberIds);

    int deleteByUserGroupIds(@Param("userGroupIds") List<Long> userGroupIds);

    int deleteByMemberIds(@Param("memberIds") List<Integer> memberIds);

    UgUserGroupMember selectByUserGroupIdAndMemberId(@Param("userGroupId") Long userGroupId, @Param("memberId") Integer memberId);

    List<UgUserGroupMember> selectByUserGroupId(@Param("userGroupId") Long userGroupId);

    List<Users> queryByUserGroupIdPage(@Param("userGroupId") Long userGroupId,
        @Param("offset") int offset, @Param("limit") int limit);

    List<Map<String, Long>> memberCount(@Param("userGroupIds") List<Long> userGroupId);

}
