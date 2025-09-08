package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.UltraGroupChannel;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UltraGroupChannelMapper {

    int insert(UltraGroupChannel record);
    List<UltraGroupChannel> queryUltraGroupChannelListByPage(@Param("groupId") Integer groupId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    // 1. 增加一个根据ultraGroupId 查询列表的方法
    List<UltraGroupChannel> selectByUltraGroupId(@Param("ultraGroupId") Integer ultraGroupId);

    // 2. 增加根据ultraGroupId删除的方法
    int deleteByUltraGroupId(@Param("ultraGroupId") Integer ultraGroupId);

    // 3. 增加根据ultraGroupId ，channelId 查询单个的方法
    UltraGroupChannel selectByUltraGroupIdAndChannelId(@Param("ultraGroupId") Integer ultraGroupId, @Param("channelId") String channelId);

    // 4. 增加根据 ultraGroupId 和 channelId列表删除的方法
    int deleteByUltraGroupIdAndChannelIds(@Param("ultraGroupId") Integer ultraGroupId, @Param("channelIds") List<String> channelIds);

    int updateByGroupIdAndChannelIdSelective(UltraGroupChannel record);
}