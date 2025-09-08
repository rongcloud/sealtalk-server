package com.rcloud.server.sealtalk.interceptor;

import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.model.ServerApiParams;
import com.rcloud.server.sealtalk.service.LoginService;
import com.rcloud.server.sealtalk.util.JacksonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class RequestInterceptor implements HandlerInterceptor {


    @Autowired
    private LoginService loginService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        ServerApiParams serverApiParams = ServerApiParamHolder.get();
        try {
            Triple<Integer, String, Long> userId = loginService.getCurrentUserId(request);
            serverApiParams.setCurrentUserId(userId.getLeft());
            serverApiParams.setCurrentUserIdStr(userId.getMiddle());
            serverApiParams.setTokenId(userId.getRight());
            log.info("request userId:[{}], [{}], tokenId:[{}]", userId.getLeft(), userId.getMiddle(), userId.getRight());
        } catch (Exception e) {
            log.error("获取currentUserId异常, {} ", e.getMessage());
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write(notLoginBody());
            return false;
        }
        return true;
    }

    private String notLoginBody() throws ServiceException {
        Map<String, String> respBody = new HashMap<>();
        respBody.put("code", String.valueOf(ErrorCode.NOT_LOGIN.getErrorCode()));
        respBody.put("msg", ErrorCode.NOT_LOGIN.getErrorMessage());
        return JacksonUtil.toJson(respBody);
    }

}
