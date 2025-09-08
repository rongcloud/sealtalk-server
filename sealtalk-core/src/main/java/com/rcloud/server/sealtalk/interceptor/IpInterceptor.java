package com.rcloud.server.sealtalk.interceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rcloud.server.sealtalk.constant.WhiteListTypeEnum;
import com.rcloud.server.sealtalk.entity.WhiteList;
import com.rcloud.server.sealtalk.model.RequestUriInfo;
import com.rcloud.server.sealtalk.model.ServerApiParams;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rcloud.server.sealtalk.service.WhiteListService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author jianzheng.li
 */
@Component
@Slf4j
public class IpInterceptor implements HandlerInterceptor {

    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String UNKNOWN = "unknown";
    public static final String PROXY_CLIENT_IP = "Proxy-Client-IP";
    public static final String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
    public static final String HTTP_CLIENT_IP = "HTTP_CLIENT_IP";
    public static final String HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR";
    public static final String USER_AGENT = "User-Agent";

    private static final String MDC_TRACE_ID = "traceId";


    public static final AtomicBoolean LOAD_OK = new AtomicBoolean(false);
    private static final Set<String> BLOCK_IP = new HashSet<>();

    @Autowired
    private WhiteListService whiteListService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        MDC.put(MDC_TRACE_ID, UUID.randomUUID().toString());
        ServerApiParams serverApiParams = new ServerApiParams();
        RequestUriInfo requestUriInfo = getRequestUriInfo(request);
        serverApiParams.setRequestUriInfo(requestUriInfo);
        ServerApiParamHolder.put(serverApiParams);
        reloadBlockIp();
        if (BLOCK_IP.contains(requestUriInfo.getIp())) {
            response.getWriter().write("request not support");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        String originHeader = request.getHeader("Origin");
        log.info("request ip:[{}], port:[{}], userAgent:[{}], uri:[{}][{}], origin:[{}]", requestUriInfo.getIp(), requestUriInfo.getPort(), requestUriInfo.getUserAgent(), requestUriInfo.getMethod(), requestUriInfo.getUri(), originHeader);
        if(request.getMethod().equals("OPTIONS")){
            response.setStatus(HttpServletResponse.SC_OK);
            return false;
        }
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
        Object handler, Exception ex) throws Exception {
        ServerApiParamHolder.remove();
        MDC.clear();
    }


    public void reloadBlockIp(){
        if (LOAD_OK.get()){
            return;
        }
        List<WhiteList> whiteLists = whiteListService.queryByType(WhiteListTypeEnum.BLOCK_IP);
        BLOCK_IP.clear();
        if (whiteLists != null){
            whiteLists.forEach(w -> BLOCK_IP.add(w.getPhone()));
        }
        LOAD_OK.set(true);
    }



    protected RequestUriInfo getRequestUriInfo(HttpServletRequest request) {
        String ip = getIpAddress(request);
        ip = StringUtils.isEmpty(ip) ? "" : ip;
        String uri = request.getRequestURI();
        String remoteAddress = request.getRemoteAddr();
        Integer remotePort = request.getRemotePort();
        RequestUriInfo requestUriInfo = new RequestUriInfo();
        requestUriInfo.setMethod(request.getMethod());
        requestUriInfo.setUri(uri);
        requestUriInfo.setRemoteAddress(remoteAddress);
        requestUriInfo.setIp(ip);
        requestUriInfo.setPort(remotePort);
        requestUriInfo.setUserAgent(request.getHeader(USER_AGENT));
        return requestUriInfo;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader(X_FORWARDED_FOR);
        if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader(PROXY_CLIENT_IP);
        }
        if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader(WL_PROXY_CLIENT_IP);
        }
        if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader(HTTP_CLIENT_IP);
        }
        if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader(HTTP_X_FORWARDED_FOR);
        }
        if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (!StringUtils.isEmpty(ip) && ip.contains(",")) {
            ip = ip.split(",")[0];
        }
        return ip;
    }

}
