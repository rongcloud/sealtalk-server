package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.UgChannelUserGroup;
import com.rcloud.server.sealtalk.entity.UgUserGroup;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * @author jianzheng.li
 */
public interface UgChannelUserGroupMapper {


    List<Long> queryByChannelIdAndUserGroupIds(@Param("channelId") String channelId,
        @Param("userGroupIds") List<Long> userGroupIds);

    int insertBatch(List<UgChannelUserGroup> list);

    int delBatch(@Param("channelId") String channelId,
        @Param("userGroupIds") List<Long> userGroupIds);

    int deleteByChannelIds(@Param("channelIds") List<String> channelIds);

    List<UgChannelUserGroup> selectByChannelIds(@Param("channelIds") List<String> channelIds);

    List<UgChannelUserGroup> selectByChannelId(@Param("channelId") String channelId);

    int deleteByUserGroupIds(@Param("userGroupIds") List<Long> userGroupIds);

    List<UgUserGroup> queryByChannelIdPage(@Param("channelId") String channelId, @Param("offset") int offset,
        @Param("limit") int limit);
}
