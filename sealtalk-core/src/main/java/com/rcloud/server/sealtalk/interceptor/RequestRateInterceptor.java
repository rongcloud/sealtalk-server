package com.rcloud.server.sealtalk.interceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.model.RequestUriInfo;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.rcloud.server.sealtalk.util.JacksonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author jianzheng.li
 */
@Component
public class RequestRateInterceptor implements HandlerInterceptor {


    private static final Cache<String, AtomicInteger> IP_URL = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .maximumSize(5000)
            .build();


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        RequestUriInfo  requestUriInfo = ServerApiParamHolder.get().getRequestUriInfo();
        if (StringUtils.isBlank(requestUriInfo.getIp()) || StringUtils.isBlank(requestUriInfo.getUri())) {
            return true;
        }
        String key = requestUriInfo.getIp() + "_" + requestUriInfo.getUri();
        AtomicInteger count = IP_URL.get(key, k -> new AtomicInteger());
        count.incrementAndGet();
        if (count.get() > 3) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write(JacksonUtil.toJson(APIResultWrap.error(ErrorCode.OVER_LIMIT.getErrorCode(), HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())));
            return false;
        }
        return true;
    }
}
