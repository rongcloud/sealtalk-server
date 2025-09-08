package com.rcloud.server.sealtalk.controller;

import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.model.dto.ShumeiVerifyDTO;
import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import java.util.HashMap;
import java.util.Map;

import com.rcloud.server.sealtalk.service.ShumeiService;
import com.rcloud.server.sealtalk.util.MiscUtils;
import com.rcloud.server.sealtalk.util.ValidateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数美相关接口
 */
@RestController
@RequestMapping("/shumei")
@Slf4j
public class ShumeiController extends BaseController {

    @Autowired
    private ShumeiService shumeiService;

    /**
     * 查询数美开关状态
     */
    @GetMapping(value = "/status")
    public APIResult<Object> getStatus() throws ServiceException {
        boolean status = shumeiService.getStatus(getCurrentUserId());
        Map<String, Boolean> result = new HashMap<>();
        result.put("openEnable", status);
        return APIResultWrap.ok(result);
    }

    /**
     * 数美黑产验证
     */
    @GetMapping(value = "/verify")
    public APIResult<Object> verify (
        @RequestParam(value = "deviceId") String deviceId,
        @RequestParam(value = "region") String region,
        @RequestParam(value = "phone") String phone,
        @RequestParam(value = "os") String os
    ) throws Exception {
        ValidateUtils.notBlank(deviceId,"deviceId");
        ValidateUtils.notBlank(region,"region");
        ValidateUtils.notBlank(phone,"phone");

        region = MiscUtils.removeRegionPrefix(region);
        ValidateUtils.checkNumberStr(region,"region");
        ValidateUtils.checkNumberStr(phone,"phone");
        ShumeiVerifyDTO dto = shumeiService.verify(deviceId, region, phone, getIp());
        return APIResultWrap.ok(dto);
    }
}
