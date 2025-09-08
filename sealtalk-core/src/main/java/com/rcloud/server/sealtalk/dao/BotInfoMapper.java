package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.BotInfo;

import java.util.List;

public interface BotInfoMapper {

    int insertBot(BotInfo botInfo);
    
    BotInfo selectByBotId(String botId);
    
    List<BotInfo> queryAllBot(Integer limit);
    
    List<BotInfo> selectByBotType(Integer botType);
    
    int countAll();
    
    int deleteByBotId(String botId);

    /**
     * 根据botId更新机器人信息
     * @param botInfo 机器人信息
     * @return 更新数量
     */
    int updateByBotId(BotInfo botInfo);

}
