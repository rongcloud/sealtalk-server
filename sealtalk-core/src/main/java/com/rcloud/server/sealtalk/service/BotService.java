package com.rcloud.server.sealtalk.service;

import com.rcloud.server.sealtalk.constant.BotTypeEnum;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.ErrorMsg;
import com.rcloud.server.sealtalk.dao.BotUserBindMapper;
import com.rcloud.server.sealtalk.entity.BotUserBind;
import com.rcloud.server.sealtalk.entity.Users;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.model.dto.BotDto;
import com.rcloud.server.sealtalk.model.dto.PhoneDTO;
import com.rcloud.server.sealtalk.model.dto.RCBotDTO;
import com.rcloud.server.sealtalk.model.dto.RCBotDTO.Auth;
import com.rcloud.server.sealtalk.model.dto.RCBotDTO.Integration;
import com.rcloud.server.sealtalk.rongcloud.RongCloudClient;
import com.rcloud.server.sealtalk.util.MiscUtils;
import io.rong.messages.TxtMessage;
import io.rong.models.Result;
import io.rong.models.message.PrivateMessage;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rcloud.server.sealtalk.dao.BotInfoMapper;
import com.rcloud.server.sealtalk.entity.BotInfo;
import com.rcloud.server.sealtalk.util.RandomUtil;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Service
@Slf4j
public class BotService {

    @Autowired
    private BotInfoMapper botInfoMapper;

    @Autowired
    private RongCloudClient rongCloudClient;

    @Autowired
    private UsersService usersService;

    @Autowired
    private BotUserBindMapper botUserBindMapper;


    /**
     * 创建机器人
     * @return 机器人ID
     */
    public String createBot(BotDto botDto) throws Exception {

        // 检查机器人数量限制
        int currentCount = botInfoMapper.countAll();
        if (currentCount >= Constants.BOT_CNT_LIMIT) {
            log.warn("机器人数量已达上限，当前数量：{}，最大限制：{}", currentCount, Constants.BOT_CNT_LIMIT);
            throw ServiceException.buildError(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.NUM_EXCEED_LIMIT, "bot");
        }

        // 生成随机机器人ID
        String botId = Constants.BOT_ID_PREFIX + RandomUtil.uuid();

        // 创建机器人信息对象
        BotInfo botInfo = new BotInfo();
        botInfo.setBotId(botId);
        botInfo.setName(botDto.getName());
        botInfo.setPortraitUri(botDto.getPortraitUri());
        botInfo.setOpeningMessage(Optional.ofNullable(botDto.getOpeningMessage()).orElse(""));
        botInfo.setBotType(Optional.ofNullable(BotTypeEnum.getByType(botDto.getBotType()))
            .orElse(BotTypeEnum.PUBLIC).getType()); // 默认公有机器人
        botInfo.setBotStatus(1); // 默认生效状态

        // 插入数据库
        botInfoMapper.insertBot(botInfo);

        if (botDto.getIntegrations() != null && !botDto.getIntegrations().isEmpty()){
            RCBotDTO rcBotDTO = new RCBotDTO();
            rcBotDTO.setUserId(botId);
            rcBotDTO.setName(botInfo.getName());
            rcBotDTO.setType("BOT");
            rcBotDTO.setProfileUrl(botInfo.getPortraitUri());
            List<Integration> integrations = botDto.getIntegrations().stream().map(b -> {
                Integration integration = new Integration();
                integration.setEnabled(true);
                integration.setType(b.getIntegrateType());
                integration.setCallbackUrl(b.getCallbackUrl());
                integration.setObjectNames(b.getObjectNames());
                integration.setEvents(Collections.singletonList("message:private"));
                integration.setStream(b.getStream());
                integration.setHandleResponse(Optional.ofNullable(b.getHandleResponse()).orElse(true));
                Auth a = new Auth();
                a.setApiKey(b.getApiKey());
                integration.setAuth(a);
                return integration;
            }).collect(Collectors.toList());
            rcBotDTO.setIntegrations(integrations);
            Result result = rongCloudClient.createBot(rcBotDTO);
            if(result== null || result.code == null || result.code != 200){
                throw new ServiceException(ErrorCode.CALL_RC_SERVER_ERROR.getErrorCode(), "im create bot error");
            }
        }
        return botId;
    }

    /**
     * 根据botId查询机器人信息
     * @param botId 机器人ID
     * @return 机器人信息
     */
    public BotInfo getBotInfo(String botId) {
        return botInfoMapper.selectByBotId(botId);
    }

    /**
     * 批量查询机器人列表
     * @param limit 查询数量限制
     * @return 机器人列表
     */
    public List<BotInfo> getBotList(Integer limit) {
        return botInfoMapper.queryAllBot(limit);
    }


    public List<BotUserBind> bindUsers(long userId, Integer bindType) {
        return botUserBindMapper.selectByUserId(userId, bindType);
    }


    /**
     * 根据botId删除机器人
     * @param botId 机器人ID
     * @return 删除是否成功
     */
    public void deleteBot(String botId) throws Exception {

        // 删除机器人信息
        botInfoMapper.deleteByBotId(botId);

        // 先清理机器人用户绑定关系
        botUserBindMapper.deleteByBotId(botId);

        // 删除融云机器人
        rongCloudClient.delBot(botId);
    }

        /**
     * 更新机器人信息
     * @param updateDTO 机器人信息
     */
    public void updateBot(BotDto updateDTO) throws Exception {
        // 检查机器人是否存在
        BotInfo existingBot = botInfoMapper.selectByBotId(updateDTO.getBotId());
        if (existingBot == null) {
            throw ServiceException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "bot");
        }
        BotInfo updateInfo = new BotInfo();
        updateInfo.setBotId(updateDTO.getBotId());
        updateInfo.setName(StringUtils.isNoneBlank(updateDTO.getName()) ? updateDTO.getName() : null);
        updateInfo.setPortraitUri(StringUtils.isNoneBlank(updateDTO.getPortraitUri()) ? updateDTO.getPortraitUri() : null);
        updateInfo.setOpeningMessage(StringUtils.isNoneBlank(updateDTO.getOpeningMessage()) ? updateDTO.getOpeningMessage() : null);
        if (updateDTO.getBotType() != null) {
            BotTypeEnum newBotType = BotTypeEnum.getByType(updateDTO.getBotType());
            updateInfo.setBotType(newBotType != null ? newBotType.getType() : null);
        }
        // 执行更新（动态SQL会自动处理空字段）
        botInfoMapper.updateByBotId(updateInfo);

        if (updateInfo.getBotType() != null && updateInfo.getBotType().equals(BotTypeEnum.PUBLIC.getType())) {
            botUserBindMapper.deleteByBotId(updateInfo.getBotId());
        }

        RCBotDTO rcBotDTO = new RCBotDTO();
        rcBotDTO.setUserId(updateDTO.getBotId());
        rcBotDTO.setName(updateInfo.getName());
        rcBotDTO.setProfileUrl(updateInfo.getPortraitUri());
        if (updateDTO.getIntegrations() != null) {
            List<Integration> integrations = updateDTO.getIntegrations().stream().map(b -> {
                Integration integration = new Integration();
                integration.setType(b.getIntegrateType());
                integration.setCallbackUrl(b.getCallbackUrl());
                integration.setObjectNames(b.getObjectNames());
                integration.setStream(b.getStream());
                integration.setHandleResponse(b.getHandleResponse());
                Auth a = new Auth();
                a.setApiKey(b.getApiKey());
                integration.setAuth(a);
                return integration;
            }).collect(Collectors.toList());
            rcBotDTO.setIntegrations(integrations);
        }
        rongCloudClient.updateBot(rcBotDTO);
    }
    

    public void bindUserToBot(List<PhoneDTO> phones, String botId) throws ServiceException {
        List<BotUserBind> binds = phones.stream().map(p -> {
            String region = MiscUtils.removeRegionPrefix(p.getRegion());
            return usersService.queryUserByPhone(region, p.getPhone());
        }).filter(Objects::nonNull)
            .map(Users::getId)
            .map(uid -> {
                BotUserBind bind = new BotUserBind();
                bind.setBotId(botId);
                bind.setUserId((long) uid);
                return bind;
            })
            .collect(Collectors.toList());
        if (binds.isEmpty()) {
            return;
        }
        botUserBindMapper.batchInsert(binds);
    }



    public void sendOpenMsg(String userId) {
        List<BotInfo> botInfos = botInfoMapper.queryAllBot(Constants.BOT_CNT_LIMIT);
        if (botInfos == null || botInfos.isEmpty()) {
            return;
        }
        for (BotInfo b : botInfos){
            if (StringUtils.isBlank(b.getOpeningMessage())) {
                continue;
            }
            TxtMessage txtMessage = new TxtMessage(b.getOpeningMessage(), null);
            PrivateMessage privateMessage = new PrivateMessage()
                .setSenderId(b.getBotId())
                .setTargetId(new String[]{userId})
                .setObjectName(txtMessage.getType())
                .setContent(txtMessage)
                .setIsIncludeSender(0)
                .setIsPersisted(1)
                .setIsCounted(1);
            try{
                rongCloudClient.sendPrivateMessage(privateMessage);
            }catch (Exception e){
                log.error("send bot open message error: botId:{} userId:{}", b.getBotId(), userId, e);
            }


        }
    }


}

