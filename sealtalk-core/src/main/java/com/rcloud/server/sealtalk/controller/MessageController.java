package com.rcloud.server.sealtalk.controller;

import com.rcloud.server.sealtalk.aspect.UnLogAspect;
import com.rcloud.server.sealtalk.dto.CallbackParam;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import com.rcloud.server.sealtalk.param.MsgRouteParam;
import com.rcloud.server.sealtalk.service.MsgCallbackService;
import com.rcloud.server.sealtalk.util.ThreadFactoryUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;


/**
 * 审核结果回调
 */
@RestController
@RequestMapping("/message")
@Slf4j
public class MessageController {

    @Autowired
    private MsgCallbackService msgCallbackService;

    /**
     * 接收回调的审核结果
     */
    @UnLogAspect
    @PostMapping(value = "/callback")
    public APIResult<Object> callback(@RequestBody CallbackParam param) throws ServiceException {
        return APIResultWrap.ok();
    }


    @UnLogAspect
    @PostMapping("/msgRoute")
    public void msgRoute(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            MsgRouteParam param = new MsgRouteParam().init(request);
            ThreadFactoryUtil.ofVirtual(() -> msgCallbackService.sendWarningNotify(param));
        } finally {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("ok");
        }
    }
}
