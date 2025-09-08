package com.rcloud.server.sealtalk.controller;

import com.rcloud.server.sealtalk.constant.BindType;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.ErrorMsg;
import com.rcloud.server.sealtalk.constant.FriendShipStatus;
import com.rcloud.server.sealtalk.constant.LogType;
import com.rcloud.server.sealtalk.dto.FriendshipParam;
import com.rcloud.server.sealtalk.dto.InviteFriendParam;
import com.rcloud.server.sealtalk.entity.BotInfo;
import com.rcloud.server.sealtalk.entity.BotUserBind;
import com.rcloud.server.sealtalk.entity.Friendships;
import com.rcloud.server.sealtalk.entity.Users;
import com.rcloud.server.sealtalk.exception.ParamException;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.service.AiUserService;
import com.rcloud.server.sealtalk.service.BotService;
import com.rcloud.server.sealtalk.model.ServerApiParams;
import com.rcloud.server.sealtalk.model.dto.*;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserDetailDTO;
import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import com.rcloud.server.sealtalk.service.FriendshipsService;
import com.rcloud.server.sealtalk.util.MiscUtils;
import com.rcloud.server.sealtalk.util.N3d;
import com.rcloud.server.sealtalk.util.ValidateUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 好友相关功能接口
 */
@RestController
@RequestMapping("/friendship")
@Slf4j
public class FriendshipController extends BaseController {

    @Autowired
    private FriendshipsService friendshipsService;


    @Autowired
    private BotService botService;

    @Autowired
    private AiUserService aiUserService;


    /**
     * 发起添加好友
     */
    @PostMapping(value = "/invite")
    public APIResult<Object> invite(@RequestBody InviteFriendParam inviteFriendParam) throws Exception {
        String friendId = inviteFriendParam.getFriendId();
        if (StringUtils.isBlank(friendId)) {
            throw new ParamException(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.formatMsg(ErrorMsg.PARAM_REQUIRED, "friendId"));
        }
        String message = inviteFriendParam.getMessage();
        message = MiscUtils.xss(message, ValidateUtils.FRIEND_REQUEST_MESSAGE_MAX_LENGTH);
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = apiParams.getCurrentUserId();
        InviteDTO inviteResponse = friendshipsService.invite(currentUserId, N3d.decode(friendId), message);
        log.info("{},{},{},{}", LogType.ADD_FRIEND, currentUserId, apiParams.getCurrentUserIdStr(), friendId);
        return APIResultWrap.ok(inviteResponse);
    }


    /**
     * 同意添加好友
     */
    @PostMapping(value = "/agree")
    public APIResult<Object> agree(@RequestBody FriendshipParam friendshipParam) throws Exception {

        String friendId = friendshipParam.getFriendId();
        ValidateUtils.notBlank(friendId,"friendId");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        friendshipsService.agree(currentUserId, N3d.decode(friendId));
        log.info("{},{},{},{}", LogType.AGREE_FRIEND, currentUserId, apiParams.getCurrentUserIdStr(), friendId);
        return APIResultWrap.ok();
    }

    /**
     * 忽略好友请求
     */
    @PostMapping(value = "/ignore")
    public APIResult<Object> ignore(@RequestBody FriendshipParam friendshipParam) throws Exception {
        String friendId = friendshipParam.getFriendId();
        ValidateUtils.notBlank(friendId,"friendId");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        friendshipsService.ignore(currentUserId, N3d.decode(friendId));
        log.info("{},{},{},{}", LogType.IGNOR_FRIEND, currentUserId, apiParams.getCurrentUserIdStr(), friendId);
        return APIResultWrap.ok();
    }

    /**
     * 删除好友
     */
    @PostMapping(value = "/delete")
    public APIResult<Object> delete(@RequestBody FriendshipParam friendshipParam) throws Exception {
        String friendId = friendshipParam.getFriendId();
        ValidateUtils.notBlank(friendId,"friendId");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        friendshipsService.delete(currentUserId, N3d.decode(friendId));
        log.info("{},{},{},{}", LogType.DELETE_FRIEND, currentUserId, apiParams.getCurrentUserIdStr(), friendId);
        return APIResultWrap.ok();
    }

    /**
     * 设置好友备注名
     */
    @PostMapping(value = {"/updateFriendInfo", "/set_display_name", "/set_friend_description"})
    public APIResult<Object> updateFriendInfo(@RequestBody FriendshipParam friendshipParam) throws Exception {

        ValidateUtils.notBlank(friendshipParam.getFriendId(), "friendId");
        String displayName = MiscUtils.xss_null(friendshipParam.getDisplayName(), ValidateUtils.FRIEND_REQUEST_MESSAGE_MAX_LENGTH);
        Friendships updateFriend =  new Friendships();
        updateFriend.setUserId(getCurrentUserId());
        updateFriend.setFriendId(N3d.decode(friendshipParam.getFriendId()));
        updateFriend.setDisplayName(displayName);
        updateFriend.setRegion(friendshipParam.getRegion());
        updateFriend.setPhone(friendshipParam.getPhone());
        updateFriend.setDescription(friendshipParam.getDescription());
        updateFriend.setImageUri(friendshipParam.getImageUri());
        friendshipsService.updateFriendInfo(updateFriend);
        return APIResultWrap.ok();
    }


    /**
     * 获取好友列表
     */
    @GetMapping(value = "/all")
    public APIResult<Object> friendList() throws ServiceException {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        Integer currentUserId = getCurrentUserId();

        List<Friendships> friendshipsList = friendshipsService.getFriendList(currentUserId);

        List<BotInfo> botList = botService.getBotList(Constants.BOT_CNT_LIMIT);
        List<BotUserBind> botUserBinds = botService.bindUsers(currentUserId, BindType.BOT.getType());
        Set<String> bindPrivateBot = botUserBinds == null ? Collections.EMPTY_SET
            : botUserBinds.stream().map(BotUserBind::getBotId).collect(Collectors.toSet());
        List<FriendShipDTO> friendShipDTOS = new ArrayList<>();
        if (!CollectionUtils.isEmpty(botList)) {
            botList = botList.stream()
                .filter(bot -> bot.getBotType() == null || bot.getBotType() == 0 || bindPrivateBot.contains(bot.getBotId()))
                .collect(Collectors.toList());
            for (BotInfo bot : botList) {
                FriendShipDTO dto = new FriendShipDTO();
                dto.setDisplayName(bot.getName());
                dto.setStatus(FriendShipStatus.AGREED.getStatus());
                UserDTO userDTO = new UserDTO();
                userDTO.setId(bot.getBotId());
                userDTO.setNickname(bot.getName());
                userDTO.setPortraitUri(bot.getPortraitUri());
                dto.setUser(userDTO);
                friendShipDTOS.add(dto);
            }
        }

        if (!CollectionUtils.isEmpty(friendshipsList)) {
            for (Friendships friendships : friendshipsList) {
                FriendShipDTO dto = new FriendShipDTO();
                dto.setDisplayName(friendships.getDisplayName());
                dto.setMessage(friendships.getMessage());
                dto.setStatus(friendships.getStatus());
                dto.setUpdatedAt(sdf.format(friendships.getUpdatedAt()));
                dto.setUpdatedTime(friendships.getUpdatedAt().getTime());
                UserDTO userDTO = new UserDTO();
                Users users = friendships.getUsers();
                if (users != null) {
                    userDTO.setId(N3d.encode(users.getId()));
                    userDTO.setNickname(users.getNickname());
                    userDTO.setPortraitUri(users.getPortraitUri());
                    userDTO.setGender(users.getGender());
                    userDTO.setStAccount(users.getStAccount());
                }
                dto.setUser(userDTO);
                friendShipDTOS.add(dto);
            }
        }


        return APIResultWrap.ok(friendShipDTOS);
    }


    /**
     * 获取好友信息
     */
    @GetMapping(value = "/{friendId}/profile")
    public APIResult<Object> getFriendProfile(@PathVariable("friendId") String friendId) throws ServiceException {

        Integer currentUserId = getCurrentUserId();

        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> userMap = new HashMap<>();
        resultMap.put("user", userMap);
        if (friendId.startsWith(Constants.BOT_ID_PREFIX)) {
            BotInfo botInfo = botService.getBotInfo(friendId);
            if (botInfo != null) {
                resultMap.put("displayName", botInfo.getName());
                userMap.put("id", friendId);
                userMap.put("nickname", botInfo.getName());
                userMap.put("portraitUri", botInfo.getPortraitUri());
            }
        } if (friendId.startsWith(Constants.AI_USER_ID_PREFIX)){
            AiUserDetailDTO aiUserDetailDTO = aiUserService.getDetail(friendId);
            if (aiUserDetailDTO != null) {
                userMap.put("id", friendId);
                userMap.put("portraitUri", aiUserDetailDTO.getAvatar());
            }
        } else {
            Friendships friendships = friendshipsService.getFriendProfile(currentUserId,
                N3d.decode(friendId));
            if (friendships != null) {
                resultMap.put("displayName", friendships.getDisplayName());
                Users users = friendships.getUsers();
                if (users != null) {
                    userMap.put("id", N3d.encode(users.getId()));
                    userMap.put("nickname", users.getNickname());
                    userMap.put("portraitUri", users.getPortraitUri());
                }
            }
        }
        return APIResultWrap.ok(resultMap);
    }


    /**
     * 获取通讯录朋友信息列表
     */
    @PostMapping(value = "/get_contacts_info")
    public APIResult<?> getContactsInfo(@RequestBody FriendshipParam friendshipParam) throws ServiceException {
        String[] contactList = friendshipParam.getContactList();
        if (contactList == null || contactList.length == 0) {
            return APIResultWrap.ok();
        }
        Integer currentUserId = getCurrentUserId();
        List<ContractInfoDTO> contractInfoDTOList = friendshipsService.getContactsInfo(currentUserId, contactList);
        return APIResultWrap.ok(contractInfoDTOList);
    }

    /**
     * 批量删除好友
     */
    @PostMapping(value = "/batch_delete")
    public APIResult<Object> batchDelete(@RequestBody FriendshipParam friendshipParam) throws Exception {
        ValidateUtils.checkLength(friendshipParam.getFriendIds(),1,50,"friendIds");
        ServerApiParams apiParams = getServerApiParams();
        List<Integer> friendIds = MiscUtils.batchDecodeIds(Arrays.asList(friendshipParam.getFriendIds()));
        Integer currentUserId = getCurrentUserId();
        friendshipsService.batchDelete(currentUserId, friendIds);
        log.info("{},{},{},{}", LogType.DELETE_FRIEND, currentUserId, apiParams.getCurrentUserIdStr(), friendIds);
        return APIResultWrap.ok();
    }


    /**
     * 获取朋友备注和描述
     */
    @PostMapping(value = "/get_friend_description")
    public APIResult<?> getFriendDescription(@RequestBody FriendshipParam param) throws Exception {

        ValidateUtils.notBlank(param.getFriendId(),"friendId");

        Integer currentUserId = getCurrentUserId();
        Friendships friendships = friendshipsService.queryByUserIdAndFriendId(currentUserId, N3d.decode(param.getFriendId()));
        FriendDTO friendDTO = new FriendDTO();
        if (friendships != null) {
            friendDTO.setDescription(friendships.getDescription());
            friendDTO.setDisplayName(friendships.getDisplayName());
            friendDTO.setImageUri(friendships.getImageUri());
            friendDTO.setRegion(friendships.getRegion());
            friendDTO.setPhone(friendships.getPhone());
        }

        return APIResultWrap.ok(friendDTO);
    }

}
