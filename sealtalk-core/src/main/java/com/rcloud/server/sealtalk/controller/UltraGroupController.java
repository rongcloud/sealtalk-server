package com.rcloud.server.sealtalk.controller;

import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.LogType;
import com.rcloud.server.sealtalk.constant.UltraGroupChannelTypeEnum;
import com.rcloud.server.sealtalk.dto.UltraGroupParam;
import com.rcloud.server.sealtalk.entity.UltraGroupMembers;
import com.rcloud.server.sealtalk.entity.Users;
import com.rcloud.server.sealtalk.model.ServerApiParams;
import com.rcloud.server.sealtalk.model.dto.UgUserGroupDTO;
import com.rcloud.server.sealtalk.model.dto.UltraGroupChannelDTO;
import com.rcloud.server.sealtalk.model.dto.UltraGroupCreateDTO;
import com.rcloud.server.sealtalk.model.dto.UltraGroupMemberDTO;
import com.rcloud.server.sealtalk.model.dto.UserDTO;
import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import com.rcloud.server.sealtalk.service.UltraGroupService;
import com.rcloud.server.sealtalk.util.MiscUtils;
import com.rcloud.server.sealtalk.util.N3d;
import com.rcloud.server.sealtalk.util.ValidateUtils;
import io.rong.models.Result;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 超级群相关接口
 */
@RestController
@RequestMapping("/ultragroup")
@Slf4j
public class UltraGroupController extends BaseController {

    @Autowired
    private UltraGroupService ultraGroupService;


    /**
     * 创建超级群
     */
    @PostMapping(value = "/create")
    public APIResult<Object> create(@RequestBody UltraGroupParam param) throws Exception {

        String name = param.getGroupName();
        String portraitUri = Optional.ofNullable(param.getPortraitUri()).orElse("");
        String summary = param.getSummary();

        ValidateUtils.checkLength(name, ValidateUtils.GROUP_NAME_MIN_LENGTH, ValidateUtils.GROUP_NAME_MAX_LENGTH, "groupName");

        name = MiscUtils.xss(name, ValidateUtils.GROUP_NAME_MAX_LENGTH);
        summary = MiscUtils.xss(summary,ValidateUtils.ULTRA_GROUP_SUMMARY_MAX_LENGTH);

        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        UltraGroupCreateDTO ultraGroupCreateDTO = ultraGroupService
            .createUltraGroup(currentUserId, name, portraitUri, summary);
        log.info("{}, {}, {}, {}, {}, {}, {}", LogType.CREATE_UG, currentUserId, apiParams.getCurrentUserIdStr(), name,ultraGroupCreateDTO.getGroupId(),ultraGroupCreateDTO.getDefaultChannelId(),ultraGroupCreateDTO.getDefaultChannelName());
        return APIResultWrap.ok(ultraGroupCreateDTO);
    }

    /**
     * 加入超级群
     */
    @PostMapping(value = "/add")
    public APIResult<Object> add(@RequestBody UltraGroupParam param) throws Exception {

        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ValidateUtils.checkLength(param.getMemberIds(), 1, ValidateUtils.DEFAULT_MAX_ULTRA_GROUP_MEMBER_COUNT, "memberIds");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        Integer groupId = N3d.decode(param.getGroupId());
        List<Integer> members = MiscUtils.batchDecodeIds(param.getMemberIds());

        ultraGroupService.joinUltraGroup(currentUserId, groupId, members);
        log.info("{}, {}, {}, {}, {}", LogType.JOIN_UG, currentUserId, apiParams.getCurrentUserIdStr(), groupId,Arrays.toString(param.getMemberIds()));
        return APIResultWrap.ok(param.getMemberIds());
    }

    /**
     * 退出超级群
     */
    @PostMapping(value = "/quit")
    public APIResult<Object> quit(@RequestBody UltraGroupParam param) throws Exception {

        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        Integer groupId = N3d.decode(param.getGroupId());

        ultraGroupService.quitUltraGroup(currentUserId, groupId);
        log.info("{}, {}, {}, {}", LogType.QUIT_UG, currentUserId, apiParams.getCurrentUserIdStr(), groupId);
        return APIResultWrap.ok();
    }

    /**
     * 解散超级群
     */
    @PostMapping(value = "/dismiss")
    public APIResult<Object> dismiss(@RequestBody UltraGroupParam param) throws Exception {

        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        Integer groupId = N3d.decode(param.getGroupId());
        ultraGroupService.dismissUltraGroup(currentUserId, groupId);
        log.info("{}, {}, {}, {}", LogType.DELETE_UG, currentUserId, apiParams.getCurrentUserIdStr(), groupId);
        return APIResultWrap.ok();
    }


    /**
     * 分页查询超级群成员
     */
    @PostMapping(value = "/members")
    public APIResult<Object> members(@RequestBody UltraGroupParam param) throws Exception {

        Integer currentUserId = getCurrentUserId();
        Integer pageNum = param.getPageNum();
        if (pageNum == null || pageNum<1) {
            pageNum = 1;
        }
        Integer limit = param.getLimit();
        if (limit == null) {
            limit = 20;
        }

        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        Integer groupId = N3d.decode(param.getGroupId());

        List<UltraGroupMembers> ultraGroupMembersList = ultraGroupService
            .getMembers(currentUserId, groupId, pageNum, limit);

        List<UltraGroupMemberDTO> memberDTOList = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMATR_PATTERN);

        if (!CollectionUtils.isEmpty(ultraGroupMembersList)) {
            for (UltraGroupMembers groupMembers : ultraGroupMembersList) {
                UltraGroupMemberDTO memberDTO = new UltraGroupMemberDTO();
                memberDTO.setRole(groupMembers.getRole());
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
                    memberDTO.setGroupNickname(u.getNickname());
                    memberDTO.setMemberName(u.getNickname());
                }
                memberDTOList.add(memberDTO);
            }
        }

        return APIResultWrap.ok(memberDTOList);
    }

    /**
     * 创建频道
     */
    @PostMapping(value = "/channel/create")
    public APIResult<Object> createChannel(@RequestBody UltraGroupParam param) throws Exception {

        ServerApiParams apiParams = getServerApiParams();
        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        String channelName = param.getChannelName();
        ValidateUtils.checkLength(channelName, ValidateUtils.GROUP_NAME_MIN_LENGTH, ValidateUtils.GROUP_NAME_MAX_LENGTH, "channelName");

        UltraGroupChannelTypeEnum channelType = param.getType() == null ? UltraGroupChannelTypeEnum.PUBLIC : UltraGroupChannelTypeEnum.channelType(param.getType());
        ValidateUtils.notNull(channelType, "channelType");
        channelName = MiscUtils.xss(channelName, ValidateUtils.ULTRA_GROUP_CHANNEL_NAME_MAX_LENGTH);

        Map<String, String> result = new HashMap<>();

        Integer currentUserId = getCurrentUserId();
        Integer groupId = N3d.decode(param.getGroupId());

        String channelId = ultraGroupService.createUltraGroupChannel(currentUserId, groupId, channelName, channelType);
        result.put("channelId", channelId);
        log.info("{}, {}, {}, ugId:{}, channel:{}", LogType.CREATE_CHANNEL, currentUserId, apiParams.getCurrentUserIdStr(), groupId, channelId);
        return APIResultWrap.ok(result);
    }

    /**
     * 获取超级群频道列表
     */
    @PostMapping(value = "/channel/list")
    public APIResult<Object> channelList(@RequestBody UltraGroupParam param) throws Exception {
        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        Integer pageNum = param.getPageNum() == null || param.getPageNum() < 1 ? 1 : param.getPageNum();
        Integer limit = param.getLimit() == null ? 20 : (param.getLimit() > 100 ? 100 : param.getLimit());
        Integer currentUserId = getCurrentUserId();
        Integer groupId = N3d.decode(param.getGroupId());
        List<UltraGroupChannelDTO> ultraGroupChannelDTOList = ultraGroupService.getUltraGroupChannels(currentUserId, groupId, pageNum, limit);
        return APIResultWrap.ok(ultraGroupChannelDTOList);
    }

    /**
     * 设置频道类型
     */
    @PostMapping(value = "/channel/type/change")
    public APIResult<Void> typeChange(@RequestBody UltraGroupParam param) throws Exception {

        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ValidateUtils.notBlank(param.getChannelId(),"channelId");
        UltraGroupChannelTypeEnum channelType = UltraGroupChannelTypeEnum.channelType(param.getType());
        ValidateUtils.notNull(channelType, "channelType");
        ServerApiParams apiParams = getServerApiParams();
        Integer groupId = N3d.decode(param.getGroupId());
        ultraGroupService.channelTypeChange(getCurrentUserId(),groupId,param.getChannelId(),channelType);
        log.info("{}, {}, {}, ugId:{}, channel:{}, type:{}", LogType.CHANGE_CHANNEL_TYPE, apiParams.getCurrentUserId(), apiParams.getCurrentUserIdStr(), param.getGroupId(), param.getChannelId(), param.getType());
        return APIResultWrap.ok();
    }


    /**
     * 私有频道白名单用户添加
     */
    @PostMapping(value = "/channel/private/users/add")
    public APIResult<Void> privateChannelUserAdd(@RequestBody UltraGroupParam param) throws Exception {

        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ValidateUtils.notBlank(param.getChannelId(),"channelId");
        ValidateUtils.checkLength(param.getMemberIds(), 1, 20, "memberIds");
        ServerApiParams apiParams = getServerApiParams();
        Result result = ultraGroupService.privateChannelMemberAdd(N3d.decode(param.getGroupId()), param.getChannelId(), MiscUtils.batchDecodeIds(param.getMemberIds()));
        log.info("{}, {}, {}, ugId:{}, channel:{}, members:{}", LogType.ADD_CHANNEL_MEMBER,
            apiParams.getCurrentUserId(), apiParams.getCurrentUserIdStr(), param.getGroupId(),
            param.getChannelId(), Arrays.toString(param.getMemberIds()));
        return new APIResult<>(String.valueOf(result.getCode()), result.getErrorMessage());
    }

    /**
     * 私有频道白名单用户删除
     */
    @PostMapping(value = "/channel/private/users/del")
    public APIResult<Void> privateChannelUserDel(@RequestBody UltraGroupParam param) throws Exception {

        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ValidateUtils.notBlank(param.getChannelId(),"channelId");
        ValidateUtils.checkLength(param.getMemberIds(), 1, 20, "memberIds");
        ServerApiParams apiParams = getServerApiParams();
        Result result = ultraGroupService
            .privateChannelMemberDel(N3d.decode(param.getGroupId()), param.getChannelId(), MiscUtils.batchDecodeIds(param.getMemberIds()));
        log.info("{}, {}, {}, ugId:{}, channel:{}, members:{}", LogType.REMOVE_CHANNEL_MEMBER,
            apiParams.getCurrentUserId(), apiParams.getCurrentUserIdStr(), param.getGroupId(),
            param.getChannelId(), Arrays.toString(param.getMemberIds()));
        return new APIResult<>(String.valueOf(result.getCode()), result.getErrorMessage());
    }


    /**
     * 私有频道白名单用户查询
     */
    @PostMapping(value = "/channel/private/users/get")
    public APIResult<Object> privateChannelUserGet(@RequestBody UltraGroupParam param) throws Exception {

        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ValidateUtils.notBlank(param.getChannelId(),"channelId");
        Integer pageNum = param.getPageNum() == null || param.getPageNum() < 1 ? 1 : param.getPageNum();
        Integer limit = param.getLimit() == null ? 20 : (param.getLimit() > 100 ? 100 : param.getLimit());
        List<String> users = ultraGroupService.privateChannelMembersQuery(param.getGroupId(), param.getChannelId(), pageNum, limit);
        Map<String, Object> result = new HashMap<>();
        result.put("users", users);
        return APIResultWrap.ok(result);
    }


    /**
     * 删除频道
     */
    @PostMapping(value = "/channel/del")
    public APIResult<Object> channelDel(@RequestBody UltraGroupParam param) throws Exception {
        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ValidateUtils.notBlank(param.getChannelId(),"channelId");
        ServerApiParams apiParams = getServerApiParams();
        Integer groupId = N3d.decode(param.getGroupId());
        ultraGroupService.channelDel(groupId, param.getChannelId());
        log.info("{}, {}, {}, ugId:{}, channel:{}", LogType.DELETE_CHANNEL, apiParams.getCurrentUserId(), apiParams.getCurrentUserIdStr(), param.getGroupId(), param.getChannelId());
        return APIResultWrap.ok();
    }

    /**
     * 新增用户组
     */
    @PostMapping(value = "/usergroup/add")
    public APIResult<String> addUserGroup(@RequestBody UltraGroupParam param)
            throws Exception {
        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        Integer currentUserId = getCurrentUserId();
        ServerApiParams apiParams = getServerApiParams();
        Integer groupId = N3d.decode(param.getGroupId());
        long userGroupId = ultraGroupService.addUserGroup(currentUserId, groupId, param.getUserGroupName());
        log.info("{}, {}, {}, ugId:{}, userGroupId:{}", LogType.CREATE_USERGROUP, apiParams.getCurrentUserId(), apiParams.getCurrentUserIdStr(), param.getGroupId(), userGroupId);
        return APIResultWrap.ok(N3d.encode(userGroupId));
    }

    /**
     * 删除用户组
     */
    @PostMapping(value = "/usergroup/del")
    public APIResult<Void> delUserGroup(@RequestBody UltraGroupParam param)
            throws Exception {
        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ValidateUtils.notBlank(param.getUserGroupId(),"userGroupId");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        Integer groupId = N3d.decode(param.getGroupId());
        Long userGroupId = N3d.decode2Long(param.getUserGroupId());
        ultraGroupService.delUserGroup(currentUserId, groupId,userGroupId);
        log.info("{}, {}, {}, ugId:{}, userGroupId:{}", LogType.DELETE_USERGROUP, apiParams.getCurrentUserId(), apiParams.getCurrentUserIdStr(), param.getGroupId(), param.getUserGroupId());
        return APIResultWrap.ok();
    }

    /**
     * 频道绑定用户组
     */
    @PostMapping(value = "/channel/usergroup/bind")
    public APIResult<Void> channelBindUserGroup(@RequestBody UltraGroupParam param) throws Exception {
        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ValidateUtils.notBlank(param.getChannelId(),"channelId");
        ValidateUtils.checkLength(param.getUserGroupIds(), 1, 10,"userGroupIds");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        Integer groupId = N3d.decode(param.getGroupId());
        List<Long> userGroupIds =  MiscUtils.batchDecodeLongIds(param.getUserGroupIds());
        ultraGroupService.channelBindUserGroup(currentUserId, groupId, param.getChannelId(), userGroupIds);
        log.info("{}, {}, {}, ugId:{}, channelId:{}, userGroupId:{}", LogType.BIND_USERGROUP_CHANNEL,
                apiParams.getCurrentUserId(), apiParams.getCurrentUserIdStr(), param.getGroupId(), param.getChannelId(), param.getUserGroupIds());
        return APIResultWrap.ok();
    }

    /**
     * 频道解绑用户组
     */
    @PostMapping(value = "/channel/usergroup/unbind")
    public APIResult<Void> channelUnBindUserGroup(@RequestBody UltraGroupParam param)
            throws Exception {
        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ValidateUtils.notBlank(param.getChannelId(),"channelId");
        ValidateUtils.checkLength(param.getUserGroupIds(), 1, 10,"userGroupIds");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        Integer groupId = N3d.decode(param.getGroupId());
        List<Long> userGroupIds =  MiscUtils.batchDecodeLongIds(param.getUserGroupIds());
        ultraGroupService.channelUnBindUserGroup(currentUserId, groupId, param.getChannelId(), userGroupIds);
        log.info("{}, {}, {}, ugId:{}, channelId:{}, userGroupId:{}", LogType.UNBIND_USERGROUP_CHANNEL,
                apiParams.getCurrentUserId(), apiParams.getCurrentUserIdStr(), param.getGroupId(), param.getChannelId(), param.getUserGroupIds());
        return APIResultWrap.ok();
    }


    /**
     * 用户组添加成员
     */
    @PostMapping(value = "/usergroup/member/add")
    public APIResult<Void> userGroupAddMember(@RequestBody UltraGroupParam param)
            throws Exception {
        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ValidateUtils.notBlank(param.getUserGroupId(),"userGroupId");
        ValidateUtils.checkLength(param.getMemberIds(), 1, 10,"memberIds");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        Integer groupId = N3d.decode(param.getGroupId());
        Long userGroupId = N3d.decode2Long(param.getUserGroupId());
        List<Integer> memberIds =  MiscUtils.batchDecodeIds(param.getMemberIds());
        ultraGroupService.userGroupAddMemeber(currentUserId, groupId, userGroupId,memberIds);
        log.info("{}, {}, {}, ugId:{}, userGroupId:{}, members:{}", LogType.ADD_USERGROUP_MEMBER, apiParams.getCurrentUserId(), apiParams.getCurrentUserIdStr(), param.getGroupId(), param.getUserGroupId(), param.getMemberIds());
        return APIResultWrap.ok();
    }

    /**
     * 用户组添加成员
     */
    @PostMapping(value = "/usergroup/member/del")
    public APIResult<Void> userGroupDelMember(@RequestBody UltraGroupParam param) throws Exception {
        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ValidateUtils.notBlank(param.getUserGroupId(),"userGroupId");
        ValidateUtils.checkLength(param.getMemberIds(), 1, 10,"memberIds");
        ServerApiParams apiParams = getServerApiParams();
        Integer currentUserId = getCurrentUserId();
        Integer groupId = N3d.decode(param.getGroupId());
        Long userGroupId = N3d.decode2Long(param.getUserGroupId());
        List<Integer> memberIds =  MiscUtils.batchDecodeIds(param.getMemberIds());
        ultraGroupService.userGroupDelMemeber(currentUserId, groupId,userGroupId,memberIds);
        log.info("{}, {}, {}, ugId:{}, userGroupId:{}, members:{}", LogType.REMOVE_USERGROUP_MEMBER, apiParams.getCurrentUserId(), apiParams.getCurrentUserIdStr(), param.getGroupId(), param.getUserGroupId(), param.getMemberIds());
        return APIResultWrap.ok();
    }

    /**
     * 用户组分页查询
     */
    @PostMapping(value = "/usergroup/query")
    public APIResult<List<UgUserGroupDTO>> userGroupQuery(@RequestBody UltraGroupParam param) throws Exception {
        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        int page = defaultPage(param.getPageNum());
        int pageSize = defaultPageSize(param.getLimit(), 20, 50);
        Integer currentUserId = getCurrentUserId();
        Integer groupId = N3d.decode(param.getGroupId());
        List<UgUserGroupDTO> result = ultraGroupService.queryUserGroup(currentUserId, groupId, page, pageSize);
        return APIResultWrap.ok(result);
    }


    /**
     * 频道绑定的用户组分页查询
     */
    @PostMapping(value = "/channel/usergroup/query")
    public APIResult<List<UgUserGroupDTO>> channelUserGroupQuery(@RequestBody UltraGroupParam param) throws Exception {
        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ValidateUtils.notBlank(param.getChannelId(),"channelId");
        int page = defaultPage(param.getPageNum());
        int pageSize = defaultPageSize(param.getLimit(), 20, 50);
        Integer currentUserId = getCurrentUserId();
        Integer groupId = N3d.decode(param.getGroupId());
        List<UgUserGroupDTO> result = ultraGroupService.queryChannelBindUserGroup(currentUserId, groupId, param.getChannelId(), page, pageSize);
        return APIResultWrap.ok(result);
    }

    /**
     * 用户组分页查询
     */
    @PostMapping(value = "/usergroup/member/query")
    public APIResult<List<UserDTO>> userGroupMemberQuery(@RequestBody UltraGroupParam param)
            throws Exception {
        ValidateUtils.notBlank(param.getGroupId(),"groupId");
        ValidateUtils.notBlank(param.getUserGroupId(),"userGroupId");
        int page = defaultPage(param.getPageNum());
        int pageSize = defaultPageSize(param.getLimit(), 20, 50);
        Integer currentUserId = getCurrentUserId();
        Integer groupId = N3d.decode(param.getGroupId());
        Long userGroupId = N3d.decode2Long(param.getUserGroupId());
        List<UserDTO> result = ultraGroupService.queryUserGroupMemebers(currentUserId, groupId,userGroupId, page, pageSize);
        return APIResultWrap.ok(result);
    }


    private int defaultPage(Integer page) {
        return Math.max(page == null ? 1 : page, 1);
    }

    private int defaultPageSize(Integer pageSize, int defaultSize, int maxSize) {
        return Math.min(Math.max(pageSize == null ? defaultSize : pageSize, 1), maxSize);
    }
}
