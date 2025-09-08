package com.rcloud.server.sealtalk.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rcloud.server.sealtalk.constant.ConfigKeyEnum;
import com.rcloud.server.sealtalk.dto.ScreenCaptureParam;
import com.rcloud.server.sealtalk.dto.SendMessageContent;
import com.rcloud.server.sealtalk.dto.SendMessageParam;
import com.rcloud.server.sealtalk.entity.ScreenStatuses;
import com.rcloud.server.sealtalk.model.dto.DemoSquareDTO;
import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import com.rcloud.server.sealtalk.service.ConfigService;
import com.rcloud.server.sealtalk.service.MiscService;
import com.rcloud.server.sealtalk.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/misc")
@Slf4j
public class MiscController extends BaseController {


    @Autowired
    private MiscService miscService;

    @Autowired
    private ConfigService configService;

    /**
     * Android、iOS 获取更新版本
     */
    @GetMapping(value = {"/client_version", "/mobile_version"})
    public String getClientVersion() {
        return configService.getConfig(ConfigKeyEnum.MOBILE_VERSION.getKey());
    }


    /**
     * Android、iOS 获取更新版本
     */
    @GetMapping(value = "/demo_square")
    public APIResult<?> getDemoSquare() {

        try {
            String demoSquare = configService.getConfig(ConfigKeyEnum.DEMO_SQUARE.getKey());
            if (StringUtils.isBlank(demoSquare)) {
                return APIResultWrap.ok();
            }
            List<DemoSquareDTO> demoSquareDTOS = JacksonUtil.fromJson(demoSquare, new TypeReference<List<DemoSquareDTO>>() {
            });
            if (demoSquareDTOS == null || demoSquareDTOS.isEmpty()) {
                return APIResultWrap.ok();
            }
            for (DemoSquareDTO dto : demoSquareDTOS) {
                dto.setId(N3d.encode(Integer.parseInt(dto.getId())));
            }
            return APIResultWrap.ok(demoSquareDTOS);
        } catch (Exception e) {
            log.error("", e);
        }
        return APIResultWrap.ok();
    }

    /**
     * Server API 发送消息
     */
    @PostMapping(value = "/send_message")
    public APIResult<Object> sendMessage(@RequestBody SendMessageParam sendMessageParam) throws Exception {

        String conversationType = sendMessageParam.getConversationType();
        String targetId = sendMessageParam.getTargetId();
        String objectName = sendMessageParam.getObjectName();
        SendMessageContent content = sendMessageParam.getContent();
        String pushContent = sendMessageParam.getPushContent();

        ValidateUtils.notBlank(conversationType,"conversationType");
        ValidateUtils.notBlank(targetId,"targetId");
        ValidateUtils.notBlank(objectName,"objectName");
        ValidateUtils.notNull(content,"content");

        Integer currentUserId = getCurrentUserId();
        miscService.sendMessage(currentUserId, conversationType, N3d.decode(targetId), objectName, content, pushContent, targetId);
        return APIResultWrap.ok();
    }


    /**
     * 截屏通知状态设置
     */
    @PostMapping(value = "/set_screen_capture")
    public APIResult<Object> setScreenCapture(@RequestBody ScreenCaptureParam screenCaptureParam) throws Exception {

        Integer conversationType = screenCaptureParam.getConversationType();
        String targetId = screenCaptureParam.getTargetId();
        Integer noticeStatus = screenCaptureParam.getNoticeStatus();

        ValidateUtils.notNull(conversationType,"conversationType");
        ValidateUtils.notBlank(targetId,"targetId");
        ValidateUtils.notNull(noticeStatus,"noticeStatus");
        Integer currentUserId = getCurrentUserId();
        miscService.setScreenCapture(currentUserId, N3d.decode(targetId), conversationType, noticeStatus);
        return APIResultWrap.ok();
    }


    /**
     * 获取截屏通知状态
     */
    @PostMapping(value = "/get_screen_capture")
    public APIResult<Object> getScreenCapture(@RequestBody ScreenCaptureParam screenCaptureParam) throws Exception {

        Integer conversationType = screenCaptureParam.getConversationType();
        String targetId = screenCaptureParam.getTargetId();
        ValidateUtils.notNull(conversationType,"conversationType");
        ValidateUtils.notBlank(targetId,"targetId");
        Integer currentUserId = getCurrentUserId();
        ScreenStatuses screenStatuses = miscService.getScreenCapture(currentUserId, N3d.decode(targetId), conversationType);
        Map<String, Object> result = new HashMap<>();
        result.put("status", screenStatuses == null ? 0 : screenStatuses.getStatus());
        return APIResultWrap.ok(result);
    }

    /**
     * 发送截屏通知消息
     */
    @PostMapping(value = "/send_sc_msg")
    public APIResult<Object> sendScreenCaptureMsg(@RequestBody ScreenCaptureParam screenCaptureParam) throws Exception {

        Integer conversationType = screenCaptureParam.getConversationType();
        String targetId = screenCaptureParam.getTargetId();
        ValidateUtils.notNull(conversationType,"conversationType");
        ValidateUtils.notBlank(targetId,"targetId");
        Integer currentUserId = getCurrentUserId();
        miscService.sendScreenCaptureMsg(currentUserId, N3d.decode(targetId), conversationType);
        return APIResultWrap.ok();
    }


}
