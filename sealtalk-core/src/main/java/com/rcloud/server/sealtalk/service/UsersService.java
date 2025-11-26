package com.rcloud.server.sealtalk.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.rcloud.server.sealtalk.configuration.SealtalkConfig;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.ErrorMsg;
import com.rcloud.server.sealtalk.constant.FriendShipStatus;
import com.rcloud.server.sealtalk.dao.BlackListsMapper;
import com.rcloud.server.sealtalk.dao.FreezeDataMapper;
import com.rcloud.server.sealtalk.dao.FriendshipsMapper;
import com.rcloud.server.sealtalk.dao.GroupFavsMapper;
import com.rcloud.server.sealtalk.dao.GroupMembersMapper;
import com.rcloud.server.sealtalk.dao.GroupReceiversMapper;
import com.rcloud.server.sealtalk.dao.UgUserGroupMemberMapper;
import com.rcloud.server.sealtalk.dao.UltraGroupMembersMapper;
import com.rcloud.server.sealtalk.dao.UsersMapper;
import com.rcloud.server.sealtalk.dto.RegisterInfoDTO;
import com.rcloud.server.sealtalk.entity.BlackLists;
import com.rcloud.server.sealtalk.entity.FreezeData;
import com.rcloud.server.sealtalk.entity.Friendships;
import com.rcloud.server.sealtalk.entity.GroupFavs;
import com.rcloud.server.sealtalk.entity.GroupMembers;
import com.rcloud.server.sealtalk.entity.Groups;
import com.rcloud.server.sealtalk.entity.UltraGroupMembers;
import com.rcloud.server.sealtalk.entity.Users;
import com.rcloud.server.sealtalk.exception.ParamException;
import com.rcloud.server.sealtalk.exception.RCloudHttpException;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.model.JwtTokenResult;
import com.rcloud.server.sealtalk.rongcloud.RongCloudClient;
import com.rcloud.server.sealtalk.util.JacksonUtil;
import com.rcloud.server.sealtalk.util.N3d;
import com.rcloud.server.sealtalk.util.TimeUtil;
import io.rong.models.response.TokenResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UsersService {

    @Autowired
    private UsersMapper usersMapper;


    public static final int BLOCK_MINUTE = 43200;
    @Autowired
    private RongCloudClient rongCloudClient;

    @Autowired
    private GroupMembersMapper groupMembersMapper;

    @Autowired
    @Lazy
    private FriendshipsService friendshipsService;

    @Autowired
    private GroupFavsMapper groupFavsMapper;
    @Autowired
    private UltraGroupMembersMapper ultraGroupMembersMapper;

    @Autowired
    private GroupReceiversMapper groupReceiversMapper;

    @Autowired
    private UgUserGroupMemberMapper ugUserGroupMemberMapper;

    @Autowired
    @Lazy
    private GroupsService groupsService;
    @Autowired
    private FreezeDataMapper freezeDataMapper;

    @Autowired
    @Lazy
    private BotService botService;

    @Autowired
    private SealtalkConfig sealtalkConfig;

    @Autowired
    private FriendshipsMapper friendshipsMapper;

    @Autowired
    private BlackListsMapper blackListsMapper;

    @Autowired
    private RegisterAfterService registerAfterService;

    private final static Cache<String, String> NICK_NAME_CACHE = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    /**
     * 根据id查询用户信息
     */
    public Users queryById(Integer userId) {
        return usersMapper.selectByPrimaryKey(userId);
    }


    public List<Users> queryByIds(List<Integer> ids) {
        return usersMapper.selectByIds(ids);
    }

    /**
     * 根据手机号用户信息
     */
    public Users queryUserByPhone(String region, String phone) {
        return  usersMapper.selectByRegionAndPhone(region,phone);
    }

    public List<Users> queryUserByPhone(String phone) {
        return usersMapper.selectByPhones(List.of(phone));
    }

    /**
     * 根据stAccount查询用户信息
     */
    public Users queryUserByStAccount(String stAccount) {
        return usersMapper.selectByStAccount(stAccount);
    }


    /**
     * 注册
     */
    public Triple<Integer, String, String> register(String region, String phone, String channel, String version, String os, String ip) throws Exception {
        //如果是新用户，注册用户
        Users u = queryUserByPhone(region, phone);
        if (u == null) {
            u = new Users();
            u.setNickname("融云" + phone.substring(phone.length() - 4, phone.length()));
            u.setRegion(region);
            u.setPhone(phone);
            u.setCreatedAt(new Date());
            u.setUpdatedAt(new Date());
            u.setPortraitUri(sealtalkConfig.getRongcloudDefaultPortraitUrl());
            usersMapper.insertSelective(u);
            String stAccount = userStAccount(u.getCreatedAt().getTime(), u.getId());
            u.setStAccount(stAccount);
            usersMapper.updateStAccountById(u.getId(), stAccount);

            String userIdStr = N3d.encode(u.getId());
            Thread.ofVirtual().start(() -> botService.sendOpenMsg(userIdStr));
            Thread.ofVirtual().start(() -> {
                RegisterInfoDTO dto = new RegisterInfoDTO();
                dto.setRegion(region);
                dto.setPhone(phone);
                dto.setChannel(channel);
                dto.setVersion(version);
                dto.setOs(os);
                dto.setIp(ip);
                registerAfterService.execute(dto);
            });
        }
        String userIdStr = N3d.encode(u.getId());
        log.info("{},id:{},{}, nickname:{}, region:{}, phone={} ", "login", u.getId(), userIdStr, u.getNickname(), u.getRegion(), u.getPhone());
        //缓存nickname
        NICK_NAME_CACHE.put(String.valueOf(u.getId()),u.getNickname());
        if (StringUtils.isNotBlank(u.getRongCloudToken())) {
            return Triple.of(u.getId(), u.getRongCloudToken(), u.getNickname());
        }
        //获取融云token
        String token = getRcToken(u.getId(), userIdStr);
        return Triple.of(u.getId(), token,u.getNickname());
    }


    /**
     * 获取融云token
     */
    public String getRcToken(Integer userId, String userIdStr) throws Exception {
        Users u = queryById(userId);
        String portraitUri = StringUtils.isEmpty(u.getPortraitUri()) ? sealtalkConfig.getRongcloudDefaultPortraitUrl() : u.getPortraitUri();
        TokenResult tokenResult = rongCloudClient.register(userIdStr, u.getNickname(), portraitUri);
        if (!Constants.CODE_OK.equals(tokenResult.getCode())) {
            throw new ServiceException(ErrorCode.CALL_RC_SERVER_ERROR.getErrorCode(), "token error");
        }
        // 异步延迟更新用户唯一标识
        if (StringUtils.isNotBlank(u.getStAccount())){
            CompletableFuture.runAsync(() -> {
                try {
                    rongCloudClient.updateUserUniqueId(userIdStr, u.getStAccount());
                } catch (Throwable e) {
                    log.error("userId:{} ", userId, e);
                }
            }, CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS));
        }
        //更新token到本地
        String token = tokenResult.getToken();
        Users updateUser = new Users();
        updateUser.setId(u.getId());
        updateUser.setRongCloudToken(token);
        usersMapper.updateByPrimaryKeySelective(updateUser);
        return token;
    }


    public void updateUserInfo(Users updateUser) throws Exception {
        updateUserInfo(updateUser, true);
    }

    /**
     * 更新用户信息
     */
    public void updateUserInfo(Users updateUser , boolean syncRc) throws Exception{
        Integer userId = updateUser.getId();
        if (syncRc && (StringUtils.isNotBlank(updateUser.getPortraitUri()) || StringUtils.isNotBlank(updateUser.getNickname()))) {
            String userIdStr = N3d.encode(updateUser.getId());
            var u = queryById(updateUser.getId());
            var nickName = StringUtils.isBlank(updateUser.getNickname()) ? u.getNickname() : updateUser.getNickname();
            var portraitUri = StringUtils.isBlank(updateUser.getPortraitUri()) ? u.getPortraitUri() : updateUser.getPortraitUri();
            rongCloudClient.updateUser(userIdStr, nickName, portraitUri);
            NICK_NAME_CACHE.put(String.valueOf(userId), nickName);
        }
        usersMapper.updateByPrimaryKeySelective(updateUser);
    }



    /**
     * 将好友加入黑名单
     */
    public void addBlackList(Integer currentUserId, Integer friendId , boolean updateFriendStatus) throws Exception {

        //调用融云服务接口新增黑名单
        rongCloudClient.addUserBlackList(N3d.encode(currentUserId), List.of(N3d.encode(friendId)));

        var black = new BlackLists();
        black.setUserId(currentUserId);
        black.setFriendId(friendId);
        black.setStatus(BlackLists.STATUS_VALID);
        //将黑名单信息插入或更新本地数据库
        blackListsMapper.saveOrUpdate(black);

        if (updateFriendStatus) {
            var friend = new Friendships();
            friend.setUserId(currentUserId);
            friend.setFriendId(friendId);
            friend.setStatus(FriendShipStatus.BLACK.getStatus());
            //好友状态更为拉黑
            friendshipsMapper.updateByUserIdAndFriendIdSelective(friend);
        }
    }

    /**
     * 将好友移除黑名单
     */
    public void removeBlackList(Integer currentUserId, Integer friendId) throws Exception {

        //调用融云服务接口移除黑名单
        rongCloudClient.removeUserBlackList(N3d.encode(currentUserId), List.of(N3d.encode(friendId)));

        //删除黑名单记录
        blackListsMapper.deleteByUserIdAndFriendId(currentUserId, friendId);

        var friend = new Friendships();
        friend.setUserId(currentUserId);
        friend.setFriendId(friendId);
        friend.setStatus(FriendShipStatus.AGREED.getStatus());
        //好友状态更新
        friendshipsMapper.updateByUserIdAndFriendIdSelective(friend);
    }



    /**
     * 获取黑名单列表
     */
    public List<BlackLists> getBlackList(Integer currentUserId) {
        return blackListsMapper.selectBlackListsWithFriendUsers(currentUserId);
    }

    /**
     * 获取用户所属群组
     */
    public List<Groups> getGroups(Integer currentUserId) {

        List<Groups> groupsList = new ArrayList<>();

        //缓存中为空，去查询db
        List<GroupMembers> groupMembersList = groupMembersMapper.queryGroupMembersWithGroupByMemberId(currentUserId);
        if (!CollectionUtils.isEmpty(groupMembersList)) {
            for (GroupMembers groupMembers : groupMembersList) {
                groupsList.add(groupMembers.getGroups());
            }
        }
        return groupsList;
    }



    /**
     * 获取通讯录群组列表
     *
     * @param userId
     * @param limit
     * @param offset
     * @return
     * @throws ServiceException
     */
    public Pair<Integer, List<Groups>> getFavGroups(Integer userId, Integer limit, Integer offset) throws ServiceException {
        List<Groups> groupsList = new ArrayList<>();
        Integer count = groupFavsMapper.queryCountGroupFavsWithGroupByUserId(userId);
        if (count != null && count > 0) {
            List<GroupFavs> groupFavsList = groupFavsMapper.queryGroupFavsWithGroupByUserId(userId, limit, offset);
            if (!CollectionUtils.isEmpty(groupFavsList)) {
                for (GroupFavs groupFavs : groupFavsList) {
                    if (groupFavs.getGroups() != null) {
                        groupsList.add(groupFavs.getGroups());
                    }
                }
            }
        }

        return Pair.of(count, groupsList);
    }


    /**
     * 获取云存储token
     */
    public String getImageToken() {
        String accessKey = sealtalkConfig.getQiniuAccessKey();
        String secretKey = sealtalkConfig.getQiniuSecretKey();
        String bucket = sealtalkConfig.getQiniuBucketName();
        Auth auth = Auth.create(accessKey, secretKey);
        StringMap policy = new StringMap();
        policy.put("mimeLimit", "image/*");
        String upToken = auth.uploadToken(bucket, null, 3600, policy);
        return upToken;
    }


    public JwtTokenResult getJwtToken(String userId) throws RCloudHttpException {
        return rongCloudClient.getJwtToken(userId);
    }

    public void blockStatus(Integer userId, boolean block, Integer minute) throws Exception{

        // 1:封禁, 0:解禁
        int blockStatus = block ? 1 : 0;
        minute = minute == null || minute <= 0 ? BLOCK_MINUTE : minute;
        Users user = queryById(userId);
        if (user == null) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "user");
        }
        if (block){
            rongCloudClient.userBlock(N3d.encode(userId), minute);
        }else {
            rongCloudClient.userBlockRemove(N3d.encode(userId));
        }
        Users users = new Users();
        users.setId(userId);
        users.setBlockStatus(blockStatus);
        users.setBlockStartTime(new Date());
        users.setBlockEndTime(Date.from(LocalDateTime.now().plusMinutes(minute).atZone(ZoneId.systemDefault()).toInstant()));
        usersMapper.updateByPrimaryKeySelective(users);
    }


    public void del(Integer userId) throws ServiceException {

        Users users = queryById(userId);

        Map<String,Object> freeze = new HashMap<>();
        freeze.put("user", freezeUserInfo(users));
        freeze.put("friend",freezeFriendInfo(userId));
        freeze.put("group",freezeGroupInfo(userId));
        freeze.put("ultraGroup",freezeUgInfo(userId));

        FreezeData freezeData= new FreezeData();
        freezeData.setPhone(users.getPhone());
        freezeData.setRegion(users.getRegion());
        freezeData.setUserId(users.getId().longValue());
        freezeData.setFreezeTime(System.currentTimeMillis());
        freezeData.setData(JacksonUtil.toJson(freeze));
        freezeDataMapper.insert(freezeData);

        //删除用户信息
        usersMapper.deleteById(userId);
        //删除黑名单信息
        blackListsMapper.deleteByUserIdOrFriendId(userId);
        //删除好友关系
        friendshipsService.delAllFriendShips(userId);
        //删除群组通讯录
        groupFavsMapper.deleteByUserId(userId);
        //退出所有群
        groupMembersMapper.deleteByMemberId(userId);
        //删除入群审核通知
        groupReceiversMapper.deleteByUserId(userId);
        //退出超级群
        ultraGroupMembersMapper.deleteByMemberId(userId);
        //退出所有超级群用户组
        ugUserGroupMemberMapper.deleteByMemberIds(List.of(userId));
    }


    private Map<String, Object> freezeUserInfo(Users users) {
        try {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("name", users.getNickname());
            userInfo.put("region", users.getRegion());
            userInfo.put("phone", users.getPhone());
            userInfo.put("gender", users.getGender());
            userInfo.put("id", users.getId());
            userInfo.put("idStr", N3d.encode(users.getId()));
            return userInfo;
        } catch (Exception e) {
            log.error("freezeUserInfo error", e);
            return null;
        }
    }

    private List<Map<String, Object>> freezeFriendInfo(Integer userId) {
        try {
            List<Friendships> friendships = friendshipsService.getFriendAgreed(userId);
            if (CollectionUtils.isEmpty(friendships)) {
                return null;
            }
            List<Integer> fiendsIds = friendships.stream().map(Friendships::getFriendId).collect(Collectors.toList());
            List<Users> friends = usersMapper.selectByIds(fiendsIds);
            if (CollectionUtils.isEmpty(friends)) {
                return null;
            }
            return friends.stream().map(u -> {
                Map<String, Object> friendInfo = new HashMap<>();
                try {
                    friendInfo.put("name", u.getNickname());
                    friendInfo.put("region", u.getRegion());
                    friendInfo.put("phone", u.getPhone());
                    friendInfo.put("gender", u.getGender());
                    friendInfo.put("id", u.getId());
                    friendInfo.put("idStr", N3d.encode(u.getId()));
                } catch (Exception e) {
                    log.error("", e);
                }
                return friendInfo;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("freezeFriendInfo error", e);
            return null;
        }
    }

    private List<Map<String, Object>> freezeGroupInfo(Integer userId) {
        try {
            List<GroupMembers> groupMembers = groupMembersMapper.selectByMemberId(userId);
            //群组信息
            if (CollectionUtils.isEmpty(groupMembers)) {
                return null;
            }
            List<Integer> groupIds = groupMembers.stream().map(GroupMembers::getGroupId).collect(Collectors.toList());
            List<Groups> groups = groupsService.queryByIds(groupIds);
            if (CollectionUtils.isEmpty(groups)) {
                return null;
            }
            Map<Integer, GroupMembers> memberMap = groupMembers.stream().collect(Collectors.toMap(GroupMembers::getGroupId, v -> v, (v1, v2) -> v2));
            return groups.stream().map(g -> {
                Map<String, Object> groupInfo = new HashMap<>();
                try {
                    groupInfo.put("name", g.getName());
                    groupInfo.put("groupId", g.getId());
                    groupInfo.put("groupIdStr", N3d.encode(g.getId()));
                    GroupMembers groupM = memberMap.get(g.getId());
                    if (groupM != null) {
                        groupInfo.put("role", groupM.getRole());
                    }
                } catch (Exception e) {
                    log.error("", e);
                }
                return groupInfo;

            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("freezeGroupInfo error", e);
            return null;
        }
    }

    private List<Map<String, Object>> freezeUgInfo(Integer userId) {
        try {
            List<UltraGroupMembers> ultraGroups = ultraGroupMembersMapper.queryGroupMembersWithGroupByMemberId(userId);
            //超级群组信息
            if (CollectionUtils.isEmpty(ultraGroups)) {
                return null;
            }
            return ultraGroups.stream().map(ug -> {
                Map<String, Object> ugInfo = new HashMap<>();
                try {
                    ugInfo.put("name", ug.getGroups().getName());
                    ugInfo.put("groupId", ug.getGroups().getId());
                    ugInfo.put("groupIdStr", N3d.encode(ug.getGroups().getId()));
                    ugInfo.put("role", ug.getRole());
                } catch (Exception e) {
                    log.error("", e);
                }
                return ugInfo;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("freezeUgInfo error", e);
            return null;
        }
    }


    public String getCurrentUserNickNameWithCache(Integer currentUserId) {

        Assert.notNull(currentUserId,"currentUserId is null");

        String nickName = NICK_NAME_CACHE.getIfPresent(String.valueOf(currentUserId));
        if (StringUtils.isEmpty(nickName)) {
            Users users = usersMapper.selectByPrimaryKey(currentUserId);
            if (users != null) {
                nickName = users.getNickname();
                NICK_NAME_CACHE.put(String.valueOf(currentUserId),nickName);
            }
        }
        return nickName;
    }


    public String userStAccount(long createTime, long userId) {
        return "ST" + TimeUtil.format(createTime, "yyyyMMddHHmm") + userId;
    }



}
