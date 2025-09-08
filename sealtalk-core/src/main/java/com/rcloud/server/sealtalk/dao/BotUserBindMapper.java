package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.BotUserBind;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BotUserBindMapper {

    /**
     * 根据用户ID查询绑定的机器人列表
     * @param userId 用户ID
     * @return 绑定列表
     */
    List<BotUserBind> selectByUserId(@Param("userId") Long userId, @Param("bindType") Integer bindType);

    /**
     * 根据机器人ID查询绑定的用户列表
     * @param botId 机器人ID
     * @return 绑定列表
     */
    List<BotUserBind> selectByBotId(String botId);

    /**
     * 根据用户ID和机器人ID查询绑定关系
     * @param userId 用户ID
     * @param botId 机器人ID
     * @return 绑定关系
     */
    BotUserBind selectByUserIdAndBotId(Long userId, String botId);

    /**
     * 删除用户与机器人的绑定关系
     * @param userId 用户ID
     * @param botId 机器人ID
     * @return 删除数量
     */
    int deleteByUserIdAndBotId(Long userId, String botId);

    /**
     * 批量插入绑定关系
     * @param bindList 绑定关系列表
     * @return 插入数量
     */
    int batchInsert(List<BotUserBind> bindList);

    /**
     * 根据机器人ID删除所有绑定关系
     * @param botId 机器人ID
     * @return 删除数量
     */
    int deleteByBotId(String botId);

    /**
     * 批量删除：按机器人ID和用户ID集合删除绑定关系
     */
    int batchDeleteByBotIdAndUserIds(@org.apache.ibatis.annotations.Param("botId") String botId,
                                     @org.apache.ibatis.annotations.Param("userIds") java.util.List<Long> userIds);

} 