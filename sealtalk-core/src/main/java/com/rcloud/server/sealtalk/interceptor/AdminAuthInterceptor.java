package com.rcloud.server.sealtalk.interceptor;

import com.rcloud.server.sealtalk.configuration.SealtalkConfig;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.util.JacksonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AdminAuthInterceptor implements HandlerInterceptor {


    @Autowired
    private SealtalkConfig sealtalkConfig;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {


        String authKey = request.getHeader("auth-key");
        if (StringUtils.isNotBlank(sealtalkConfig.getAdminAuthKey()) && !sealtalkConfig.getAdminAuthKey().equals(authKey)){
            Map<String, String> respBody = new HashMap<>();
            respBody.put("code", String.valueOf(ErrorCode.NOT_LOGIN.getErrorCode()));
            respBody.put("msg", "admin auth error");
            response.getWriter().write(JacksonUtil.toJson(respBody));
            response.setStatus(HttpStatus.FORBIDDEN.value());
            log.info("request error auth-key [{}]", authKey);
            return false;
        }
        return true;
    }


}
