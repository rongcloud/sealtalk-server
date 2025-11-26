package com.rcloud.server.sealtalk.service;

import com.google.common.collect.ImmutableList;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.ErrorMsg;
import com.rcloud.server.sealtalk.constant.FriendShipStatus;
import com.rcloud.server.sealtalk.constant.GroupAuth.JoinAuth;
import com.rcloud.server.sealtalk.constant.GroupOperationType;
import com.rcloud.server.sealtalk.constant.GroupRole;
import com.rcloud.server.sealtalk.constant.UserAddStatus;
import com.rcloud.server.sealtalk.constant.UserAuth.JoinGroupAuth;
import com.rcloud.server.sealtalk.dao.GroupBulletinsMapper;
import com.rcloud.server.sealtalk.dao.GroupExitedListsMapper;
import com.rcloud.server.sealtalk.dao.GroupFavsMapper;
import com.rcloud.server.sealtalk.dao.GroupMembersMapper;
import com.rcloud.server.sealtalk.dao.GroupReceiversMapper;
import com.rcloud.server.sealtalk.dao.GroupsMapper;
import com.rcloud.server.sealtalk.entity.Friendships;
import com.rcloud.server.sealtalk.entity.GroupBulletins;
import com.rcloud.server.sealtalk.entity.GroupExitedLists;
import com.rcloud.server.sealtalk.entity.GroupFavs;
import com.rcloud.server.sealtalk.entity.GroupMembers;
import com.rcloud.server.sealtalk.entity.GroupReceivers;
import com.rcloud.server.sealtalk.entity.Groups;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.rcloud.server.sealtalk.entity.Users;
import com.rcloud.server.sealtalk.exception.ParamException;
import com.rcloud.server.sealtalk.exception.RCloudHttpException;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.model.dto.GroupAddStatusDTO;
import com.rcloud.server.sealtalk.model.dto.UserStatusDTO;
import com.rcloud.server.sealtalk.rongcloud.RongCloudClient;
import com.rcloud.server.sealtalk.rongcloud.message.CustomerClearGroupMessage;
import com.rcloud.server.sealtalk.rongcloud.message.CustomerConNtfMessage;
import com.rcloud.server.sealtalk.rongcloud.message.CustomerGroupApplyMessage;
import com.rcloud.server.sealtalk.rongcloud.message.CustomerGroupNtfMessage;
import com.rcloud.server.sealtalk.util.MiscUtils;
import com.rcloud.server.sealtalk.util.N3d;
import com.rcloud.server.sealtalk.util.ThreadFactoryUtil;
import io.rong.messages.TxtMessage;
import io.rong.models.message.GroupMessage;
import io.rong.models.message.MentionMessage;
import io.rong.models.message.MentionMessageContent;
import io.rong.models.message.MentionedInfo;
import io.rong.models.message.PrivateMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GroupsService {

    @Autowired
    private GroupsMapper groupsMapper;

    @Autowired
    private RongCloudClient rongCloudClient;

    @Autowired
    private GroupMembersMapper groupMembersMapper;

    @Autowired
    private UsersService usersService;

    @Autowired
    private GroupReceiversMapper groupReceiversMapper;

    @Autowired
    private GroupExitedListsMapper groupExitedListsMapper;

    @Autowired
    private GroupBulletinsMapper groupBulletinsMapper;

    @Autowired
    private GroupFavsMapper groupFavsMapper;

    @Autowired
    private FriendshipsService friendshipsService;

    /**
     * 根据群Id获取群信息
     */
    public Groups queryGroupById(Integer groupId) {
        return groupsMapper.selectByPrimaryKey(groupId);
    }

    public List<Groups> queryByIds(List<Integer> groupIds) {
        return groupsMapper.selectByIds(groupIds);
    }


    private List<Integer> queryManagerIds(Integer groupId){
        List<GroupMembers> managers = groupMembersMapper.selectByGroupIdAndRoles(groupId, List.of(GroupRole.CREATOR.getRole(), GroupRole.MANAGER.getRole()));

        if (managers == null || managers.isEmpty()){
            return Collections.emptyList();
        }
        return managers.stream().map(GroupMembers::getMemberId).toList();
    }



    /**
     * 获取群成员信息
     */
    public GroupMembers queryGroupMember(Integer groupId, Integer memberId) {
        return groupMembersMapper.selectByGroupIdAndMemberId(groupId, memberId);
    }



    /**
     * 创建群组
     */
    public GroupAddStatusDTO createGroup(Integer currentUserId, String groupName, String portraitUri, List<Integer> memberIds) throws Exception {
        // 1. 验证用户创建群组数量是否达到上限
        List<GroupMembers> creatorJoinGroups = groupMembersMapper.selectByMemberId(currentUserId);
        if (creatorJoinGroups != null && creatorJoinGroups.size() >= Constants.MAX_USER_GROUP_OWN_COUNT) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.USER_JOIN_GROUP_OVER);
        }

        // 2. 筛选出要邀请的好友
        List<Integer> friendIds = getFriendIds(currentUserId, memberIds);
        if (friendIds.isEmpty()) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_FRIEND);
        }

        // 3. 根据是否需要入群验证将用户分组
        Map<Boolean, List<Integer>> partitionedUsers = partitionUsersByGroupVerify(friendIds);
        List<Integer> verifyNeededUserList = Optional.ofNullable(partitionedUsers.get(true)).orElse(Collections.emptyList());
        List<Integer> verifyNotNeededUserList = Optional.ofNullable(partitionedUsers.get(false)).orElse(Collections.emptyList());

        // 4. 创建群组
        Groups group = createAndSaveGroup(currentUserId, groupName, portraitUri, verifyNotNeededUserList.size());

        // 5. 处理无需验证直接入群的用户
        List<Integer> directJoinUserIds = Stream.concat(verifyNotNeededUserList.stream(), Stream.of(currentUserId))
                .collect(Collectors.toList());
        //直接加群操作
        joinGroup(currentUserId, directJoinUserIds, group, true);

        List<UserStatusDTO> userStatusDTOList = directJoinUserIds.stream()
                .map(id -> new UserStatusDTO(id, UserAddStatus.GROUP_ADDED.getCode()))
                .collect(Collectors.toList());

        // 6. 处理需要验证的用户
        if (!verifyNeededUserList.isEmpty()) {
            saveGroupReceiverOfMember(group, currentUserId, verifyNeededUserList);
            sendGroupApplyMessage(currentUserId, verifyNeededUserList, group.getId(), group.getName(),
                    GroupReceivers.GROUP_RECEIVE_STATUS_WAIT, GroupReceivers.GROUP_RECEIVE_TYPE_MEMBER);

            userStatusDTOList.addAll(verifyNeededUserList.stream()
                    .map(id -> new UserStatusDTO(id, UserAddStatus.WAIT_MEMBER.getCode()))
                    .toList());
        }

        // 7. 构建并返回结果
        return new GroupAddStatusDTO(group.getId(), userStatusDTOList);
    }



    /**
     * 添加群成员
     */
    public List<UserStatusDTO> addMember(Integer currentUserId, Integer groupId, List<Integer> memberIds) throws Exception {

        Groups groupInfo = queryGroupById(groupId);
        if (groupInfo == null) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "group");
        }
        if (groupInfo.getMemberCount() >= groupInfo.getMaxMemberCount()) {
            throw ServiceException.buildError(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.GROUP_MEMBER_OVER, "group");
        }

        GroupMembers groupMembers = groupMembersMapper.selectByGroupIdAndMemberId(groupId, currentUserId);
        if (groupMembers == null) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_MEMBER);
        }

        //邀请人是否是管理员
        boolean isManager = GroupRole.CREATOR.getRole().equals(groupMembers.getRole()) || GroupRole.MANAGER.getRole().equals(groupMembers.getRole());
        //群组是否开启了入群认证
        boolean isGroupRequireAuth = JoinAuth.REQUIRE_AUTH.getAuth().equals(groupInfo.getCertiStatus());
        //只能邀请自己的好友
        // 2. 筛选出要邀请的好友
        List<Integer> friendIds = getFriendIds(currentUserId, memberIds);
        if (friendIds.isEmpty()) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_FRIEND);
        }

        Map<Boolean, List<Integer>> partitionedUsers = partitionUsersByGroupVerify(friendIds);
        List<Integer> userJoinRequireAuth = Optional.ofNullable(partitionedUsers.get(true)).orElse(Collections.emptyList());;
        List<Integer> userJoinNoRequireAuth = Optional.ofNullable(partitionedUsers.get(false)).orElse(Collections.emptyList());

        List<UserStatusDTO> statusDTOS = new ArrayList<>();

        //处理 verifyOpendUserIds 开启认证的用户，更新为待用户处理状态, 并批量发消息
        if (userJoinRequireAuth.size() > 0) {

            saveGroupReceiverOfMember(groupInfo,currentUserId, userJoinRequireAuth);
            //发送好友邀请消息
            sendGroupApplyMessage(currentUserId, userJoinRequireAuth, groupInfo.getId(), groupInfo.getName(), GroupReceivers.GROUP_RECEIVE_STATUS_WAIT, GroupReceivers.GROUP_RECEIVE_TYPE_MEMBER);

            statusDTOS.addAll(userJoinRequireAuth.stream().map(id -> new UserStatusDTO(id, UserAddStatus.WAIT_MEMBER.getCode())).toList());
        }

        //处理 verifyClosedUserIds 关闭认证的用户
        if (userJoinNoRequireAuth.size() > 0) {
            //当自己不是管理者 && 群组开启了入群认证时, 需要管理员同意
            if (!isManager && isGroupRequireAuth) {
                //更新为待管理员审批状态, 更新多个, 发消息
                List<Integer> managerIds = queryManagerIds(groupId);
                //更新为待管理员处理状态, 并批量发消息
                saveGroupReceiverOfManager(groupInfo, currentUserId, userJoinNoRequireAuth, managerIds);
                //给群管理发送入群申请消息
                sendGroupApplyMessage(currentUserId, managerIds, groupInfo.getId(), groupInfo.getName(), GroupReceivers.GROUP_RECEIVE_STATUS_WAIT, GroupReceivers.GROUP_RECEIVE_TYPE_MANAGER);

                statusDTOS.addAll(userJoinNoRequireAuth.stream().map(id -> new UserStatusDTO(id, UserAddStatus.WAIT_MANAGER.getCode())).toList());
            } else {
                //如果没有开启群验证或者有管理员角色 直接加群
                statusDTOS.addAll(userJoinNoRequireAuth.stream().map(id -> new UserStatusDTO(id, UserAddStatus.GROUP_ADDED.getCode())).toList());
                //加群发通知
                joinGroup(currentUserId, userJoinNoRequireAuth, groupInfo, false);
            }
            groupExitedListsMapper.deleteByGroupIdAndQuitUserIds(groupId, userJoinNoRequireAuth);
        }
        return statusDTOS;
    }



    /**
     * 同意群邀请
     */
    public void agree(Integer currentUserId, Integer groupId, Integer receiverId, String status) throws Exception,RCloudHttpException {
        //是否为 被邀请者同意或忽略
        boolean isReceiverOpt = currentUserId.equals(receiverId);
        //是否同意
        boolean isAgree = GroupReceivers.GROUP_RECEIVE_STATUS_AGREED.equals(Integer.valueOf(status));
        //是普通成员还是管理员同意或忽略
        Integer type = isReceiverOpt ? GroupReceivers.GROUP_RECEIVE_TYPE_MEMBER : GroupReceivers.GROUP_RECEIVE_TYPE_MANAGER;//是普通成员同意，还是管理员同意

        if (type.equals(GroupReceivers.GROUP_RECEIVE_TYPE_MANAGER)){
            GroupMembers currentUser = groupMembersMapper.selectByGroupIdAndMemberId(groupId,currentUserId);
            if (currentUser == null || !isManagerRole(currentUser.getRole())){
                throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_MANAGER);
            }
        }

        Groups groupInfo = groupsMapper.selectByPrimaryKey(groupId);
        if (groupInfo.getMemberCount() >= groupInfo.getMaxMemberCount()) {
            throw ServiceException.buildError(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.GROUP_MEMBER_OVER, "group");
        }

        GroupReceivers receivers = groupReceiversMapper.selectLatest(currentUserId, groupId, receiverId, type, GroupReceivers.GROUP_RECEIVE_STATUS_WAIT);
        if (receivers == null) {
            throw ServiceException.buildError(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "apply");
        }

        groupReceiversMapper.updateStatus(groupId,receiverId,type,GroupReceivers.GROUP_RECEIVE_STATUS_WAIT, GroupReceivers.GROUP_RECEIVE_STATUS_IGNORE);


        //发送群组申请通知消息
        sendGroupApplyMessage(currentUserId, ImmutableList.of(currentUserId), groupId, groupInfo.getName(), type, Integer.valueOf(status));

        if (!isAgree) {
            //不同意直接返回
            return;
        }

        boolean needManagerVerify = false;
        if (isReceiverOpt && Groups.CERTI_STATUS_OPENED.equals(groupInfo.getCertiStatus())) {
            //判断邀请人是不是管理员
            GroupMembers managers = groupMembersMapper.selectByGroupIdAndMemberId(groupId, receivers.getRequesterId());
            if (managers == null || !isManagerRole(managers.getRole())) {
                needManagerVerify = true;
            }
        }
        //如果为被邀请者同意且群组开启了认证, 新增为管理员认证
        if (needManagerVerify) {
            //查询出群组的管理员们
            List<Integer> memberIds = queryManagerIds(groupId);

            saveGroupReceiverOfManager(groupInfo,receivers.getRequesterId(),List.of(receiverId),memberIds);
            //发送好友邀请消息
            sendGroupApplyMessage(receivers.getRequesterId(), memberIds, groupInfo.getId(), groupInfo.getName(), GroupReceivers.GROUP_RECEIVE_STATUS_WAIT, GroupReceivers.GROUP_RECEIVE_TYPE_MANAGER);
        } else {
            //如果为群组未开启认证 或 为管理员同意, 直接加群
            joinGroup(groupInfo.getCreatorId(),ImmutableList.of(receiverId),groupInfo,false);
        }
        //删除群组退出列表
        groupExitedListsMapper.deleteByGroupIdAndQuitUserIds(groupId, List.of(receiverId));
    }




    /**
     * 用户加入群组
     */
    public void joinGroup(Integer currentUserId, Integer groupId) throws Exception {

        Groups groups = groupsMapper.selectByPrimaryKey(groupId);
        if (groups == null) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "group");
        }
        if (groups.getMemberCount() >= groups.getMaxMemberCount()) {
            throw ServiceException.buildError(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.GROUP_MEMBER_OVER, "group");
        }
        joinGroup(currentUserId, List.of(currentUserId), groups, false);
    }


    /**
     * 复制群
     */
    public GroupAddStatusDTO copyGroup(Integer currentUserId, Integer groupId, String groupName, String portraitUri) throws Exception {

        //根据groupId查询群组信息
        Groups groupInfo = queryGroupById(groupId);
        if (groupInfo == null) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "group");
        }
        if (System.currentTimeMillis() - groupInfo.getCreatedAt().getTime() <= Constants.ONE_HOUR_MILLION_SECONDS){
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), "The group cannot be copied during the current time period.");
        }
        if (groupInfo.getCopiedTime() != null && System.currentTimeMillis() - groupInfo.getCopiedTime() <= Constants.ONE_HOUR_MILLION_SECONDS){
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), "The group cannot be copied during the current time period.");
        }
        List<GroupMembers> members = groupMembersMapper.selectByGroupId(groupId);
        if (members == null || members.size() <= 1) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), "Group is empty.");
        }
        return createGroup(currentUserId, groupName, portraitUri, members.stream().map(GroupMembers::getMemberId).toList());
    }





    private List<Integer> getFriendIds(Integer currentUserId, List<Integer> memberIds) {
        List<Integer> joinUserIdsList = memberIds.stream().filter(id -> !id.equals(currentUserId)).collect(Collectors.toList());
        if (joinUserIdsList.isEmpty()) {
            return Collections.emptyList();
        }
        return friendshipsService.getFriendList(currentUserId, joinUserIdsList, FriendShipStatus.AGREED)
                .stream()
                .map(Friendships::getFriendId)
                .toList();
    }

    private Map<Boolean, List<Integer>> partitionUsersByGroupVerify(List<Integer> userIds) {
        if (userIds.isEmpty()) {
            return Map.of(true, Collections.emptyList(), false, Collections.emptyList());
        }
        List<Users> usersList = usersService.queryByIds(userIds);
        return usersList.stream()
                .collect(Collectors.partitioningBy(
                        user -> JoinGroupAuth.REQUIRE_AUTH.getAuth().equals(user.getGroupVerify()),
                        Collectors.mapping(Users::getId, Collectors.toList())
                ));
    }

    private Groups createAndSaveGroup(Integer currentUserId, String groupName, String portraitUri, int initialMemberCount) {
        Groups groups = new Groups();
        groups.setName(groupName);
        groups.setPortraitUri(portraitUri);
        groups.setMemberCount(initialMemberCount + 1); // +1 for the creator
        groups.setCreatorId(currentUserId);
        groups.setMemberProtection(1); // 默认开启群保护
        groupsMapper.insertSelective(groups);
        return groups;
    }


    private void joinGroup(Integer userId, Collection<Integer> memberIds, Groups group, boolean isCreate) {
        Set<Integer> memberSet = new HashSet<>(memberIds);
        List<GroupMembers> members = memberSet.stream().map(id -> {
            GroupMembers groupMembers = new GroupMembers();
            groupMembers.setGroupId(group.getId());
            groupMembers.setMemberId(id);
            groupMembers.setRole(id.equals(group.getCreatorId()) ? GroupRole.CREATOR.getRole() : GroupRole.MEMBER.getRole());
            return groupMembers;
        }).collect(Collectors.toList());
        groupMembersMapper.insertIgnoreBatch(members);

        Thread.ofVirtual().start(() -> {
            //更新申请/邀请记录
            groupReceiversMapper.updateStatusByReceiverIds(group.getId(), new ArrayList<>(memberIds), GroupReceivers.GROUP_RECEIVE_STATUS_WAIT, GroupReceivers.GROUP_RECEIVE_STATUS_AGREED);

            //更新群成员数量
            int memberCnt = groupMembersMapper.groupMemberCnt(group.getId());
            Groups update = new Groups();
            update.setId(group.getId());
            update.setMemberCount(memberCnt);
            groupsMapper.updateByPrimaryKeySelective(update);
            try {
                List<String> memberIdStrs = memberIds.stream().map(i -> {
                    try {
                        return N3d.encode(i);
                    } catch (Exception e) {
                        return null;
                    }
                }).filter(StringUtils::isNotBlank).toList();

                Users operator = usersService.queryById(userId);
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("operatorNickname", operator.getNickname());
                messageData.put("targetGroupName", group.getName());
                messageData.put("timestamp", System.currentTimeMillis());
                if (isCreate) {
                    rongCloudClient.createGroup(N3d.encode(group.getId()), group.getName(), N3d.encode(userId), memberIdStrs);
                    Thread.sleep(1000);
                    sendGroupNotificationMessageBySystem(group.getId(), messageData, userId, GroupOperationType.CREATE);
                    return;
                }
                messageData.put("targetUserIds", memberIdStrs);
                //调用融云加入群组
                rongCloudClient.joinGroup(N3d.encode(group.getId()), memberIdStrs);
                Thread.sleep(1000);
                sendGroupNotificationMessageBySystem(group.getId(), messageData, userId, GroupOperationType.Add);
            } catch (Exception e) {
                log.error("", e);
            }
        });
    }





    /**
     * 发送群组通知： 操作人固定=》"__system__"
     */
    private void sendGroupNotificationMessageBySystem(Integer groupId, Map<String, Object> messageData, Integer operatorUserId, GroupOperationType groupOperationType) throws Exception, RCloudHttpException {

        CustomerGroupNtfMessage customerGroupNtfMessage = new CustomerGroupNtfMessage();
        customerGroupNtfMessage.setOperatorUserId(N3d.encode(operatorUserId));
        customerGroupNtfMessage.setOperation(groupOperationType.getType());
        customerGroupNtfMessage.setData(messageData);

        GroupMessage groupMessage = new GroupMessage();
        groupMessage.setSenderId(Constants.GroupNotificationMessage_fromUserId);
        groupMessage.setTargetId(new String[]{N3d.encode(groupId)});
        groupMessage.setObjectName(customerGroupNtfMessage.getType());
        groupMessage.setContent(customerGroupNtfMessage);
        rongCloudClient.sendGroupMessage(groupMessage);
    }

    /**
     * 发送群申请消息
     */
    private void sendGroupApplyMessage(Integer requesterId, List<Integer> operatorUserIdList, Integer targetGroupId, String targetGroupName, Integer status, Integer type) throws Exception, RCloudHttpException {

        //构建消息内容
        CustomerGroupApplyMessage grpApplyMessage = new CustomerGroupApplyMessage();
        grpApplyMessage.setOperatorUserId(N3d.encode(requesterId));
        grpApplyMessage.setOperation(GroupOperationType.INVITE.getType());

        String requesterName = usersService.getCurrentUserNickNameWithCache(requesterId);

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("operatorNickname", requesterName);
        messageData.put("targetGroupId", N3d.encode(targetGroupId));
        messageData.put("targetGroupName", targetGroupName == null ? "" : targetGroupName);
        messageData.put("status", status);
        messageData.put("type", type);
        messageData.put("timestamp", System.currentTimeMillis());
        grpApplyMessage.setData(messageData);

        //构建消息内容
        PrivateMessage privateMessage = new PrivateMessage();
        privateMessage.setSenderId(Constants.GrpApplyMessage_fromUserId);
        privateMessage.setTargetId(MiscUtils.batchEncodeIds(operatorUserIdList).toArray(new String[0]));
        privateMessage.setObjectName(grpApplyMessage.getType());
        privateMessage.setContent(grpApplyMessage);

        //发送群组申请消息，走的单聊消息
        rongCloudClient.sendPrivateMessage(privateMessage);
    }


    /**
     * 被邀请人的邀请记录
     * @param requestId 邀请人
     * @param members 被邀请人列表
     */
    private void saveGroupReceiverOfMember(Groups group, Integer requestId, List<Integer> members) {

        //将被邀请人所有没有同意的记录删除
        groupReceiversMapper.deleteByGroupIdAndUserIds(group.getId(), members);
        //在创建新的邀请记录
        List<GroupReceivers> receivers =  members.stream().map(id -> {
                    GroupReceivers tmp = new GroupReceivers();
                    tmp.setUserId(id);
                    tmp.setGroupId(group.getId());
                    tmp.setGroupName(group.getName());
                    tmp.setGroupPortraitUri(group.getPortraitUri());
                    tmp.setRequesterId(requestId);
                    tmp.setReceiverId(id);
                    tmp.setStatus(GroupReceivers.GROUP_RECEIVE_STATUS_WAIT);
                    tmp.setType(GroupRole.MEMBER.getRole());
                    return tmp;
        }).toList();
        groupReceiversMapper.insertBatch(receivers);
    }


    /**
     * 保存管理员视角下的申请列表
     * @param requestId 邀请人
     * @param members 被邀请人列表
     * @param managers 管理员列表
     */
    private void saveGroupReceiverOfManager(Groups group, Integer requestId, List<Integer> members, List<Integer> managers) {
        groupReceiversMapper.deleteByGroupIdAndUserIdsAndReceiverIds(group.getId(), managers, members);
        List<GroupReceivers> list = new ArrayList<>();
        for (Integer managerId : managers) {
            for (Integer memeberId : members) {
                GroupReceivers tmp = new GroupReceivers();
                tmp.setUserId(managerId);
                tmp.setGroupId(group.getId());
                tmp.setGroupName(group.getName());
                tmp.setGroupPortraitUri(group.getPortraitUri());
                tmp.setRequesterId(requestId);
                tmp.setReceiverId(memeberId);
                tmp.setStatus(GroupReceivers.GROUP_RECEIVE_STATUS_WAIT);
                tmp.setType(GroupRole.MANAGER.getRole());
                list.add(tmp);
            }
        }
        groupReceiversMapper.insertBatch(list);
    }


    /**
     * 更新群信息
     */
    public void updateGroupInfo(Integer userId, Groups updateGroups) throws Exception {

        Groups groupInfo = queryGroupById(updateGroups.getId());
        if (groupInfo == null){
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "group");
        }
        GroupMembers groupMember = groupMembersMapper.queryGroupMembersWithGroupByGroupIdAndMemberId(updateGroups.getId(), userId);
        if (groupMember == null || !isManagerRole(groupMember.getRole())) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_MANAGER);
        }

        groupsMapper.updateByPrimaryKeySelective(updateGroups);

        String userIdStr = N3d.encode(userId);
        String groupIdStr = N3d.encode(updateGroups.getId());
        if (updateGroups.getMemberProtection() != null){
            String operation = updateGroups.getMemberProtection() == 0 ? "closeMemberProtection" : "openMemberProtection";
            //发送群组通知
            CustomerGroupNtfMessage customerGroupNtfMessage = new CustomerGroupNtfMessage();
            customerGroupNtfMessage.setOperatorUserId(userIdStr);
            customerGroupNtfMessage.setOperation(operation);
            GroupMessage groupMessage = new GroupMessage();
            groupMessage.setTargetId(new String[]{groupIdStr});
            groupMessage.setSenderId(userIdStr);
            groupMessage.setObjectName(customerGroupNtfMessage.getType());
            groupMessage.setContent(customerGroupNtfMessage);
            groupMessage.setIsIncludeSender(1);
            ThreadFactoryUtil.ofVirtual(() -> rongCloudClient.sendGroupMessage(groupMessage));
        }

        if (updateGroups.getClearStatus() != null){
            String operation = updateGroups.getClearStatus() == 0 ? "closeRegularClear" : "openRegularClear";
            updateGroups.setClearTimeAt(System.currentTimeMillis());

            CustomerConNtfMessage customerConNtfMessage = new CustomerConNtfMessage();
            customerConNtfMessage.setOperatorUserId(userIdStr);
            customerConNtfMessage.setOperation(operation);

            GroupMessage groupMessage = new GroupMessage();
            groupMessage.setTargetId(new String[]{groupIdStr});
            groupMessage.setSenderId(userIdStr);
            groupMessage.setObjectName(customerConNtfMessage.getType());
            groupMessage.setContent(customerConNtfMessage);
            groupMessage.setIsIncludeSender(1);

            ThreadFactoryUtil.ofVirtual(() -> rongCloudClient.sendGroupMessage(groupMessage));
        }

        if (updateGroups.getName() != null){
            ThreadFactoryUtil.ofVirtual(()-> rongCloudClient.refreshGroupName(groupIdStr, updateGroups.getName()));
            ThreadFactoryUtil.ofVirtual(()-> {
                String nickname = usersService.getCurrentUserNickNameWithCache(userId);
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("operatorNickname", nickname);
                messageData.put("targetGroupName", updateGroups.getName());
                messageData.put("timestamp", System.currentTimeMillis());
                //发送群组重命名通知
                sendGroupNotificationMessageBySystem(updateGroups.getId(), messageData, userId, GroupOperationType.RENAME);
            });
        }
    }


    private void sendCustomerClearGroupMessage(Integer operatorId, Integer targetId, String operation, Long clearTimestamp) throws Exception,RCloudHttpException {

        CustomerClearGroupMessage customerClearGroupMessage = new CustomerClearGroupMessage();
        customerClearGroupMessage.setOperatorUserId(N3d.encode(operatorId));
        customerClearGroupMessage.setOperation(operation);
        customerClearGroupMessage.setClearTime(String.valueOf(clearTimestamp));
        GroupMessage groupMessage = new GroupMessage();
        groupMessage.setTargetId(new String[]{N3d.encode(targetId)});
        groupMessage.setSenderId(N3d.encode(operatorId));
        groupMessage.setObjectName(customerClearGroupMessage.getType());
        groupMessage.setContent(customerClearGroupMessage);
        groupMessage.setIsIncludeSender(1);
        rongCloudClient.sendGroupMessage(groupMessage);
    }


    /**
     * 获取退群列表
     */
    public List<GroupExitedLists> getExitedList(Integer userId, Integer groupId) throws Exception {
        GroupMembers member = queryGroupMember(groupId,userId);
        if (member == null || !isManagerRole(member.getRole())){
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_MANAGER);
        }
        return groupExitedListsMapper.selectByGroupId(groupId);
    }

    /**
     * 设置群成员信息 设置哪个传哪个，不传为不设置
     */
    public void setMemberInfo(GroupMembers updateGroupMember) throws Exception {
        GroupMembers groupMembers = queryGroupMember(updateGroupMember.getGroupId(), updateGroupMember.getMemberId());
        if (groupMembers == null) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_MANAGER);
        }

        updateGroupMember.setId(groupMembers.getId());
        groupMembersMapper.updateByPrimaryKeySelective(updateGroupMember);
    }


    /**
     * 设置/取消 禁言状态
     */
    public void setMuteAll(Integer userId, Integer groupId, Integer muteStatus, List<Integer> whiteUserIds) throws Exception {


        Groups groupInfo = queryGroupById(groupId);
        if (groupInfo == null) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "group");
        }
        GroupMembers member = queryGroupMember(groupId, userId);
        if (member == null || !isManagerRole(member.getRole())) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_MANAGER);
        }

        Groups update = new Groups();
        update.setId(groupId);
        update.setIsMute(muteStatus);
        groupsMapper.updateByPrimaryKeySelective(update);

        String encodeGroupId = N3d.encode(groupId);
        //取消禁言
        if (Groups.MUTE_STATUS_CLOSE.equals(muteStatus)) {
            ThreadFactoryUtil.ofVirtual(()-> rongCloudClient.removeMuteStatus(List.of(encodeGroupId)));
            return;
        }

        //禁言
        Set<Integer> whites = new HashSet<>(queryManagerIds(groupId));
        if (whiteUserIds != null) {
            whites.addAll(whiteUserIds);
        }
        ThreadFactoryUtil.ofVirtual(() -> rongCloudClient.setMuteStatus(List.of(encodeGroupId)));
        ThreadFactoryUtil.ofVirtual(() -> rongCloudClient.addGroupWhitelist(encodeGroupId, MiscUtils.batchEncodeIds(new ArrayList<>(whites))));
    }

    /**
     * 清空群验证通知消息
     */
    public void clearNotice(Integer currentUserId) {
        groupReceiversMapper.deleteByUserId(currentUserId);
    }

    /**
     * 根据userID获取群验证通知消息
     */
    public List<GroupReceivers> getNoticeInfo(Integer currentUserId) {
        return groupReceiversMapper.selectGroupReceiversWithIncludes(currentUserId);
    }


    private boolean isManagerRole(Integer role) {
        return GroupRole.CREATOR.getRole().equals(role) || GroupRole.MANAGER.getRole().equals(role);
    }

    /**
     * 获取群成员信息
     */
    public List<GroupMembers> getGroupMembers(Integer currentUserId, Integer groupId) throws Exception {

        GroupMembers member = queryGroupMember(groupId,currentUserId);
        if (member == null){
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), "Group is empty.");
        }
        //bugfix 查询所有未被删除的群组成员
        return groupMembersMapper.queryGroupMembersWithUsersByGroupId(groupId);
    }




    /**
     * 获取群公告
     *
     * @param groupId
     * @return
     */
    public GroupBulletins getBulletin(Integer groupId) {
        return groupBulletinsMapper.getLastestGroupBulletin(groupId);
    }

    /**
     * 设置群公告
     *
     */
    public void setBulletin(Integer currentUserId, Integer groupId, String bulletin) {

        GroupBulletins groupBulletins = new GroupBulletins();
        groupBulletins.setGroupId(groupId);
        groupBulletins.setContent(bulletin);
        groupBulletinsMapper.deleteByGroupId(groupId);
        groupBulletinsMapper.insert(groupBulletins);

        String nickname = usersService.getCurrentUserNickNameWithCache(currentUserId);
        if (!StringUtils.isEmpty(nickname)) {
            ThreadFactoryUtil.ofVirtual(() -> {
                TxtMessage txtMessage = new TxtMessage("@所有人 " + bulletin, "");
                MentionedInfo mentionedInfo = new MentionedInfo(1, null, "@所有人 " + bulletin);
                MentionMessageContent mentionMessageContent = new MentionMessageContent(txtMessage, mentionedInfo);
                MentionMessage mentionMessage = new MentionMessage()
                        .setSenderId(N3d.encode(currentUserId))
                        .setTargetId(new String[]{N3d.encode(groupId)})
                        .setObjectName(txtMessage.getType())
                        .setContent(mentionMessageContent)
                        .setIsIncludeSender(1);
                rongCloudClient.sendGroupMentionMessage(mentionMessage);
            });
        }
    }



    /**
     * 批量设置管理员
     */
    public void batchSetManager(Integer currentUserId, Integer groupId, List<Integer> memberIds) throws Exception {
        List<Integer> managerIds = queryManagerIds(groupId);
        if (managerIds.size() + memberIds.size() >= Constants.MAX_GROUP_MANAGER_CNT) {
            throw ServiceException.buildError(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_CREATOR, "group manager");
        }
        List<Integer> actualIds = setGroupMemberRole(currentUserId, groupId, memberIds, GroupRole.MANAGER, GroupOperationType.SET_MANAGER);
        if (actualIds.isEmpty()) {
            return;
        }
        Groups groups = queryGroupById(groupId);
        //开启全员禁言要把管理员加进白名单
        if (Groups.MUTE_STATUS_OPENED.equals(groups.getIsMute())) {
            ThreadFactoryUtil.ofVirtual(() -> rongCloudClient.addGroupWhitelist(N3d.encode(groupId), MiscUtils.batchEncodeIds(actualIds)));
        }
    }



    /**
     * 批量删除群管理员
     */
    public void batchRemoveManager(Integer currentUserId, Integer groupId, List<Integer> memberIds) throws Exception {
        List<Integer> actualIds = setGroupMemberRole(currentUserId, groupId, memberIds, GroupRole.MEMBER, GroupOperationType.REMOVE_MANAGER);
        if (actualIds.isEmpty()) {
            return;
        }
        Groups groups = queryGroupById(groupId);
        groupReceiversMapper.deleteByGroupIdAndUserIds(groupId, actualIds);
        if (Groups.MUTE_STATUS_OPENED.equals(groups.getIsMute())){
            ThreadFactoryUtil.ofVirtual(() -> rongCloudClient.removeGroupWhiteList(N3d.encode(groupId), MiscUtils.batchEncodeIds(actualIds)));
        }
    }


    /**
     * 批量设置群成员角色
     */
    private List<Integer> setGroupMemberRole(Integer userId, Integer groupId, List<Integer> memberIds, GroupRole role, GroupOperationType groupOperationType) throws Exception {

        if (memberIds.contains(userId)) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.MUST_NOT_SELF);
        }
        Groups groupInfo = queryGroupById(groupId);
        if (!userId.equals(groupInfo.getCreatorId())) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_CREATOR);
        }

        List<GroupMembers> members = groupMembersMapper.selectByGroupIdAndMemberIds(groupId, memberIds);
        memberIds = members.stream().map(GroupMembers::getMemberId).toList();
        if (memberIds.isEmpty()) {
            return memberIds;
        }
        groupMembersMapper.updateRoleByGroupIdAndMemberIds(groupId, memberIds, role.getRole());

        List<String> memberIdStrs = MiscUtils.batchEncodeIds(memberIds);
        String groupIdStr = N3d.encode(groupId);

        if (groupOperationType == GroupOperationType.SET_MANAGER) {
            ThreadFactoryUtil.ofVirtual(() -> rongCloudClient.addManager(groupIdStr, memberIdStrs));
        } else if (groupOperationType == GroupOperationType.REMOVE_MANAGER) {
            ThreadFactoryUtil.ofVirtual(() -> rongCloudClient.removeManager(groupIdStr, memberIdStrs));
        }

        //发送群组通知
        String nickname = usersService.getCurrentUserNickNameWithCache(userId);
        List<Users> users = usersService.queryByIds(memberIds);
        List<String> targetUserDisplayNames = new ArrayList<>();
        for (Users u : users) {
            targetUserDisplayNames.add(u.getNickname());
        }

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("operatorId", N3d.encode(userId));
        messageData.put("operatorNickname", nickname);
        messageData.put("targetUserIds", memberIdStrs);
        messageData.put("targetUserDisplayNames", targetUserDisplayNames);
        messageData.put("timestamp", System.currentTimeMillis());
        //发送群组通知
        sendGroupNotificationMessageBySystem(groupId, messageData, userId, groupOperationType);

        return memberIds;
    }



    /**
     * 转让群主
     */
    public void transfer(Integer currentUserId, Integer groupId, Integer targetId) throws Exception {

        if (targetId.equals(currentUserId)) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), "Can not do self");
        }
        Groups groupInfo = queryGroupById(groupId);
        if (!currentUserId.equals(groupInfo.getCreatorId())){
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_CREATOR);
        }
        GroupMembers targetMember = groupMembersMapper.selectByGroupIdAndMemberId(groupId,targetId);
        if (targetMember == null){
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_MEMBER);
        }

        Groups updateGroup = new Groups();
        updateGroup.setId(groupId);
        updateGroup.setCreatorId(targetId);
        groupsMapper.updateByPrimaryKeySelective(updateGroup);
        groupMembersMapper.updateRoleByGroupIdAndMemberIds(groupId,List.of(currentUserId),GroupRole.MEMBER.getRole());
        groupMembersMapper.updateRoleByGroupIdAndMemberIds(groupId,List.of(targetId),GroupRole.CREATOR.getRole());
        groupReceiversMapper.deleteByGroupIdAndUserIds(groupId, List.of(currentUserId));

        if (Groups.MUTE_STATUS_OPENED.equals(groupInfo.getIsMute())) {
            ThreadFactoryUtil.ofVirtual(()-> rongCloudClient.addGroupWhitelist(N3d.encode(groupId), List.of(N3d.encode(targetId))));
        }
        ThreadFactoryUtil.ofVirtual(()->{
            String newOwner = N3d.encode(targetId);
            rongCloudClient.groupTransferOwner(N3d.encode(groupId), newOwner);
            String currentUserNickName = usersService.getCurrentUserNickNameWithCache(currentUserId);
            String userNickName = usersService.getCurrentUserNickNameWithCache(targetId);
            //发送群通知
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("operatorId", N3d.encode(currentUserId));
            messageData.put("operatorNickname", currentUserNickName);
            messageData.put("targetUserIds", List.of(newOwner));
            messageData.put("targetUserDisplayNames", ImmutableList.of(userNickName));
            messageData.put("timestamp", System.currentTimeMillis());
            //发送群组通知消息
            sendGroupNotificationMessageBySystem(groupId, messageData, currentUserId, GroupOperationType.TRANSFER);
        });
    }



    /**
     * 解散群组
     */
    public void dismiss(Integer currentUserId, Integer groupId) throws Exception {

        Groups groupInfo = queryGroupById(groupId);
        if (groupInfo == null || !currentUserId.equals(groupInfo.getCreatorId())) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_CREATOR);
        }
        ThreadFactoryUtil.ofVirtual(()->{
            String nickname = usersService.getCurrentUserNickNameWithCache(currentUserId);
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("operatorNickname", nickname);
            messageData.put("timestamp", System.currentTimeMillis());
            //发送群组通知消息
            sendGroupNotificationMessageBySystem(groupId, messageData, currentUserId, GroupOperationType.DISMISS);
            Thread.sleep(1000);
            rongCloudClient.dismiss(N3d.encode(groupId));
        });
        groupsMapper.deleteByPrimaryKey(groupId);
        groupMembersMapper.deleteByGroupId(groupId);
        groupReceiversMapper.deleteByGroupId(groupId);
        groupBulletinsMapper.deleteByGroupId(groupId);
        groupExitedListsMapper.deleteByGroupId(groupId);
        groupFavsMapper.deleteByGroupId(groupId);
    }



    /**
     * 用户退出群组
     */
    public void quitGroup(Integer currentUserId, Integer groupId) throws Exception {

        Groups groupInfo = queryGroupById(groupId);
        if (groupInfo == null) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "group");
        }

        if (currentUserId.equals(groupInfo.getCreatorId())){
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.MUST_NOT_GROUP_CREATOR);
        }

        GroupMembers groupMember = queryGroupMember(groupId, currentUserId);
        if (groupMember == null){
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_MEMBER);
        }

        removeGroupMembers(currentUserId, groupInfo, List.of(currentUserId), GroupOperationType.QUIT, GroupExitedLists.QUITE_REASON_SELF);
    }


    /**
     * 群主或群管理员将群成员踢出群组
     */
    public void kickMember(Integer currentUserId, Integer groupId, List<Integer> kickMembers) throws Exception {

        if (kickMembers.contains(currentUserId)) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.MUST_NOT_SELF);
        }
        //查询群组信息
        Groups groupInfo = queryGroupById(groupId);
        if (groupInfo == null) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "group");
        }
        // 不能踢自己
        if (kickMembers.contains(groupInfo.getCreatorId())) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.MUST_NOT_GROUP_CREATOR);
        }

        GroupMembers currentMember = groupMembersMapper.selectByGroupIdAndMemberId(groupId,currentUserId);
        if (currentMember == null || !isManagerRole(currentMember.getRole())) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_MANAGER);
        }

        List<GroupMembers> members = groupMembersMapper.selectByGroupIdAndMemberIds(groupId, kickMembers);
        boolean kickManager = members.stream().anyMatch(m -> isManagerRole(m.getRole()));
        boolean isCreator = GroupRole.CREATOR.getRole().equals(currentMember.getRole());
        if (!isCreator && kickManager){
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), "You can not kick group manager.");
        }

        removeGroupMembers(currentUserId, groupInfo, kickMembers, GroupOperationType.KICKED, isCreator ? GroupExitedLists.QUITE_REASON_CREATOR : GroupExitedLists.QUITE_REASON_MANAGER);
    }


    private void removeGroupMembers(Integer operatorId, Groups group, List<Integer> members, GroupOperationType operationType, int removeReason) throws Exception {

        List<String> memberIdStrs = MiscUtils.batchEncodeIds(members);
        String groupIdStr = N3d.encode(group.getId());
        String operatorNickName = usersService.getCurrentUserNickNameWithCache(operatorId);

        List<Users> memberUsers = usersService.queryByIds(members);
        List<String> targetIdStrs = MiscUtils.batchEncodeIds(memberUsers.stream().map(Users::getId).toList());
        List<String> targetNames = memberUsers.stream().map(Users::getNickname).toList();


        groupMembersMapper.deleteByGroupIdAndMemberIds(group.getId(), members);
        //删除群组通许录
        groupFavsMapper.deleteByGroupIdAndUserIds(group.getId(), members);

        ThreadFactoryUtil.ofVirtual(()->{
            rongCloudClient.quitGroup(groupIdStr, memberIdStrs);
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("operatorNickname", operatorNickName);
            messageData.put("targetUserIds", targetIdStrs);
            messageData.put("targetUserDisplayNames", targetNames);
            messageData.put("timestamp", System.currentTimeMillis());
            //发送群组通知消息
            sendGroupNotificationMessageBySystem(group.getId(), messageData, operatorId, operationType);
        });


        ThreadFactoryUtil.ofVirtual(() -> {
            //更新群成员数量
            int memberCnt = groupMembersMapper.groupMemberCnt(group.getId());
            //群里只有一个人,直接解散群组
            if (memberCnt <= 1) {
                groupsMapper.deleteByPrimaryKey(group.getId());
                groupMembersMapper.deleteByGroupId(group.getId());
                groupReceiversMapper.deleteByGroupId(group.getId());
                groupBulletinsMapper.deleteByGroupId(group.getId());
                groupExitedListsMapper.deleteByGroupId(group.getId());
                groupFavsMapper.deleteByGroupId(group.getId());
                rongCloudClient.dismiss(groupIdStr);
                return;
            }

            Groups update = new Groups();
            update.setId(group.getId());
            update.setMemberCount(memberCnt);
            groupsMapper.updateByPrimaryKeySelective(update);
            groupReceiversMapper.deleteByGroupIdAndUserIds(group.getId(), members);

            List<GroupExitedLists> saveList = memberUsers.stream().map(u -> {
                GroupExitedLists groupExitedLists = new GroupExitedLists();
                groupExitedLists.setGroupId(group.getId());
                groupExitedLists.setQuitUserId(u.getId());
                groupExitedLists.setQuitNickname(u.getNickname());
                groupExitedLists.setQuitPortraitUri(u.getPortraitUri());
                groupExitedLists.setQuitReason(removeReason);
                groupExitedLists.setQuitTime(System.currentTimeMillis());
                groupExitedLists.setOperatorId(operatorId);
                groupExitedLists.setOperatorName(operatorNickName);
                return groupExitedLists;
            }).toList();
            groupExitedListsMapper.insertBatch(saveList);
        });
    }



    /**
     * 删除群组通讯录
     */
    public void deletefav(Integer currentUserId, Integer groupId) {
        groupFavsMapper.deleteByGroupIdAndUserIds(groupId, List.of(currentUserId));
    }


    /**
     * 定时清理任务 1小时执行一次，清理群组历史消息
     */
    public void cleanGroupMessage() throws Exception {
    }

    /**
     * 保存群组到通信录
     *
     * @param currentUserId
     * @param groupId
     */
    public void fav(Integer currentUserId, Integer groupId) throws Exception {

        GroupFavs groupFavs = new GroupFavs();
        groupFavs.setUserId(currentUserId);
        groupFavs.setGroupId(groupId);
        groupFavsMapper.insert(groupFavs);
    }


}
