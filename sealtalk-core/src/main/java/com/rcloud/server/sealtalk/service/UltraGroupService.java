package com.rcloud.server.sealtalk.service;

import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.ErrorMsg;
import com.rcloud.server.sealtalk.constant.GroupOperationType;
import com.rcloud.server.sealtalk.constant.UltraGroupChannelTypeEnum;
import com.rcloud.server.sealtalk.constant.UltraGroupRole;
import com.rcloud.server.sealtalk.dao.UgChannelUserGroupMapper;
import com.rcloud.server.sealtalk.dao.UgUserGroupMapper;
import com.rcloud.server.sealtalk.dao.UgUserGroupMemberMapper;
import com.rcloud.server.sealtalk.dao.UltraGroupChannelMapper;
import com.rcloud.server.sealtalk.dao.UltraGroupMapper;
import com.rcloud.server.sealtalk.dao.UltraGroupMembersMapper;
import com.rcloud.server.sealtalk.entity.UgChannelUserGroup;
import com.rcloud.server.sealtalk.entity.UgUserGroup;
import com.rcloud.server.sealtalk.entity.UgUserGroupMember;
import com.rcloud.server.sealtalk.entity.UltraGroup;
import com.rcloud.server.sealtalk.entity.UltraGroupChannel;
import com.rcloud.server.sealtalk.entity.UltraGroupMembers;
import com.rcloud.server.sealtalk.entity.Users;
import com.rcloud.server.sealtalk.exception.ParamException;
import com.rcloud.server.sealtalk.exception.RCloudHttpException;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.model.UltraGroupDTO;
import com.rcloud.server.sealtalk.model.dto.UgUserGroupDTO;
import com.rcloud.server.sealtalk.model.dto.UltraGroupChannelDTO;
import com.rcloud.server.sealtalk.model.dto.UltraGroupCreateDTO;
import com.rcloud.server.sealtalk.model.dto.UserDTO;
import com.rcloud.server.sealtalk.rongcloud.RongCloudClient;
import com.rcloud.server.sealtalk.rongcloud.message.DismissUltraGroupMessage;
import com.rcloud.server.sealtalk.util.MiscUtils;
import com.rcloud.server.sealtalk.util.N3d;
import com.rcloud.server.sealtalk.util.ThreadFactoryUtil;
import io.rong.models.Result;
import io.rong.models.message.UltraGroupMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UltraGroupService {

    @Autowired
    private RongCloudClient rongCloudClient;

    @Autowired
    private UsersService usersService;

    @Autowired
    private UltraGroupMapper ultraGroupMapper;
    @Autowired
    private UltraGroupChannelMapper ultraGroupChannelMapper;
    @Autowired
    private UltraGroupMembersMapper ultraGroupMembersMapper;
    @Autowired
    private UgUserGroupMapper ugUserGroupMapper;
    @Autowired
    private UgUserGroupMemberMapper ugUserGroupMemberMapper;
    @Autowired
    private UgChannelUserGroupMapper ugChannelUserGroupMapper;

    private static final AtomicLong CHANNEL_ID_SEQ = new AtomicLong();

    public UltraGroup queryUgById(Integer groupId) {
        return ultraGroupMapper.selectByPrimaryKey(groupId);
    }

    public UltraGroupMembers queryUgMemberByMemberId(Integer groupId, Integer memberId) {
        return ultraGroupMembersMapper.selectByUltraGroupIdAndMemberId(groupId, memberId);
    }


    public Map<Integer, Integer> queryMemberJoinUgCnt(List<Integer> members) {
        List<Map<String, Object>> cntMap = ultraGroupMembersMapper.countByMemberIds(members);
        if (cntMap == null || cntMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, Integer> result = new HashMap<>();
        cntMap.forEach(m -> {
            Long memberId = (Long) m.get("memberId");
            Long cnt = (Long) m.get("count");
            result.put(memberId.intValue(), cnt.intValue());
        });
        return result;
    }


    public Map<Long, Long> queryUserGroupMemberCnt(List<Long> userGroupIds) {
        List<Map<String, Long>> dbResult = ugUserGroupMemberMapper.memberCount(userGroupIds);
        return dbResult == null || dbResult.isEmpty()
                ? Collections.emptyMap()
                : dbResult.stream().collect(Collectors.toMap(m -> m.get("userGroupId"), m -> m.get("num"), (k1, k2) -> k2));
    }


    private UltraGroup checkExistUg(Integer groupId) throws ServiceException {
        UltraGroup ultraGroup = queryUgById(groupId);
        if (ultraGroup == null) {
            throw ServiceException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "ultra group");
        }
        return ultraGroup;
    }

    private UgUserGroup checkExistUserGroup(Integer groupId, Long userGroupId) throws ServiceException {
        UgUserGroup ugUserGroup = ugUserGroupMapper.selectById(userGroupId);
        if (ugUserGroup == null || !groupId.equals(ugUserGroup.getGroupId())) {
            throw ServiceException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "ultra user group");
        }
        return ugUserGroup;
    }

    private UltraGroupMembers checkExistMember(Integer groupId, Integer memberId) throws ServiceException {
        UltraGroupMembers member = queryUgMemberByMemberId(groupId,memberId);
        if (member == null) {
            throw new ServiceException(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_MEMBER);
        }
        return member;
    }

    private void checkCreator(Integer userId, UltraGroup group) throws ServiceException {
        if (!userId.equals(group.getCreatorId())) {
            throw new ServiceException(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_CREATOR);
        }
    }




    /**
     * 创建超级群
     */
    public UltraGroupCreateDTO createUltraGroup(Integer currentUserId, String name, String portraitUri, String summary) throws ServiceException {

        List<UltraGroupMembers> joinedGroups = ultraGroupMembersMapper.selectByMemberId(currentUserId);
        if (joinedGroups != null && joinedGroups.size() >= Constants.MAX_USER_ULTRA_GROUP_OWN_COUNT) {
            throw new ServiceException(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.USER_JOIN_GROUP_OVER);
        }

        var group = new UltraGroup();
        group.setName(name);
        group.setPortraitUri(portraitUri);
        group.setCreatorId(currentUserId);
        group.setSummary(Optional.ofNullable(summary).orElse(""));
        group.setMemberCount(1);
        ultraGroupMapper.insert(group);

        //创建默认频道
        var channel = new UltraGroupChannel();
        channel.setUltraGroupId(group.getId());
        channel.setChannelId(Constants.ULTRA_GROUP_DEFAUT_CHANNEL_ID);
        channel.setChannelName(Constants.ULTRA_GROUP_DEFAUT_CHANNEL_NAME);
        channel.setType(UltraGroupChannelTypeEnum.PUBLIC.getType());
        channel.setCreatorId(currentUserId);
        ultraGroupChannelMapper.insert(channel);

        var member = new UltraGroupMembers();
        member.setMemberId(currentUserId);
        member.setUltraGroupId(group.getId());
        member.setRole(UltraGroupRole.CREATOR.getRole());
        member.setGroupNickname("");
        ultraGroupMembersMapper.insertIgnoreBatch(List.of(member));

        String groupIdStr = N3d.encode(group.getId());
        String currentIdStr = N3d.encode(currentUserId);
        ThreadFactoryUtil.ofVirtual(()->{
            rongCloudClient.createUltraGroup(currentIdStr, groupIdStr, name);
            Thread.sleep(1000);
            rongCloudClient.joinUltraGroup(groupIdStr, currentIdStr);
        });

        UltraGroupCreateDTO ultraGroupCreateDTO = new UltraGroupCreateDTO();
        ultraGroupCreateDTO.setGroupId(groupIdStr);
        ultraGroupCreateDTO.setDefaultChannelId(Constants.ULTRA_GROUP_DEFAUT_CHANNEL_ID);
        ultraGroupCreateDTO.setDefaultChannelName(Constants.ULTRA_GROUP_DEFAUT_CHANNEL_NAME);
        return ultraGroupCreateDTO;
    }


    /**
     * 加入超级群
     */
    public void joinUltraGroup(Integer currentUserId, Integer groupId, List<Integer> memberIds) throws Exception {

        if (memberIds.contains(currentUserId)){
            throw new ParamException(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.MUST_NOT_SELF);
        }

        checkExistUg(groupId);

        Map<Integer, Integer> joinCntMap = queryMemberJoinUgCnt(memberIds);
        boolean overLimit = joinCntMap.values().stream().anyMatch(c -> c >= Constants.MAX_USER_ULTRA_GROUP_OWN_COUNT);
        if (overLimit){
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.GROUP_MEMBER_OVER);
        }
        List<UltraGroupMembers> memberList = memberIds.stream().map(m -> {
            var member = new UltraGroupMembers();
            member.setMemberId(m);
            member.setUltraGroupId(groupId);
            member.setRole(UltraGroupRole.MEMBER.getRole());
            member.setGroupNickname("");
            return member;
        }).toList();
        ultraGroupMembersMapper.insertIgnoreBatch(memberList);
        memberIds.forEach(m -> ThreadFactoryUtil.ofVirtual(() -> rongCloudClient.joinUltraGroup(N3d.encode(groupId), N3d.encode(m))));
    }


    /**
     * 退出超级群
     */
    public void quitUltraGroup(Integer currentUserId, Integer groupId) throws ServiceException {
        UltraGroup ultraGroup = checkExistUg(groupId);

        checkExistMember(groupId, currentUserId);

        ultraGroupMembersMapper.deleteByUltraGroupIdAndMemberIds(groupId, List.of(currentUserId));
        ugUserGroupMemberMapper.deleteByMemberIds(List.of(currentUserId));
        ThreadFactoryUtil.ofVirtual(() -> rongCloudClient.quitUltraGroup(N3d.encode(groupId), N3d.encode(currentUserId)));
    }

    /**
     * 解散超级群
     */
    public void dismissUltraGroup(Integer currentUserId, Integer groupId) throws ServiceException {
        UltraGroup ultraGroup = checkExistUg(groupId);

        checkCreator(currentUserId, ultraGroup);

        ThreadFactoryUtil.ofVirtual(()->{
            //获取用户昵称
            String groupIdStr = N3d.encode(groupId);
            String nickname = usersService.getCurrentUserNickNameWithCache(currentUserId);
            DismissUltraGroupMessage content = new DismissUltraGroupMessage();
            content.setOperatorUserId(N3d.encode(currentUserId));
            content.setOperatorUserNickName(nickname);
            content.setOperation(GroupOperationType.DISMISS.getType());
            content.setMessage("解散群组");

            UltraGroupMessage ultraGroupMessage = new UltraGroupMessage();
            ultraGroupMessage.setSenderId(Constants.GroupNotificationMessage_fromUserId);
            ultraGroupMessage.setTargetId(new String[]{groupIdStr});
            ultraGroupMessage.setContent(content);
            ultraGroupMessage.setObjectName(content.getType());
            rongCloudClient.sendUltraGroupMessage(ultraGroupMessage);
            Thread.sleep(1000);
            rongCloudClient.dismissUltraGroup(groupIdStr);
        });


        List<UgUserGroup> userGroups = ugUserGroupMapper.selectByGroupId(groupId);

        ultraGroupMapper.deleteByPrimaryKey(groupId);
        ultraGroupChannelMapper.deleteByUltraGroupId(groupId);
        ultraGroupMembersMapper.deleteByUltraGroupId(groupId);
        ugUserGroupMapper.deleteByGroupId(groupId);

        if (userGroups!= null && !userGroups.isEmpty()){
            List<Long> userGroupIds = userGroups.stream().map(UgUserGroup::getId).toList();
            ugChannelUserGroupMapper.deleteByUserGroupIds(userGroupIds);
            ugUserGroupMemberMapper.deleteByUserGroupIds(userGroupIds);
        }
    }

    /**
     * 创建超级群频道
     */
    public String createUltraGroupChannel(Integer currentUserId, Integer groupId, String channelName, UltraGroupChannelTypeEnum channelType) throws ServiceException {
        //判断groupId是否存在
        UltraGroup ultraGroup = checkExistUg(groupId);

        checkCreator(currentUserId, ultraGroup);

        //每个超级群创建频道上限数量为20
        List<UltraGroupChannel> busChannels = ultraGroupChannelMapper.selectByUltraGroupId(groupId);

        if (busChannels != null && busChannels.size() >= Constants.MAX_USER_ULTRA_GROUP_CHANNEL_OWN_COUNT) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.UG_CHANNEL_OVER);
        }

        long seq = CHANNEL_ID_SEQ.updateAndGet(prev -> Math.max(System.currentTimeMillis() / 1000, prev));
        String channelId = Constants.UG_BS_ID_PREFIX + seq;


        var channel = new UltraGroupChannel();
        channel.setUltraGroupId(groupId);
        channel.setChannelId(channelId);
        channel.setChannelName(channelName);
        channel.setType(channelType.getType());
        channel.setCreatorId(currentUserId);
        ultraGroupChannelMapper.insert(channel);

        ThreadFactoryUtil.ofVirtual(()->{
            String groupIdStr = N3d.encode(groupId);
            String userStr = N3d.encode(currentUserId);
            rongCloudClient.createUltraGroupChannel(groupIdStr, channelId, channelType.getType());
            Thread.sleep(1000);
            rongCloudClient.ultraGroupChannelMemberAdd(groupIdStr, channelId, List.of(userStr));
        });

        return channelId;
    }

    /**
     * 群频道增加用户
     */
    public Result privateChannelMemberAdd(Integer groupId, String channelId, List<Integer> members) throws ServiceException, RCloudHttpException {
        return rongCloudClient.ultraGroupChannelMemberAdd(N3d.encode(groupId), channelId, MiscUtils.batchEncodeIds(members));
    }

    /**
     * 群频道删除用户
     */
    public Result privateChannelMemberDel(Integer groupId, String channelId, List<Integer> members) throws ServiceException, RCloudHttpException {
        return rongCloudClient.ultraGroupChannelMemberRemove(N3d.encode(groupId), channelId, MiscUtils.batchEncodeIds(members));
    }

    /**
     * 删除群频道
     */
    public void channelDel(Integer groupId, String channelId) {

        ultraGroupChannelMapper.deleteByUltraGroupIdAndChannelIds(groupId, List.of(channelId));
        ugChannelUserGroupMapper.deleteByChannelIds(List.of(channelId));
        ThreadFactoryUtil.ofVirtual(() -> rongCloudClient.ultraGroupDeleteChannel(N3d.encode(groupId), channelId));
    }

    /**
     * 修改频道类型
     */
    public void channelTypeChange(Integer currentUserId, Integer groupId, String channelId, UltraGroupChannelTypeEnum channelType) throws ServiceException {

        //判断groupId是否存在
        UltraGroup ultraGroup = checkExistUg(groupId);
        checkCreator(currentUserId, ultraGroup);

        var channel = new UltraGroupChannel();
        channel.setUltraGroupId(groupId);
        channel.setChannelId(channelId);
        channel.setType(channelType.getType());
        ultraGroupChannelMapper.updateByGroupIdAndChannelIdSelective(channel);

        ThreadFactoryUtil.ofVirtual(() -> rongCloudClient.ultragruopChannelChangeType(N3d.encode(groupId), channelId, channelType.getType()));
    }



    /**
     * 新建用户组
     */
    public Long addUserGroup(Integer userId, Integer groupId, String userGroupName) throws ServiceException {

        UltraGroup ultraGroup = checkExistUg(groupId);
        //判断是不是群成员
        checkCreator(userId, ultraGroup);

        List<UgUserGroup> userGroups = ugUserGroupMapper.selectByGroupId(groupId);

        if (userGroups != null && userGroups.size() >= Constants.MAX_UG_USERGROUP_COUNT) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.UG_USER_GROUP_OVER);
        }

        UgUserGroup insert = new UgUserGroup();
        insert.setGroupId(groupId);
        insert.setName(Optional.ofNullable(userGroupName).orElse(""));
        insert.setCreatorId(userId);
        ugUserGroupMapper.insert(insert);

        UgUserGroupMember member = new UgUserGroupMember();
        member.setUserGroupId(insert.getId());
        member.setMemberId(userId);
        ugUserGroupMemberMapper.insertBatch(List.of(member));

        ThreadFactoryUtil.ofVirtual(() -> {
            String groupIdStr = N3d.encode(groupId);
            String userGroupIdStr = N3d.encode(insert.getId());
            String memberIdStr = N3d.encode(userId);
            rongCloudClient.ugUserGroupAdd(groupIdStr,userGroupIdStr);
            Thread.sleep(1000);
            rongCloudClient.ugUserGroupMemberAdd(groupIdStr, userGroupIdStr, List.of(memberIdStr));
        });

        return insert.getId();
    }


    /**
     * 用户组新增用户
     */
    public void userGroupAddMemeber(Integer currentUser, Integer groupId, Long userGroupId, List<Integer> memberIds) throws ServiceException {

        UltraGroup group = checkExistUg(groupId);

        //用户组是否存在
        UgUserGroup userGroup = checkExistUserGroup(groupId, userGroupId);

        UgUserGroupMember userGroupMember = ugUserGroupMemberMapper.selectByUserGroupIdAndMemberId(userGroupId,currentUser);

        //创建者或者用户组成员才能加人
        if (!currentUser.equals(userGroup.getCreatorId()) && userGroupMember == null) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_MEMBER);
        }

        //只能添加群内的成员
        List<UltraGroupMembers> groupMembers = ultraGroupMembersMapper.queryByMemberIds(groupId, memberIds);
        memberIds = groupMembers.stream().map(UltraGroupMembers::getMemberId).collect(Collectors.toList());
        if (memberIds.isEmpty()) {
            return;
        }

        List<UgUserGroupMember> userGroupMembers = ugUserGroupMemberMapper.selectByUserGroupId(userGroupId);
        Set<Integer> userGroupMemberIds = userGroupMembers.stream().map(UgUserGroupMember::getMemberId).collect(Collectors.toSet());
        userGroupMemberIds.addAll(memberIds);

        if (userGroupMemberIds.size() > Constants.MAX_USERGROUP_MEMBER_COUNT) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.UG_USER_GROUP_MEMBER_OVER);
        }

        List<UgUserGroupMember> insertList = memberIds.stream().map(id -> {
            UgUserGroupMember member = new UgUserGroupMember();
            member.setUserGroupId(userGroupId);
            member.setMemberId(id);
            return member;
        }).collect(Collectors.toList());
        ugUserGroupMemberMapper.insertBatch(insertList);

        ThreadFactoryUtil.ofVirtual(()-> {
            String groupIdStr = N3d.encode(groupId);
            String userGroupIdStr = N3d.encode(userGroupId);
            List<String> memberIdStrs = MiscUtils.batchEncodeIds(insertList.stream().map(UgUserGroupMember::getMemberId).toList());
            //同步到IM
            rongCloudClient.ugUserGroupMemberAdd(groupIdStr, userGroupIdStr, memberIdStrs);
        });

    }



    /**
     * 用户组删除用户
     */
    public void userGroupDelMemeber(Integer currentUser, Integer groupId, Long userGroupId, List<Integer> memberIds) throws ServiceException {

        //群是否存在
        UltraGroup group = checkExistUg(groupId);

        checkCreator(currentUser, group);
        //用户组是否存在
        UgUserGroup userGroup = checkExistUserGroup(groupId, userGroupId);

        ugUserGroupMemberMapper.delBatch(userGroupId, memberIds);

        ThreadFactoryUtil.ofVirtual(()->{
            String groupIdStr = N3d.encode(groupId);
            String userGroupIdStr = N3d.encode(userGroupId);
            List<String> memberIdStrs = MiscUtils.batchEncodeIds(memberIds);
            rongCloudClient.ugUserGroupMemberDel(groupIdStr, userGroupIdStr, memberIdStrs);
        });

    }

    /**
     * 删除用户组
     */
    public void delUserGroup(Integer delUserId, Integer groupId, Long userGroupId) throws ServiceException {

        UltraGroup group = checkExistUg(groupId);
        checkCreator(delUserId, group);

        checkExistUserGroup(groupId,userGroupId);

        //删除db
        ugUserGroupMapper.deleteById(userGroupId);
        ugChannelUserGroupMapper.deleteByUserGroupIds(List.of(userGroupId));
        ugUserGroupMemberMapper.deleteByUserGroupIds(List.of(userGroupId));

        ThreadFactoryUtil.ofVirtual(()->{
            String groupIdStr = N3d.encode(groupId);
            String userGroupIdStr = N3d.encode(userGroupId);
            rongCloudClient.ugUserGroupDel(groupIdStr, userGroupIdStr);
        });
    }



    /**
     * 频道绑定 用户组
     */
    public void channelBindUserGroup(Integer currentUserId, Integer groupId, String channelId, List<Long> userGroupIds) throws Exception {

        UltraGroup ultraGroup = checkExistUg(groupId);
        checkCreator(currentUserId, ultraGroup);
        UltraGroupChannel ugChannel = ultraGroupChannelMapper.selectByUltraGroupIdAndChannelId(groupId, channelId);
        if (ugChannel == null) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST,"ultra group channel");
        }

        List<Long> existUserGroupIds = ugUserGroupMapper.queryByGroupIdAndIds(groupId,userGroupIds);
        if (existUserGroupIds == null || existUserGroupIds.isEmpty()) {
            return;
        }

        List<UgChannelUserGroup> bind = ugChannelUserGroupMapper.selectByChannelId(channelId);
        Set<Long> bindUserGroupIds = bind.stream().map(UgChannelUserGroup::getUserGroupId).collect(Collectors.toSet());
        bindUserGroupIds.addAll(userGroupIds);
        if (bindUserGroupIds.size() > Constants.MAX_CHANNEL_USERGROUP_COUNT) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.UG_CHANNEL_BIND_USERGROUP_OVER);
        }

        List<UgChannelUserGroup> insertList = userGroupIds.stream().map(u -> {
            UgChannelUserGroup tmp = new UgChannelUserGroup();
            tmp.setChannelId(channelId);
            tmp.setUserGroupId(u);
            return tmp;
        }).toList();
        ugChannelUserGroupMapper.insertBatch(insertList);

        ThreadFactoryUtil.ofVirtual(()->{
            String groupIdStr = N3d.encode(groupId);
            List<String> userGroupIdStrs = MiscUtils.batchEncodeLongIds(userGroupIds);
            rongCloudClient.ugChannelBindUserGroup(groupIdStr, channelId, userGroupIdStrs);
        });
    }

    /**
     * 频道解绑 用户组
     */
    public void channelUnBindUserGroup(Integer currentUserId, Integer groupId, String channelId, List<Long> userGroupIds) throws ServiceException {

        UltraGroup ultraGroup = checkExistUg(groupId);
        checkCreator(currentUserId, ultraGroup);
        ugChannelUserGroupMapper.delBatch(channelId, userGroupIds);
        ThreadFactoryUtil.ofVirtual(()->{
            String groupIdStr = N3d.encode(groupId);
            List<String> userGroupIdStrs = MiscUtils.batchEncodeLongIds(userGroupIds);
            rongCloudClient.ugChannelUnBindUserGroup(groupIdStr, channelId, userGroupIdStrs);
        });
    }




    public List<UltraGroupDTO> getUltragroups(Integer currentUserId) throws ServiceException {

        List<UltraGroupDTO> ultraGroupDTOList = new ArrayList<>();
        List<UltraGroupMembers> ultraGroupMembersList = ultraGroupMembersMapper.queryGroupMembersWithGroupByMemberId(currentUserId);
        if (ultraGroupMembersList != null) {
            for (UltraGroupMembers member : ultraGroupMembersList) {
                UltraGroupDTO dto = new UltraGroupDTO();
                dto.setGroupId(N3d.encode(member.getUltraGroupId()));
                dto.setGroupName(member.getGroups().getName());
                dto.setCreatorId(N3d.encode(member.getGroups().getCreatorId()));
                dto.setPortraitUri(member.getGroups().getPortraitUri());
                dto.setSummary(member.getGroups().getSummary());
                dto.setCreatedTime(member.getCreatedAt().getTime());
                ultraGroupDTOList.add(dto);
            }
        }
        return ultraGroupDTOList;
    }


    public List<UltraGroupMembers> getMembers(Integer currentUserId, Integer groupId, Integer pageNum, Integer limit) throws ServiceException {

        checkExistMember(groupId, currentUserId);

        Integer offset = (pageNum - 1) * limit;
        return ultraGroupMembersMapper.queryGroupMembersWithUsersByGroupId(groupId, offset, limit);
    }


    public List<UltraGroupChannelDTO> getUltraGroupChannels(Integer currentUserId, Integer groupId, Integer pageNum, Integer limit) throws ServiceException {

        checkExistMember(groupId, currentUserId);

        List<UltraGroupChannelDTO> ultraGroupChannelDTOList = new ArrayList<>();
        Integer offset = (pageNum - 1) * limit;
        List<UltraGroupChannel> ultraGroupChannelList = ultraGroupChannelMapper.queryUltraGroupChannelListByPage(groupId, offset, limit);
        if(ultraGroupChannelList!=null){
            for(UltraGroupChannel channel:ultraGroupChannelList){
                UltraGroupChannelDTO dto = new UltraGroupChannelDTO();
                dto.setChannelId(channel.getChannelId());
                dto.setChannelName(channel.getChannelName());
                dto.setType(channel.getType());
                dto.setCreatedAt(channel.getCreatedAt());
                ultraGroupChannelDTOList.add(dto);
            }
        }
        return ultraGroupChannelDTOList;
    }




    public List<String> privateChannelMembersQuery(String groupId, String channelId, Integer page, Integer pageSize) throws RCloudHttpException {
        return rongCloudClient.ultraGroupChannelMemberQuery(groupId, channelId, page, pageSize).getMembers();
    }



    /**
     * 分页查询用户组信息
     */
    public List<UgUserGroupDTO> queryUserGroup(Integer currentUser, Integer groupId, int page, int pageSize) throws ServiceException {

        //群成员才能查看
        checkExistMember(groupId, currentUser);

        int offset = (page - 1) * pageSize;
        List<UgUserGroup> pageResult =  ugUserGroupMapper.queryByGroupIdPage(groupId, offset, pageSize);

        if (CollectionUtils.isEmpty(pageResult)) {
            return Collections.emptyList();
        }
        List<Long> userGroupIds = pageResult.stream().map(UgUserGroup::getId).toList();
        Map<Long, Long> memberCountMap = queryUserGroupMemberCnt(userGroupIds);
        List<UgUserGroupDTO> result = new ArrayList<>();
        for (UgUserGroup u : pageResult) {
            UgUserGroupDTO dto = new UgUserGroupDTO();
            dto.setUserGroupId(N3d.encode(u.getId()));
            dto.setUserGroupName(u.getName());
            dto.setMemberCount(memberCountMap.getOrDefault(u.getId(), 0L));
            result.add(dto);
        }
        return result;
    }

    /**
     * 分页查询频道绑定的用户组信息
     */
    public List<UgUserGroupDTO> queryChannelBindUserGroup(Integer currentUser, Integer groupId, String channelId, int page, int pageSize) throws Exception {

        //群成员才能查看
        checkExistMember(groupId, currentUser);

        UltraGroupChannel ugChannel = ultraGroupChannelMapper.selectByUltraGroupIdAndChannelId(groupId, channelId);
        if (ugChannel == null) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST,"channel");
        }

        int offset = (page - 1) * pageSize;
        List<UgUserGroup> pageResult = ugChannelUserGroupMapper.queryByChannelIdPage(channelId, offset, pageSize);
        if (CollectionUtils.isEmpty(pageResult)) {
            return Collections.emptyList();
        }
        List<Long> userGroupIds = pageResult.stream().map(UgUserGroup::getId).toList();
        Map<Long, Long> memberCountMap = queryUserGroupMemberCnt(userGroupIds);
        List<UgUserGroupDTO> result = new ArrayList<>();
        for (UgUserGroup u : pageResult) {
            UgUserGroupDTO dto = new UgUserGroupDTO();
            dto.setUserGroupId(N3d.encode(u.getId()));
            dto.setUserGroupName(u.getName());
            dto.setMemberCount(memberCountMap.getOrDefault(u.getId(), 0L));
            result.add(dto);
        }
        return result;
    }

    /**
     * 分页查询用户组内的成员信息
     */
    public List<UserDTO> queryUserGroupMemebers(Integer currentUser, Integer groupId, Long userGroupId, int page, int pageSize) throws ServiceException {

        //群成员才能查看
        checkExistMember(groupId, currentUser);

        //用户组是否存在
        checkExistUserGroup(groupId, userGroupId);

        int offset = (page - 1) * pageSize;
        List<Users> users  = ugUserGroupMemberMapper.queryByUserGroupIdPage(userGroupId, offset, pageSize);
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyList();
        }
        List<UserDTO> result = new ArrayList<>();
        for (Users u : users) {
            UserDTO dto = new UserDTO();
            dto.setId(N3d.encode(u.getId()));
            dto.setNickname(u.getNickname());
            dto.setPortraitUri(u.getPortraitUri());
            dto.setGender(u.getGender());
            result.add(dto);
        }
        return result;
    }

}
