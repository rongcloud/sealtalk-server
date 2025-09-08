package com.rcloud.server.sealtalk.service;

import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.ErrorMsg;
import com.rcloud.server.sealtalk.constant.FriendShipStatus;
import com.rcloud.server.sealtalk.constant.UserAuth.FriendAuth;
import com.rcloud.server.sealtalk.dao.BlackListsMapper;
import com.rcloud.server.sealtalk.dao.FriendshipsMapper;
import com.rcloud.server.sealtalk.dao.UsersMapper;
import com.rcloud.server.sealtalk.entity.BlackLists;
import com.rcloud.server.sealtalk.entity.Friendships;
import com.rcloud.server.sealtalk.entity.Users;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.model.dto.ContractInfoDTO;
import com.rcloud.server.sealtalk.model.dto.InviteDTO;
import com.rcloud.server.sealtalk.rongcloud.RongCloudClient;
import com.rcloud.server.sealtalk.rongcloud.message.ContactNotificationMessage;
import com.rcloud.server.sealtalk.util.MiscUtils;
import com.rcloud.server.sealtalk.util.N3d;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class FriendshipsService {

    @Autowired
    private UsersService usersService;
    @Autowired
    private FriendshipsMapper friendshipsMapper;
    @Autowired
    private RongCloudClient rongCloudClient;
    @Autowired
    private BlackListsMapper blackListsMapper;
    @Autowired
    private UsersMapper usersMapper;


    public Friendships queryByUserIdAndFriendId(Integer userId, Integer friendId) {
        return friendshipsMapper.selectByUserIdAndFriendId(userId, friendId);
    }

    public void delAllFriendShips(Integer userId){
        friendshipsMapper.deleteByUserIdOrFriendId(userId);
    }

    public List<Friendships> getFriendAgreed(Integer userId) {
        return friendshipsMapper.selectByUserIdAndStatus(userId,List.of(FriendShipStatus.AGREED.getStatus()));
    }

    /**
     * 发起添加好友
     */
    public InviteDTO invite(Integer userId, Integer friendId, String message) throws Exception {
        log.info("invite user. currentUserId:[{}] friendId:[{}]", userId, friendId);
        if (userId.equals(friendId)) {
            return new InviteDTO("None", "Do nothing.");
        }
        var users = usersService.queryById(friendId);
        if (FriendAuth.REQUIRE_AUTH.getAuth().equals(users.getFriVerify())) {
            // 需要对方验证
            return addVerifyFriend(userId, friendId, message);
        }
        addFriend(userId, friendId, message, true, Constants.CONTACT_OPERATION_REQUEST);
        return new InviteDTO("AddDirectly", "Request sent.");
    }


    /**
     * 同意添加好友
     */
    public void agree(Integer userId, Integer friendId) throws Exception {
        addFriend(userId, friendId, "", true, Constants.CONTACT_OPERATION_ACCEPT_RESPONSE);
    }



    /**
     * 忽略好友请求
     */
    public void ignore(Integer userId, Integer friendId) {
        Friendships update = new Friendships();
        update.setUserId(userId);
        update.setFriendId(friendId);
        update.setStatus(FriendShipStatus.IGNORED.getStatus());
        friendshipsMapper.updateByUserIdAndFriendIdSelective(update);
    }

    /**
     * 删除好友
     */
    public void delete(Integer userId, Integer friendId) throws Exception {

        usersService.addBlackList(userId, friendId, false);

        Friendships update = new Friendships();
        update.setUserId(userId);
        update.setFriendId(friendId);
        update.setStatus(FriendShipStatus.DELETED.getStatus());
        friendshipsMapper.updateByUserIdAndFriendIdSelective(update);


        Friendships update2 = new Friendships();
        update2.setUserId(friendId);
        update2.setFriendId(userId);
        update2.setStatus(FriendShipStatus.DELETED.getStatus());
        friendshipsMapper.updateByUserIdAndFriendIdSelective(update2);

    }

    /**
     * 更新好友信息
     */
    public void updateFriendInfo(Friendships friendships){
        friendshipsMapper.updateByUserIdAndFriendIdSelective(friendships);
    }



    private InviteDTO addVerifyFriend(Integer userId, Integer friendId, String message) throws Exception {

        Friendships userFriendships = friendshipsMapper.selectByUserIdAndFriendId(userId, friendId);
        Friendships targetFriendships = friendshipsMapper.selectByUserIdAndFriendId(friendId, userId);
        if (userFriendships == null || targetFriendships == null) {
            saveFriendShip(userId, friendId, "", FriendShipStatus.REQUESTINT);
            saveFriendShip(friendId, userId, message, FriendShipStatus.REQUESTED);
            sendAddFriendMsg(userId, friendId, message, Constants.CONTACT_OPERATION_REQUEST);
            return new InviteDTO("Sent", "Request sent.");
        }

        BlackLists blackLists = blackListsMapper.selectByUserIdAndFriendId(friendId, userId);
        if (blackLists != null && BlackLists.STATUS_VALID.equals(blackLists.getStatus()) && FriendShipStatus.BLACK.getStatus().equals(targetFriendships.getStatus())) {
            //在对方黑名单中不能添加好友，返回Do nothing.
            return new InviteDTO("None", "Do nothing.");
        }

        if (FriendShipStatus.AGREED.getStatus().equals(userFriendships.getStatus()) && FriendShipStatus.AGREED.getStatus().equals(targetFriendships.getStatus())) {
            //如果双方的已经是好友了，返回异常提示
            String errorMsg = "User " + N3d.encode(friendId) + " is already your friend.";
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), errorMsg);
        }

        //对方如果也加你,或者对方已同意，那直接添加成为好友
        if (FriendShipStatus.REQUESTINT.getStatus().equals(targetFriendships.getStatus())
                || FriendShipStatus.AGREED.getStatus().equals(targetFriendships.getStatus())) {
            addFriend(userId, friendId, message, false, null);
            return new InviteDTO("Added", "Do nothing.");
        }

        //对方忽略好友请求,一天后才能发送加好友消息
        if (FriendShipStatus.IGNORED.getStatus().equals(targetFriendships.getStatus())
            &&  System.currentTimeMillis() - targetFriendships.getUpdatedAt().getTime() < Constants.ONE_DAY_MILLION_SECONDS){
            return new InviteDTO("None", "Do nothing.");
        }
        //对方没看,三天后才能发送加好友消息
        if (FriendShipStatus.REQUESTED.getStatus().equals(targetFriendships.getStatus())
                && System.currentTimeMillis() - targetFriendships.getUpdatedAt().getTime() < 3 * Constants.ONE_DAY_MILLION_SECONDS) {
            return new InviteDTO("None", "Do nothing.");
        }
        //其他场景都可以发消息
        saveFriendShip(userId, friendId, "", FriendShipStatus.REQUESTINT);
        saveFriendShip(friendId, userId, message, FriendShipStatus.REQUESTED);
        sendAddFriendMsg(userId, friendId, message, Constants.CONTACT_OPERATION_REQUEST);
        return new InviteDTO("Sent", "Request sent.");
    }



    /**
     * 添加好友
     */
    private void addFriend(Integer currentUserId, Integer friendId, String message, boolean sendMsg, String operate) throws Exception {
        usersService.removeBlackList(currentUserId, friendId);
        usersService.removeBlackList(friendId, currentUserId);
        saveFriendShip(currentUserId, friendId, message, FriendShipStatus.AGREED);
        saveFriendShip(friendId, currentUserId, message, FriendShipStatus.AGREED);
        if (sendMsg) {
            sendAddFriendMsg(currentUserId, friendId, message, operate);
        }
    }


    /**
     * 发送好友申请消息
     */
    private void sendAddFriendMsg(Integer userId, Integer friendId, String message, String operate){
        Thread.ofVirtual().start(() -> {
            try {
                Users currentUser = usersService.queryById(userId);
                String userIdStr = N3d.encode(userId);
                String friendIdStr =N3d.encode(friendId);

                Map<String, Object> extraInfoMap = new HashMap<>();
                extraInfoMap.put("sourceUserNickname", currentUser.getNickname());
                extraInfoMap.put("version", System.currentTimeMillis());
                ContactNotificationMessage contactNotificationMessage = new ContactNotificationMessage(userIdStr, friendIdStr, operate, message, extraInfoMap);
                rongCloudClient.sendSystemMessage(userIdStr,List.of(friendIdStr), contactNotificationMessage);
            } catch (Exception e) {
                log.error("", e);
            }
        });
    }

    /**
     * 保存/更新好友信息
     */
    private void saveFriendShip(Integer userId, Integer friendId, String message, FriendShipStatus status){
        Friendships friendships = new Friendships();
        friendships.setUserId(userId);
        friendships.setFriendId(friendId);
        friendships.setMessage(Optional.ofNullable(message).orElse(""));
        friendships.setStatus(status.getStatus());
        friendshipsMapper.saveOrUpdate(friendships);
    }



    /**
     * 获取当前用户好友列表
     */
    public List<Friendships> getFriendList(Integer currentUserId) {
        List<Friendships> friendships = friendshipsMapper.getFriendShipListWithUsers(currentUserId);
        return friendships.stream()
                .filter(fs -> !Integer.valueOf(FriendShipStatus.DELETED.getStatus()).equals(fs.getStatus()))
                .collect(Collectors.toList());
    }


    public List<Friendships> getFriendList(Integer currentUserId, List<Integer> friendIds, FriendShipStatus friendShipStatus) {
        List<Friendships> friendships = friendshipsMapper.selectByUserIdAndFriendIds(currentUserId, friendIds);
        return friendships.stream()
                .filter(fs -> Integer.valueOf(friendShipStatus.getStatus()).equals(fs.getStatus()))
                .collect(Collectors.toList());
    }




    /**
     * 获取好友信息
     */
    public Friendships getFriendProfile(Integer currentUserId, Integer friendId) throws ServiceException {
        Friendships friendships = friendshipsMapper.getFriendShipWithUsers(currentUserId, friendId, FriendShipStatus.AGREED.getStatus());
        if (friendships == null) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_FRIEND);
        }
        return friendships;
    }


    /**
     * 获取手机通讯录好友列表
     */
    public List<ContractInfoDTO> getContactsInfo(Integer currentUserId, String[] contactList) {

        List<Users> usersList = usersMapper.selectByPhones(Arrays.asList(contactList));
        List<Integer> friendIds = usersList == null ? Collections.emptyList() : usersList.stream().map(Users::getId).toList();
        List<Friendships> friends = friendIds.isEmpty() ? Collections.emptyList() : getFriendList(currentUserId, friendIds, FriendShipStatus.AGREED);
        Set<Integer> registerAndFriends = friends.stream().map(Friendships::getFriendId).collect(Collectors.toSet());
        Map<String, Users> phoneUserMap = usersList == null
                ? Collections.emptyMap()
                : usersList.stream().collect(Collectors.toMap(Users::getPhone, v -> v, (v1, v2) -> v2));
        return Stream.of(contactList).map(p ->{
            ContractInfoDTO contractInfoDTO = new ContractInfoDTO();
            Users users = phoneUserMap.get(p);
            if (users == null) {
                contractInfoDTO.setRegistered(ContractInfoDTO.UN_REGISTERED);
                contractInfoDTO.setRelationship(ContractInfoDTO.NON_FRIEND);
                contractInfoDTO.setPhone(p);
                return contractInfoDTO;
            }
            try{
                contractInfoDTO.setId(N3d.encode(users.getId()));
                contractInfoDTO.setRegistered(ContractInfoDTO.REGISTERED);
                boolean friendFlag = registerAndFriends.contains(users.getId());
                contractInfoDTO.setRelationship(friendFlag ? ContractInfoDTO.IS_FRIEND : ContractInfoDTO.NON_FRIEND);
                contractInfoDTO.setNickname(users.getNickname());
                contractInfoDTO.setPortraitUri(users.getPortraitUri());
                contractInfoDTO.setStAccount(users.getStAccount());
                contractInfoDTO.setPhone(p);
                return contractInfoDTO;
            }catch (Exception e){
                log.error("",e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public void batchDelete(Integer userId, List<Integer> friendIds) throws Exception {
        friendshipsMapper.updateStatusByUserIdAndFriendIds(userId, friendIds, FriendShipStatus.DELETED.getStatus());
        //更新成功后添加到 IM 黑名单
        List<String> encodeFriendIds = MiscUtils.batchEncodeIds(friendIds);
        rongCloudClient.addUserBlackList(N3d.encode(userId), encodeFriendIds);
    }

}
