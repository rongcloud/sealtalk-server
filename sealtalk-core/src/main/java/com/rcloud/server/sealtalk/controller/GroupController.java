package com.rcloud.server.sealtalk.controller;

import com.google.common.collect.Maps;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.LogType;
import com.rcloud.server.sealtalk.dto.GroupParam;
import com.rcloud.server.sealtalk.dto.TransferGroupParam;
import com.rcloud.server.sealtalk.entity.GroupBulletins;
import com.rcloud.server.sealtalk.entity.GroupExitedLists;
import com.rcloud.server.sealtalk.entity.GroupMembers;
import com.rcloud.server.sealtalk.entity.GroupReceivers;
import com.rcloud.server.sealtalk.entity.Groups;
import com.rcloud.server.sealtalk.entity.Users;
import com.rcloud.server.sealtalk.model.ServerApiParams;
import com.rcloud.server.sealtalk.model.dto.*;
import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import com.rcloud.server.sealtalk.service.GroupsService;
import com.rcloud.server.sealtalk.util.JacksonUtil;
import com.rcloud.server.sealtalk.util.MiscUtils;
import com.rcloud.server.sealtalk.util.N3d;
import com.rcloud.server.sealtalk.util.ValidateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 * 群组相关接口
 */
@RestController
@RequestMapping("/group")
@Slf4j
public class GroupController extends BaseController {

    @Autowired
    private GroupsService groupsService;


    /**
     * 创建群组
     */
    @PostMapping(value = "/create")
    public APIResult<Object> create(@RequestBody GroupParam groupParam) throws Exception {
        String name = MiscUtils.xss(groupParam.getName(), ValidateUtils.GROUP_NAME_MAX_LENGTH);
        ValidateUtils.notBlank(name,"name");
        ValidateUtils.checkLength(groupParam.getMemberIds(), 2, Constants.GROUP_MAX_MEMBER_CNT, "memberIds");
        String portraitUri = Optional.ofNullable(groupParam.getPortraitUri()).orElse("");
        List<Integer> memberIds = MiscUtils.batchDecodeIds(groupParam.getMemberIds());
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = apiParams.getCurrentUserId();
        GroupAddStatusDTO groupAddStatusDTO = groupsService.createGroup(currentUserId, name, portraitUri, memberIds);
        groupAddStatusDTO.setId(N3d.encode(groupAddStatusDTO.getGroupId()));
        if (groupAddStatusDTO.getUserStatus() != null) {
            for (var u : groupAddStatusDTO.getUserStatus()) {
                if (u.getUserId()!= null){
                    u.setId(N3d.encode(u.getUserId()));
                }
            }
        }
        log.info("{}, {}, {}, {}, {}, {}", LogType.CREATE_GROUP, currentUserId, apiParams.getCurrentUserIdStr(), groupAddStatusDTO.getId(), name, memberIds);
        return APIResultWrap.ok(groupAddStatusDTO);
    }


    /**
     * 添加群成员
     */
    @PostMapping(value = "/add")
    public APIResult<Object> addMember(@RequestBody GroupParam groupParam) throws Exception {
        ValidateUtils.notBlank(groupParam.getGroupId(),"groupId");
        ValidateUtils.checkLength(groupParam.getMemberIds(), 1, Constants.GROUP_MAX_MEMBER_CNT, "memberIds");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = apiParams.getCurrentUserId();
        List<UserStatusDTO> userStatusDTOList = groupsService.addMember(currentUserId, N3d.decode(groupParam.getGroupId()), MiscUtils.batchDecodeIds(groupParam.getMemberIds()));
        if (userStatusDTOList != null) {
            for (var u : userStatusDTOList) {
                if (u.getUserId() != null) {
                    u.setId(N3d.encode(u.getUserId()));
                }
            }
        }
        log.info("{}, {}, {}, {}, {}", LogType.ADD_GROUP_MEMBER, currentUserId, apiParams.getCurrentUserIdStr(), groupParam.getGroupId(), Arrays.toString(groupParam.getMemberIds()));
        return APIResultWrap.ok(userStatusDTOList);
    }

    /**
     * 用户加入群组
     */
    @PostMapping(value = "/join")
    public APIResult<Object> joinGroup(@RequestBody GroupParam groupParam) throws Exception {

        ValidateUtils.notBlank(groupParam.getGroupId(), "groupId");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        groupsService.joinGroup(currentUserId, N3d.decode(groupParam.getGroupId()));
        log.info("{}, {}, {}, {}", LogType.JOIN_GROUP, currentUserId, apiParams.getCurrentUserIdStr(), groupParam.getGroupId());
        return APIResultWrap.ok();
    }

    /**
     * 群主或群管理将群成员移出群组
     */
    @PostMapping(value = "/kick")
    public APIResult<Object> kickMember(@RequestBody GroupParam groupParam) throws Exception {
        ValidateUtils.notBlank(groupParam.getGroupId(),"groupId");
        ValidateUtils.checkLength(groupParam.getMemberIds(), 1, 100, "memberIds");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        groupsService.kickMember(currentUserId, N3d.decode(groupParam.getGroupId()), MiscUtils.batchDecodeIds(groupParam.getMemberIds()));
        log.info("{}, {}, {}, {}", LogType.REMOVE_GROUP_MEMBER, apiParams.getCurrentUserIdStr(), groupParam.getGroupId(), Arrays.toString(groupParam.getMemberIds()));
        return APIResultWrap.ok();
    }

    /**
     * 退出群组
     */
    @PostMapping(value = "/quit")
    public APIResult<?> quitGroup(@RequestBody GroupParam groupParam) throws Exception {
        String groupId = groupParam.getGroupId();
        ValidateUtils.notBlank(groupId,"groupId");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        groupsService.quitGroup(currentUserId, N3d.decode(groupId));
        log.info("{}, {}, {}, {}", LogType.QUIT_GROUP, currentUserId, apiParams.getCurrentUserIdStr(), groupId);
        return APIResultWrap.ok();
    }


    /**
     * 解散群组
     */
    @PostMapping(value = "/dismiss")
    public APIResult<Object> dismiss(@RequestBody GroupParam groupParam) throws Exception {

        String groupId = groupParam.getGroupId();
        ValidateUtils.notBlank(groupId,"groupId");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        groupsService.dismiss(currentUserId, N3d.decode(groupId));
        log.info("{}, {}, {}, {}", LogType.DELETE_GROUP, currentUserId, apiParams.getCurrentUserIdStr(), groupId);
        return APIResultWrap.ok();
    }

    /**
     * 转让群主
     */
    @PostMapping(value = "/transfer")
    public APIResult<Object> transfer(@RequestBody TransferGroupParam transferGroupParam) throws Exception {

        String groupId = transferGroupParam.getGroupId();
        String userId = transferGroupParam.getUserId();
        ValidateUtils.notBlank(groupId,"groupId");
        ValidateUtils.notBlank(userId,"userId");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        groupsService.transfer(currentUserId, N3d.decode(groupId), N3d.decode(userId));
        log.info("{}, {}, {}, {},{}", LogType.CHANGE_GROUP_OWNER, currentUserId, apiParams.getCurrentUserIdStr(), groupId, userId);
        return APIResultWrap.ok();
    }

    /**
     * 批量增加管理员
     */
    @PostMapping(value = "/set_manager")
    public APIResult<Object> batchSetManager(@RequestBody GroupParam groupParam) throws Exception {

        String groupId = groupParam.getGroupId();
        String[] memberIds = groupParam.getMemberIds();
        ValidateUtils.notBlank(groupId,"groupId");
        ValidateUtils.notNull(memberIds,"memberIds");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        groupsService.batchSetManager(currentUserId, N3d.decode(groupId), MiscUtils.batchDecodeIds(memberIds));
        log.info("{}, {}, {}, {},{}", LogType.ADD_GROUP_MANAGER, currentUserId, apiParams.getCurrentUserIdStr(), groupId, Arrays.toString(memberIds));
        return APIResultWrap.ok();
    }

    /**
     * 批量删除管理员
     */
    @PostMapping(value = "/remove_manager")
    public APIResult<Object> batchRemoveManager(@RequestBody GroupParam groupParam) throws Exception {

        String groupId = groupParam.getGroupId();
        String[] memberIds = groupParam.getMemberIds();
        ValidateUtils.notBlank(groupId,"groupId");
        ValidateUtils.notNull(memberIds,"memberIds");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        groupsService.batchRemoveManager(currentUserId, N3d.decode(groupId), MiscUtils.batchDecodeIds(Arrays.asList(memberIds)));
        log.info("{}, {}, {}, {},{}", LogType.REMOVE_GROUP_MANAGER, currentUserId, apiParams.getCurrentUserIdStr(), groupId, Arrays.toString(memberIds));
        return APIResultWrap.ok();
    }

    /**
     * 群组重命名、设置头像、设置认证、设置群定时清理状态、设置群成员保护模式
     *
     */
    @PostMapping(value = {"/updateGroupInfo", "/rename", "/set_portrait_uri", "/set_certification", "/set_regular_clear", "/set_member_protection"})
    public APIResult<Object> rename(@RequestBody GroupParam groupParam) throws Exception {
        String groupId = groupParam.getGroupId();
        String name = MiscUtils.xss_null(groupParam.getName(), ValidateUtils.GROUP_NAME_MAX_LENGTH);
        String portraitUri = MiscUtils.xss_null(groupParam.getPortraitUri(), ValidateUtils.PORTRAIT_URI_MAX_LENGTH);
        if (portraitUri != null) {
            ValidateUtils.checkURLFormat(portraitUri, "portraitUri");
        }
        if (groupParam.getCertiStatus() != null){
            ValidateUtils.check_boolean_num(groupParam.getCertiStatus(), "certiStatus");
        }
        if (groupParam.getClearStatus() != null){
            ValidateUtils.valueOf(groupParam.getClearStatus(), List.of(0, 3, 7, 36),"clearStatus");
        }
        if (groupParam.getMemberProtection()!= null){
            ValidateUtils.check_boolean_num(groupParam.getCertiStatus(), "memberProtection");
        }
        Groups updateGroup = new Groups();
        updateGroup.setId(N3d.decode(groupId));
        updateGroup.setName(name);
        updateGroup.setPortraitUri(portraitUri);
        updateGroup.setCertiStatus(groupParam.getCertiStatus());
        updateGroup.setClearStatus(groupParam.getClearStatus());
        updateGroup.setMemberProtection(groupParam.getMemberProtection());
        Integer currentUserId = getServerApiParams().getCurrentUserId();
        groupsService.updateGroupInfo(currentUserId, updateGroup);
        return APIResultWrap.ok();
    }




    /**
     * 设置/取消 全员禁言
     */
    @PostMapping(value = "/mute_all")
    public APIResult<Object> setMuteAll(@RequestBody GroupParam groupParam) throws Exception {

        String groupId = groupParam.getGroupId();
        Integer muteStatus = groupParam.getMuteStatus();
        String[] userId = groupParam.getUserId();

        ValidateUtils.notBlank(groupId,"groupId");
        ValidateUtils.check_boolean_num(muteStatus,"muteStatus");

        Integer currentUserId = getCurrentUserId();

        groupsService.setMuteAll(currentUserId, N3d.decode(groupId), muteStatus, userId == null || userId.length == 0 ? Collections.emptyList() : MiscUtils.batchDecodeIds(userId));

        return APIResultWrap.ok();
    }


    /**
     * 保存群组至通讯录
     */
    @PostMapping(value = "/fav")
    public APIResult<Object> fav(@RequestBody GroupParam groupParam) throws Exception {

        ValidateUtils.notBlank(groupParam.getGroupId(),"groupId");
        Integer currentUserId = getCurrentUserId();
        groupsService.fav(currentUserId, N3d.decode(groupParam.getGroupId()));
        return APIResultWrap.ok();
    }

    /**
     * 删除群组通讯录
     */
    @DeleteMapping(value = "/fav")
    public APIResult<Object> deleteGroupFav(@RequestBody GroupParam groupParam) throws Exception {

        String groupId = groupParam.getGroupId();
        ValidateUtils.notBlank(groupId,"groupId");

        Integer currentUserId = getCurrentUserId();
        groupsService.deletefav(currentUserId, N3d.decode(groupId));
        return APIResultWrap.ok();
    }

    /**
     * 发布群公告
     */
    @PostMapping(value = "/set_bulletin")
    public APIResult<Object> setBulletin(@RequestBody GroupParam groupParam) throws Exception {

        ValidateUtils.notBlank(groupParam.getGroupId(),"groupId");
        String bulletin = groupParam.getBulletin() == null ? groupParam.getContent() : groupParam.getBulletin();
        ValidateUtils.notBlank(bulletin,"bulletin");
        bulletin = MiscUtils.xss(bulletin, ValidateUtils.GROUP_BULLETIN_MAX_LENGTH);

        Integer currentUserId = getCurrentUserId();
        groupsService.setBulletin(currentUserId, N3d.decode(groupParam.getGroupId()), bulletin);
        return APIResultWrap.ok();
    }

    /**
     * 获取群公告
     */
    @GetMapping(value = "/get_bulletin")
    public APIResult<?> getBulletin(@RequestParam("groupId") String groupId) throws Exception {

        ValidateUtils.notBlank(groupId, "groupId");

        GroupBulletins groupBulletins = groupsService.getBulletin(N3d.decode(groupId));

        GroupBulletinsDTO groupBulletinsDTO = new GroupBulletinsDTO();
        if (groupBulletins != null) {
            // 返回给前端的结构id属性需要N3d编码
            groupBulletinsDTO.setId(N3d.encode(groupBulletins.getId()));
            groupBulletinsDTO.setGroupId(N3d.encode(groupBulletins.getGroupId()));
            groupBulletinsDTO.setContent(groupBulletins.getContent());
        }

        return APIResultWrap.ok(groupBulletinsDTO);
    }




    /**
     * 获取群信息
     */
    @GetMapping(value = "/{id}")
    public APIResult<?> getGroupInfo(@PathVariable("id") String groupId) throws Exception {
        ValidateUtils.notBlank(groupId, "id");

        Groups group = groupsService.queryGroupById(N3d.decode(groupId));

        GroupDTO groupDTO = new GroupDTO();
        if (group != null) {
            groupDTO.setId(N3d.encode(group.getId()));
            groupDTO.setName(group.getName());
            groupDTO.setPortraitUri(group.getPortraitUri());
            groupDTO.setCreatorId(N3d.encode(group.getCreatorId()));
            groupDTO.setMemberCount(group.getMemberCount());
            groupDTO.setMaxMemberCount(group.getMaxMemberCount());
            groupDTO.setCertiStatus(group.getCertiStatus());
            groupDTO.setBulletin(group.getBulletin());
            groupDTO.setIsMute(group.getIsMute());
            groupDTO.setMemberProtection(group.getMemberProtection());
        }
        return APIResultWrap.ok(groupDTO);
    }

    /**
     * 获取群成员列表
     */
    @GetMapping(value = "/{id}/members")
    public APIResult<?> getGroupMembers(@PathVariable("id") String groupId) throws Exception {

        ValidateUtils.notBlank(groupId, "id");

        Integer currentUserId = getCurrentUserId();

        List<GroupMembers> groupMembersList = groupsService.getGroupMembers(currentUserId, N3d.decode(groupId));

        List<MemberDTO> memberDTOList = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMATR_PATTERN);

        if (!CollectionUtils.isEmpty(groupMembersList)) {
            for (GroupMembers groupMembers : groupMembersList) {
                MemberDTO memberDTO = new MemberDTO();
                memberDTO.setGroupNickname(groupMembers.getGroupNickname());
                memberDTO.setRole(groupMembers.getRole());
                memberDTO.setDisplayName(groupMembers.getDisplayName()==null?"":groupMembers.getDisplayName());

                memberDTO.setCreatedAt(sdf.format(groupMembers.getCreatedAt()));
                memberDTO.setCreatedTime(groupMembers.getCreatedAt().getTime());
                memberDTO.setUpdatedAt(sdf.format(groupMembers.getUpdatedAt()));
                memberDTO.setUpdatedTime(groupMembers.getUpdatedAt().getTime());

                UserDTO userDTO = new UserDTO();
                memberDTO.setUser(userDTO);

                Users u = groupMembers.getUsers();
                if (u != null) {
                    userDTO.setId(N3d.encode(u.getId()));
                    userDTO.setNickname(u.getNickname());
                    userDTO.setGender(u.getGender());
                    userDTO.setPortraitUri(u.getPortraitUri());
                    userDTO.setStAccount(u.getStAccount());

                }
                memberDTOList.add(memberDTO);
            }
        }

        return APIResultWrap.ok(memberDTOList);
    }



    /**
     * 获取群验证通知消息
     */
    @GetMapping(value = "/notice_info")
    public APIResult<?> getNoticeInfo() throws Exception {

        Integer currentUserId = getCurrentUserId();

        List<GroupReceivers> groupReceiversList = groupsService.getNoticeInfo(currentUserId);
        List<GroupReceiverDTO> groupReceiverDTOList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMATR_PATTERN);

        if (!CollectionUtils.isEmpty(groupReceiversList)) {
            for (GroupReceivers groupReceivers : groupReceiversList) {
                GroupReceiverDTO dto = new GroupReceiverDTO();
                dto.setId(N3d.encode(groupReceivers.getId()));
                dto.setCreatedTime(groupReceivers.getCreatedAt());
                dto.setCreatedAt(sdf.format(groupReceivers.getCreatedAt()));
                dto.setTimestamp(groupReceivers.getCreatedAt().getTime());
                dto.setStatus(groupReceivers.getStatus());
                dto.setType(groupReceivers.getType());
                Map<String, Object> group = Maps.newHashMap();
                Map<String, Object> receiver = Maps.newHashMap();
                Map<String, Object> requester = Maps.newHashMap();
                if (groupReceivers.getGroup() != null) {
                    group.put("id", N3d.encode(groupReceivers.getGroup().getId()));
                    group.put("name", groupReceivers.getGroup().getName());

                }
                dto.setGroup(group);

                if (groupReceivers.getReceiver() != null) {
                    receiver.put("id", N3d.encode(groupReceivers.getReceiver().getId()));
                    receiver.put("nickname", groupReceivers.getReceiver().getNickname());
                }
                dto.setReceiver(receiver);
                if (groupReceivers.getRequester() != null) {
                    requester.put("id", N3d.encode(groupReceivers.getRequester().getId()));
                    requester.put("nickname", groupReceivers.getRequester().getNickname());
                }
                dto.setRequester(requester);
                groupReceiverDTOList.add(dto);
            }
        }

        return APIResultWrap.ok(groupReceiverDTOList);
    }


    /**
     * 清空群验证通知消息
     */
    @PostMapping(value = "/clear_notice")
    public APIResult<Object> clearNotice() throws Exception {

        Integer currentUserId = getCurrentUserId();

        groupsService.clearNotice(currentUserId);

        return APIResultWrap.ok();
    }




    /**
     * 获取群定时清理状态
     */
    @PostMapping(value = "/get_regular_clear")
    public APIResult<?> getRegularClear(@RequestBody GroupParam groupParam) throws Exception {

        String groupId = groupParam.getGroupId();
        ValidateUtils.notBlank(groupId, "groupId");

        Groups groups = groupsService.queryGroupById(N3d.decode(groupId));

        if (groups != null) {
            Integer clearStatus = groups.getClearStatus();
            Map<String, Integer> result = new HashMap<>();
            result.put("clearStatus", clearStatus);
            return APIResultWrap.ok(clearStatus);
        } else {
            return APIResultWrap.ok();
        }
    }

    /**
     * 设置群成员信息
     */
    @PostMapping(value = "/set_member_info")
    public APIResult<Object> setMemberInfo(@RequestBody GroupParam groupParam) throws Exception {

        ValidateUtils.notBlank(groupParam.getGroupId(), "groupId");
        ValidateUtils.notBlank(groupParam.getMemberId(), "memberId");
        //更新GroupMember 信息
        GroupMembers newGroupMembers = new GroupMembers();
        newGroupMembers.setGroupId(N3d.decode(groupParam.getGroupId()));
        newGroupMembers.setMemberId(N3d.decode(groupParam.getMemberId()));
        newGroupMembers.setGroupNickname(groupParam.getGroupNickname());
        newGroupMembers.setDisplayName(groupParam.getGroupNickname());
        newGroupMembers.setRegion(groupParam.getRegion());
        newGroupMembers.setPhone(groupParam.getPhone());
        newGroupMembers.setWeChat(groupParam.getWeChat());
        newGroupMembers.setAlipay(groupParam.getAlipay());
        if (groupParam.getMemberDesc() != null) {
            newGroupMembers.setMemberDesc(JacksonUtil.toJson(groupParam.getMemberDesc()));
        }
        groupsService.setMemberInfo(newGroupMembers);
        return APIResultWrap.ok();
    }



    /**
     * 设置自己的群名片
     */
    @PostMapping(value = "/set_display_name")
    public APIResult<Object> setDisplayName(@RequestBody GroupParam groupParam) throws Exception {

        ValidateUtils.notBlank(groupParam.getGroupId(), "groupId");
        var displayName = MiscUtils.xss(groupParam.getDisplayName(), ValidateUtils.GROUP_MEMBER_DISPLAY_NAME_MAX_LENGTH);
        ValidateUtils.checkLength(displayName, 1,32,"displayName");
        Integer currentUserId = getCurrentUserId();

        GroupMembers updateMember = new GroupMembers();
        updateMember.setGroupId(N3d.decode(groupParam.getGroupId()));
        updateMember.setMemberId(currentUserId);
        updateMember.setDisplayName(displayName);
        groupsService.setMemberInfo(updateMember);
        return APIResultWrap.ok();
    }



    /**
     * 获取群成员信息
     */
    @PostMapping(value = "/get_member_info")
    public APIResult<?> getMemberInfo(@RequestBody GroupParam groupParam) throws Exception {

        String groupId = groupParam.getGroupId();
        String memberId = groupParam.getMemberId();

        ValidateUtils.notBlank(groupId, "groupId");
        ValidateUtils.notBlank(memberId, "memberId");

        GroupMembers groupMembers = groupsService.queryGroupMember(N3d.decode(groupId), N3d.decode(memberId));

        Map<String, Object> resultMap = new HashMap<>();

        if (groupMembers != null) {
            resultMap.put("isDeleted", 0);
            resultMap.put("groupNickname", groupMembers.getGroupNickname());
            resultMap.put("WeChat", groupMembers.getWeChat());
            resultMap.put("Alipay", groupMembers.getAlipay());
            if (groupMembers.getMemberDesc() != null) {
                //memberDesc 特殊处理
                resultMap.put("memberDesc", JacksonUtil.getJsonNode(groupMembers.getMemberDesc()));
            } else {
                resultMap.put("memberDesc", null);
            }
        } else {
            resultMap.put("isDeleted", null);
            resultMap.put("groupNickname", null);
            resultMap.put("region", null);
            resultMap.put("phone", null);
            resultMap.put("WeChat", null);
            resultMap.put("Alipay", null);
            resultMap.put("memberDesc", null);
        }

        return APIResultWrap.ok(resultMap);
    }


    /**
     * 获取退群列表
     */
    @PostMapping(value = "/exited_list")
    public APIResult<?> getExitedList(@RequestBody GroupParam groupParam) throws Exception {

        String groupId = groupParam.getGroupId();
        ValidateUtils.notBlank(groupId, "groupId");

        Integer currentUserId = getCurrentUserId();
        List<GroupExitedListDTO> groupExitedListDTOList = new ArrayList<>();
        List<GroupExitedLists> groupExitedListsList = groupsService.getExitedList(currentUserId, N3d.decode(groupId));
        if (!CollectionUtils.isEmpty(groupExitedListsList)) {
            for (GroupExitedLists groupExitedLists : groupExitedListsList) {
                GroupExitedListDTO dto = new GroupExitedListDTO();
                dto.setQuitUserId(N3d.encode(groupExitedLists.getQuitUserId()));
                dto.setQuitNickname(groupExitedLists.getQuitNickname());
                dto.setQuitPortraitUri(groupExitedLists.getQuitPortraitUri());
                dto.setQuitReason(groupExitedLists.getQuitReason());
                dto.setQuitTime(groupExitedLists.getQuitTime());
                dto.setOperatorId(N3d.encode(groupExitedLists.getOperatorId()));
                dto.setOperatorName(groupExitedLists.getOperatorName());
                groupExitedListDTOList.add(dto);
            }
        }
        return APIResultWrap.ok(groupExitedListDTOList);
    }


    /**
     * 复制群组
     */
    @PostMapping(value = "/copy_group")
    public APIResult<GroupAddStatusDTO> copyGroup(@RequestBody GroupParam groupParam) throws Exception {
        String groupId = groupParam.getGroupId();
        String name = groupParam.getName();
        String portraitUri = groupParam.getPortraitUri();

        name = MiscUtils.xss(name, ValidateUtils.GROUP_NAME_MAX_LENGTH);
        ValidateUtils.notBlank(groupId, "groupId");
        ValidateUtils.notBlank(name, "name");
        portraitUri = MiscUtils.xss(portraitUri, ValidateUtils.PORTRAIT_URI_MAX_LENGTH);

        Integer currentUserId = getCurrentUserId();
        GroupAddStatusDTO groupAddStatusDTO = groupsService.copyGroup(currentUserId, N3d.decode(groupId), name, portraitUri);
        return APIResultWrap.ok(groupAddStatusDTO);

    }


    /**
     * 同意群邀请
     */
    @PostMapping(value = "/agree")
    public APIResult<GroupAddStatusDTO> agree(@RequestBody GroupParam groupParam) throws Exception {
        String groupId = groupParam.getGroupId();
        String receiverId = groupParam.getReceiverId();
        String status = groupParam.getStatus();

        ValidateUtils.notBlank(groupId, "groupId");
        ValidateUtils.notBlank(receiverId, "receiverId");
        ValidateUtils.valueOf(status, List.of("0", "1"), "status");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        groupsService.agree(currentUserId, N3d.decode(groupId), N3d.decode(receiverId), status);
        log.info("{}, {}, {}, groupId:{}, {}, status:{}", LogType.AGREE_JOIN_GROUP, currentUserId, apiParams.getCurrentUserIdStr(), groupId, receiverId, status);
        return APIResultWrap.ok();

    }


}
