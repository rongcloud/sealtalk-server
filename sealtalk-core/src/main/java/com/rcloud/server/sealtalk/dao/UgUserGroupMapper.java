package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.UgUserGroup;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * @author jianzheng.li
 */
public interface UgUserGroupMapper {


    int insert(UgUserGroup record);

    List<Long> queryByGroupIdAndIds(@Param("groupId") Integer groupId,
        @Param("ids") List<Long> ids);

    List<UgUserGroup> queryByGroupIdPage(@Param("groupId") Integer groupId,
        @Param("offset") int offset, @Param("limit") int limit);

    UgUserGroup selectById(@Param("id") Long id);
    List<UgUserGroup> selectByGroupId(@Param("groupId") Integer groupId);

    int deleteByGroupId(@Param("groupId") Integer groupId);
    int deleteById(@Param("id") Long id);

}
