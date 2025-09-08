package com.rcloud.server.sealtalk.service;

import com.rcloud.server.sealtalk.configuration.SealtalkConfig;
import com.rcloud.server.sealtalk.constant.ConfigKeyEnum;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.ErrorMsg;
import com.rcloud.server.sealtalk.constant.WhiteListTypeEnum;
import com.rcloud.server.sealtalk.dao.RiskListMapper;
import com.rcloud.server.sealtalk.entity.RiskList;
import com.rcloud.server.sealtalk.entity.Users;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.model.ShumeiParams;
import com.rcloud.server.sealtalk.model.ShumeiRequestParams;
import com.rcloud.server.sealtalk.model.ShumeiResponseDetail;
import com.rcloud.server.sealtalk.model.ShumeiResponseResult;
import com.rcloud.server.sealtalk.model.dto.ShumeiVerifyDTO;
import com.rcloud.server.sealtalk.util.AES256;
import com.rcloud.server.sealtalk.util.JacksonUtil;
import com.rcloud.server.sealtalk.util.ThreadFactoryUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
@Slf4j
public class ShumeiService {


    private static final int SHUMEI_SUCCESS = 1100;
    private static final String SHUMEI_OPENED = "1";
    private static final String SHUMEI_CLOSED = "0";
    public static final String SHUMEI_EVENTID_REGISTER = "register";
    public static final String SHUMEI_EVENTID_LOGIN = "login";
    public static final String SHUMEI_RISK_LEVEL_PASS = "PASS";
    public static final String SHUMEI_RISK_LEVEL_REJECT = "REJECT";

    @Autowired
    private ConfigService configService;

    @Autowired
    private RiskListMapper riskListMapper;

    @Autowired
    private UsersService usersService;

    @Autowired
    private WhiteListService whiteListService;

    @Autowired
    private SealtalkConfig sealtalkConfig;

    @Autowired
    private HttpService httpService;

    public boolean getStatus(Integer userId) {
        String openStatus = configService.getConfig(ConfigKeyEnum.SHU_MEI_STATUS.getKey());
        if (StringUtils.isBlank(openStatus) || SHUMEI_CLOSED.equals(openStatus)) {
            return false;
        }
        if (userId == null) {
            return true;
        }
        Users u = usersService.queryById(userId);
        return !whiteListService.whiteCheck(u.getRegion(),u.getPhone(), WhiteListTypeEnum.LOGIN_RISK_VERIFY);
    }

    public ShumeiVerifyDTO verify(String deviceId, String region, String phone, String ip) throws ServiceException {

        Users u = usersService.queryUserByPhone(region, phone);
        if (u == null) {
            throw ServiceException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "user");
        }
        if (whiteListService.whiteCheck(u.getRegion(),u.getPhone(), WhiteListTypeEnum.LOGIN_RISK_VERIFY)){
            return new ShumeiVerifyDTO(SHUMEI_RISK_LEVEL_PASS);
        }
        RiskList riskList = riskListMapper.selectByUserIdAndDeviceId(u.getId(),deviceId);
        if (riskList == null) {
            riskList = requestShumei(SHUMEI_EVENTID_REGISTER, ip, u.getId(), deviceId);
            if (riskList != null) {
                riskListMapper.insertSelective(riskList);
            }
        }

        ShumeiVerifyDTO dto = new ShumeiVerifyDTO(SHUMEI_RISK_LEVEL_PASS);
        riskList = requestShumei(SHUMEI_EVENTID_LOGIN, ip, u.getId(), deviceId);
        if (riskList != null){
            riskListMapper.updateByUserIdAndDeviceIdSelective(riskList);
            dto.setRiskLevel(riskList.getRiskStatus());
        }
        ThreadFactoryUtil.ofVirtual(() -> {
            Users update = new Users();
            update.setId(u.getId());
            update.setLastIp(ip);
            usersService.updateUserInfo(update);
            if (SHUMEI_RISK_LEVEL_REJECT.equalsIgnoreCase(dto.getRiskLevel())){
                usersService.blockStatus(u.getId(), true, null);
            }
        });
        return dto;
    }



    private RiskList requestShumei(String eventId, String ip, int userPkId, String deviceId) {
        try {
            String accessKey = sealtalkConfig.getShumeiAccessKey();
            String appId = sealtalkConfig.getShumeiAppId();
            ShumeiRequestParams requestParams = new ShumeiRequestParams(userId2TokenId(userPkId),ip,System.currentTimeMillis(),deviceId);
            ShumeiParams<ShumeiRequestParams> params = new ShumeiParams<>(accessKey, appId, eventId, requestParams);
            String body = JacksonUtil.toJson(params);
            String result = httpService.postJson(sealtalkConfig.getShumeiApiUrl(), body);
            ShumeiResponseResult registerResult = JacksonUtil.fromJson(result, ShumeiResponseResult.class);
            int code = registerResult.getCode();
            if (SHUMEI_SUCCESS != code) {
                return null;
            }
            RiskList rl = new RiskList();
            rl.setDeviceId(deviceId);
            rl.setUserId(userPkId);
            rl.setIp(ip);
            rl.setRiskStatus(registerResult.getRiskLevel());
            rl.setOtherDetail(result);
            ShumeiResponseDetail detail = registerResult.getDetail();
            if (detail != null) {
                rl.setDetail(registerResult.getDetail().getDescription());
            }
            return rl;
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    private String userId2TokenId(Integer userPkId) {
        byte[] encrypt = AES256.encrypt(userPkId.toString(), sealtalkConfig.getN3dKey());
        if (encrypt != null) {
            return new String(encrypt);
        }
        return "";
    }

}
