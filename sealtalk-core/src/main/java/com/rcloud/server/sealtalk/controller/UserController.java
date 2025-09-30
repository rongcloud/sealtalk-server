package com.rcloud.server.sealtalk.controller;

import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.LogType;
import com.rcloud.server.sealtalk.dto.UserParam;
import com.rcloud.server.sealtalk.entity.BlackLists;
import com.rcloud.server.sealtalk.entity.Groups;
import com.rcloud.server.sealtalk.entity.Users;
import com.rcloud.server.sealtalk.exception.ParamException;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.model.JwtTokenResult;
import com.rcloud.server.sealtalk.model.ServerApiParams;
import com.rcloud.server.sealtalk.model.UltraGroupDTO;
import com.rcloud.server.sealtalk.model.dto.BlackListDTO;
import com.rcloud.server.sealtalk.model.dto.BlackListsUserDTO;
import com.rcloud.server.sealtalk.model.dto.FavGroupInfoDTO;
import com.rcloud.server.sealtalk.model.dto.FavGroupsDTO;
import com.rcloud.server.sealtalk.model.dto.PicCodeDTO;
import com.rcloud.server.sealtalk.model.dto.UserDTO;
import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import com.rcloud.server.sealtalk.service.ConfigService;
import com.rcloud.server.sealtalk.service.LoginService;
import com.rcloud.server.sealtalk.service.UltraGroupService;
import com.rcloud.server.sealtalk.service.UsersService;
import com.rcloud.server.sealtalk.service.VerificationCodeService;
import com.rcloud.server.sealtalk.util.JacksonUtil;
import com.rcloud.server.sealtalk.util.MiscUtils;
import com.rcloud.server.sealtalk.util.N3d;
import com.rcloud.server.sealtalk.util.ValidateUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/user")
@Slf4j
public class UserController extends BaseController {

    @Autowired
    private UsersService usersService;
    @Autowired
    private UltraGroupService ultraGroupService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private ConfigService configService;

    /**
     * 获取图片验证码
     * 图片验证码5分钟内有效
     */
    @GetMapping(value = "/pic_code")
    public APIResult<PicCodeDTO> picCode() {
        PicCodeDTO picCodeDTO = verificationCodeService.pictureCode();
        return APIResultWrap.ok(picCodeDTO);
    }

    /**
     * 发送短信验证码
     */
    @PostMapping(value = "/send_code_yp")
    public APIResult<Object> sendSmsCode(@RequestBody UserParam userParam) throws Exception {

        String region = userParam.getRegion();
        String phone = userParam.getPhone();
        ValidateUtils.notBlank(region, "region");
        region = MiscUtils.removeRegionPrefix(region);
        ValidateUtils.checkNumberStr(region,"region");
        ValidateUtils.checkNumberStr(phone,"phone");
//        ValidateUtils.notBlank(userParam.getPicCode(),"smdCode");
//        ValidateUtils.notBlank(userParam.getPicCodeId(),"smdCode");

        //验证图片验证码
        if (!verificationCodeService.verifyPicCode(userParam.getPicCodeId(),userParam.getPicCode())){
            throw new ServiceException(ErrorCode.INVALID_SMS_CODE.getErrorCode(), "Invalid Picture Code");
        }

        ServerApiParams serverApiParams = getServerApiParams();

        verificationCodeService.sendSmsCode(region, phone, userParam.getDevice(), serverApiParams.getRequestUriInfo().getIp());
        return APIResultWrap.ok();

    }


    /**
     * 验证短信验证码+注册+登录
     */
    @PostMapping(value = "/verify_code_register")
    public APIResult<Object> loginOrRegister(@RequestBody UserParam userParam, HttpServletResponse response) throws Exception {
        String region = userParam.getRegion();
        String phone = userParam.getPhone();
        String code = userParam.getCode();
        String channel = userParam.getChannel();
        String version = userParam.getVersion();
        String os = userParam.getOs();
        region = MiscUtils.removeRegionPrefix(region);

        ValidateUtils.checkNumberStr(region,"region");
        ValidateUtils.checkNumberStr(phone,"phone");

        ServerApiParams serverApiParams = getServerApiParams();
        String ip = serverApiParams.getRequestUriInfo().getIp();

        //验证短信验证码
        verificationCodeService.verifySmsCode(region, phone, code);

        Triple<Integer, String, String> pairResult = usersService.register(region, phone, channel, version, os, ip);

        //设置cookie  userId加密存入cookie
        //登录成功后的其他请求，当前登录用户useId获取从cookie中获取
        loginService.setCookie(response,pairResult.getLeft());

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("id", N3d.encode(pairResult.getLeft()));
        resultMap.put("token", pairResult.getMiddle());
        resultMap.put("nickName", pairResult.getRight());

        //对result编码
        return APIResultWrap.ok(resultMap);
    }



    /**
     * 设置昵称/头像/SealTalk Account/性别/隐私设置
     */
    @PostMapping(value = {"/updateUser", "/set_nickname", "/set_portrait_uri", "/set_st_account", "/set_gender", "/set_privacy", "/set_poke"})
    public APIResult<Object> updateUser(@RequestBody UserParam userParam) throws Exception {
        String nickname = MiscUtils.xss_null(userParam.getNickname(), ValidateUtils.NICKNAME_MAX_LENGTH);
        String portraitUri = MiscUtils.xss_null(userParam.getPortraitUri(), ValidateUtils.PORTRAIT_URI_MAX_LENGTH);
        ValidateUtils.checkStAccount(userParam.getStAccount(),"stAccount");
        ValidateUtils.checkGender(userParam.getGender(),"gender");
        ValidateUtils.checkBooleanNum(userParam.getPhoneVerify(),"phoneVerify",false);
        ValidateUtils.checkBooleanNum(userParam.getStSearchVerify(),"stSearchVerify",false);
        ValidateUtils.checkBooleanNum(userParam.getFriVerify(),"friVerify",false);
        ValidateUtils.checkBooleanNum(userParam.getGroupVerify(),"groupVerify",false);
        ValidateUtils.checkBooleanNum(userParam.getPokeStatus(),"pokeStatus",false);

        Users updateUser = new Users();
        updateUser.setId(getCurrentUserId());
        updateUser.setNickname(nickname);
        updateUser.setPortraitUri(portraitUri);
        updateUser.setGender(userParam.getGender());
        updateUser.setStAccount(userParam.getStAccount());
        updateUser.setPhoneVerify(userParam.getPhoneVerify());
        updateUser.setStSearchVerify(userParam.getStSearchVerify());
        updateUser.setFriVerify(userParam.getFriVerify());
        updateUser.setGroupVerify(userParam.getGroupVerify());
        updateUser.setPokeStatus(userParam.getPokeStatus());
        usersService.updateUserInfo(updateUser);
        return APIResultWrap.ok();
    }



    /**
     * 获取个人隐私设置
     */
    @GetMapping(value = "/get_privacy")
    public APIResult<Object> getPrivacy() throws ServiceException {
        Integer currentUserId = getCurrentUserId();
        Users users = usersService.queryById(currentUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("phoneVerify", users.getPhoneVerify());
        result.put("stSearchVerify", users.getStSearchVerify());
        result.put("friVerify", users.getFriVerify());
        result.put("groupVerify", users.getGroupVerify());
        return APIResultWrap.ok(result);
    }


    /**
     * 获取区域信息
     */
    @GetMapping(value = "/regionlist")
    public APIResult<Object> regionlist() throws ServiceException {
        return APIResultWrap.ok(configService.getRegionList());
    }


    /**
     * 用户登出
     */
    @PostMapping(value = "/logout")
    public APIResult<Object> logout(HttpServletResponse response) {
        ServerApiParams apiParams = getServerApiParams();
        log.info("{},{},{}", LogType.LOGOUT, apiParams.getCurrentUserId(), apiParams.getCurrentUserIdStr());
        loginService.removeCookie(apiParams.getCurrentUserId(), apiParams.getTokenId(), response);
        return APIResultWrap.ok();
    }


    /**
     * 用户注销 会删除用户所有数据
     */
    @PostMapping(value = "/del")
    public APIResult<Object> del(HttpServletResponse response) throws ServiceException {
        ServerApiParams params = getServerApiParams();
        log.info("{},{},{}", LogType.DELETE, params.getCurrentUserId(), params.getCurrentUserIdStr());
        usersService.del(params.getCurrentUserId());
        loginService.removeCookie(params.getCurrentUserId(), params.getTokenId(), response);
        return APIResultWrap.ok();
    }



    /**
     * 获取RongCloud token
     */
    @GetMapping(value = "/get_token")
    public APIResult<Object> getRcToken() throws Exception {
        Integer currentUserId = getCurrentUserId();
        String currentUserIdStr = getServerApiParams().getCurrentUserIdStr();
        String token = usersService.getRcToken(currentUserId, currentUserIdStr);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("id", currentUserIdStr);
        resultMap.put("token", token);
        return APIResultWrap.ok(resultMap);
    }


    /**
     * 获取当前用户黑名单列表
     */
    @GetMapping(value = "/blacklist")
    public APIResult<Object> blacklist() throws ServiceException {

        Integer currentUserId = getCurrentUserId();

        List<BlackLists> resultList = usersService.getBlackList(currentUserId);

        List<BlackListDTO>  BlackListDTOList = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMATR_PATTERN);

        if(!CollectionUtils.isEmpty(resultList)) {

            for (BlackLists blackLists : resultList) {
                BlackListDTO blackListDTO = new BlackListDTO();
                Users users = blackLists.getUsers();
                BlackListsUserDTO dto = new BlackListsUserDTO();
                dto.setId(N3d.encode(users.getId()));
                dto.setGender(users.getGender());
                dto.setNickname(users.getNickname());
                dto.setPortraitUri(users.getPortraitUri());
                dto.setStAccount(users.getStAccount());
                dto.setUpdatedAt(sdf.format(users.getUpdatedAt()));
                dto.setUpdatedTime(users.getUpdatedAt().getTime());
                blackListDTO.setUser(dto);
                BlackListDTOList.add(blackListDTO);
            }
        }
        return APIResultWrap.ok(BlackListDTOList);
    }


    /**
     * 将好友加入黑名单
     */
    @PostMapping(value = "/add_to_blacklist")
    public APIResult<Object> addBlackList(@RequestBody UserParam userParam) throws Exception {
        String friendId = userParam.getFriendId();
        ValidateUtils.notBlank(friendId,"friendId");

        Integer currentUserId = getCurrentUserId();
        usersService.addBlackList(currentUserId, N3d.decode(friendId), true);
        return APIResultWrap.ok();
    }


    /**
     * 将好友移除黑名单
     */
    @PostMapping(value = "remove_from_blacklist")
    public APIResult<Object> removeBlacklist(@RequestBody UserParam userParam) throws Exception {
        String friendId = userParam.getFriendId();
        ValidateUtils.notBlank(friendId,"friendId");

        Integer currentUserId = getCurrentUserId();
        usersService.removeBlackList(currentUserId, N3d.decode(friendId));
        return APIResultWrap.ok();
    }


    /**
     * 获取七牛云文件存储token
     */
    @GetMapping(value = "/get_image_token")
    public APIResult<Object> getImageToken() throws ServiceException {

        String token = usersService.getImageToken();

        Map<String, Object> map = new HashMap<>();
        map.put("target", "qiniu");
        map.put("domain", sealtalkConfig.getQiniuBucketDomain());
        map.put("token", token);
        return APIResultWrap.ok(map);
    }


    /**
     * 获取当前用户所属群组
     */
    @GetMapping(value = "/groups")
    public APIResult<Object> getGroups() throws ServiceException {
        Integer currentUserId = getCurrentUserId();
        List<Groups> groupsList = usersService.getGroups(currentUserId);
        List<Map<String,Object>> resultMapList = new ArrayList<>();
        for (Groups g : groupsList) {
            String json = JacksonUtil.toJson(g);
            Map<String, Object> tmp = JacksonUtil.fromJson(json, Map.class);
            tmp.put("id", N3d.encode(g.getId()));
            tmp.put("creatorId", N3d.encode(g.getCreatorId()));
            resultMapList.add(tmp);
        }
        return APIResultWrap.ok(resultMapList);
    }


    /**
     * 根据手机号查找用户信息
     */
    @GetMapping(value = "/find/{region}/{phone}")
    public APIResult<Object> getUserByPhone(@PathVariable("region") String region, @PathVariable("phone") String phone) throws Exception {
        ValidateUtils.checkNumberStr(region,"region");
        ValidateUtils.checkNumberStr(phone,"phone");
        Users users = usersService.queryUserByPhone(region, phone);
        if (users != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", N3d.encode(users.getId()));
            map.put("nickname", users.getNickname());
            map.put("portraitUri", users.getPortraitUri());
            return APIResultWrap.ok(map);
        }
        return APIResultWrap.ok();
    }

    /**
     * 根据手机号查找用户信息
     */
    @GetMapping(value = "/find_user")
    public APIResult<Object> getUserByPhoneOrAccount(@RequestParam(value = "region", required = false) String region,
                                                     @RequestParam(value = "phone", required = false) String phone,
                                                     @RequestParam(value = "st_account", required = false) String account) throws Exception {

        if ((StringUtils.isBlank(region) || StringUtils.isBlank(phone)) && StringUtils.isBlank(account)) {
            throw new ParamException(ErrorCode.PARAM_ERROR.getErrorCode(), "Parameter error,Please check.");
        }
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotBlank(region) && StringUtils.isNotBlank(phone) && MiscUtils.isNumberStr(MiscUtils.removeRegionPrefix(region)) && MiscUtils.isNumberStr(phone)) {
            region = MiscUtils.removeRegionPrefix(region);
            Users users = usersService.queryUserByPhone(region, phone);
            if (users != null && Users.PHONE_VERIFY_NO_NEED.equals(users.getPhoneVerify())) {
                //用户存在，并且用户允许通过手机号搜索到我
                map.put("id", N3d.encode(users.getId()));
                map.put("nickname", users.getNickname());
                map.put("portraitUri", users.getPortraitUri());
                return APIResultWrap.ok(map);
            }
        }

        if (StringUtils.isNotEmpty(account)) {
            Users users = usersService.queryUserByStAccount(account);
            if (users != null && Users.ST_SEARCH_VERIFY_NO_NEED.equals(users.getPhoneVerify())) {
                // 用户存在并且 用户允许通过st账号搜索到我
                map.put("id", N3d.encode(users.getId()));
                map.put("nickname", users.getNickname());
                map.put("portraitUri", users.getPortraitUri());
                return APIResultWrap.ok(map);
            }
        }

        return APIResultWrap.ok(map, "查无此人");
    }

    /**
     * 获取用户信息
     */
    @GetMapping(value = "/{id}")
    public APIResult<Object> getUserInfo(@PathVariable("id") String id) throws ServiceException {

        Integer userId = N3d.decode(id);
        Users users = usersService.queryById(userId);
        if (users != null) {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(N3d.encode(users.getId()));
            userDTO.setNickname(users.getNickname());
            userDTO.setPortraitUri(users.getPortraitUri());
            userDTO.setGender(users.getGender());
            userDTO.setStAccount(users.getStAccount());
            return APIResultWrap.ok(userDTO);
        }
        return APIResultWrap.ok();
    }

    /**
     * 获取通讯录群组
     */
    @GetMapping(value = "/favgroups")
    public APIResult<Object> getFavGroups(@RequestParam(value = "limit", required = false) Integer limit,
                                          @RequestParam(value = "offset", required = false) Integer offset) throws ServiceException {

        Integer currentUserId = getCurrentUserId();
        Pair<Integer, List<Groups>> result = usersService.getFavGroups(currentUserId, limit, offset);

        Integer count = result.getLeft();
        List<Groups> groupsList = result.getRight();

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMATR_PATTERN);

        List<FavGroupInfoDTO> favGroupInfoDTOS = new ArrayList<>();
        if (!CollectionUtils.isEmpty(groupsList)) {
            for (Groups groups : groupsList) {
                FavGroupInfoDTO favGroupInfoDTO = new FavGroupInfoDTO();
                favGroupInfoDTO.setId(N3d.encode(groups.getId()));
                favGroupInfoDTO.setName(groups.getName());
                favGroupInfoDTO.setPortraitUri(groups.getPortraitUri());
                favGroupInfoDTO.setMemberCount(groups.getMemberCount());
                favGroupInfoDTO.setMaxMemberCount(groups.getMaxMemberCount());
                favGroupInfoDTO.setMemberProtection(groups.getMemberProtection());
                favGroupInfoDTO.setCreatorId(N3d.encode(groups.getCreatorId()));
                favGroupInfoDTO.setIsMute(groups.getIsMute());
                favGroupInfoDTO.setCertiStatus(groups.getCertiStatus());
                favGroupInfoDTO.setCreatedAt(sdf.format(groups.getCreatedAt()));
                favGroupInfoDTO.setUpdatedAt(sdf.format(groups.getUpdatedAt()));
                favGroupInfoDTO.setCreatedTime(groups.getCreatedAt().getTime());
                favGroupInfoDTO.setUpdatedTime(groups.getUpdatedAt().getTime());
                favGroupInfoDTOS.add(favGroupInfoDTO);
            }
        }

        FavGroupsDTO favGroupsDTO = new FavGroupsDTO();
        favGroupsDTO.setLimit(limit);
        favGroupsDTO.setOffset(offset);
        favGroupsDTO.setTotal(count);
        favGroupsDTO.setList(favGroupInfoDTOS);

        return APIResultWrap.ok(favGroupsDTO);
    }



    /**
     * 获取接收戳一下消息状态
     */
    @GetMapping(value = "/get_poke")
    public APIResult<Object> getPokeStatus() throws ServiceException {

        Integer currentUserId = getCurrentUserId();
        Users users = usersService.queryById(currentUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("pokeStatus", users.getPokeStatus());
        return APIResultWrap.ok(result);
    }


    /**
     * 查询用户所在的超级群组
     */
    @PostMapping(value = "/ultragroups")
    public APIResult<Object> ultragroups() throws ServiceException {

        Integer currentUserId = getCurrentUserId();
        List<UltraGroupDTO> ultraGroupDTOList = ultraGroupService.getUltragroups(currentUserId);
        return APIResultWrap.ok(ultraGroupDTOList);
    }

    @GetMapping(value = "/getJwtToken")
    public JwtTokenResult getJwtToken(@RequestParam("userId") String userId) throws Exception {
        JwtTokenResult jwtTokenResult = usersService.getJwtToken(userId);
        return jwtTokenResult;
    }

    @GetMapping(value = "/getUserInfo")
    public APIResult<Object> getUserInfo (
        @RequestParam(value = "id", required = false) String id,
        @RequestParam(value = "region", required = false) String region,
        @RequestParam(value = "phone", required = false) String phone) throws ServiceException {
        Users users = null;
        if(StringUtils.isNotBlank(id)){
            Integer userId = N3d.decode(id);
            users = usersService.queryById(userId);
        } else if (StringUtils.isNotBlank(region) && StringUtils.isNotBlank(phone)){
            users = usersService.queryUserByPhone(region, phone);
        }
        if (users != null) {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(N3d.encode(users.getId()));
            userDTO.setNickname(users.getNickname());
            userDTO.setPortraitUri(users.getPortraitUri());
            userDTO.setGender(users.getGender());
            userDTO.setStAccount(users.getStAccount());
            return APIResultWrap.ok(userDTO);
        }
        return APIResultWrap.ok();
    }



}
