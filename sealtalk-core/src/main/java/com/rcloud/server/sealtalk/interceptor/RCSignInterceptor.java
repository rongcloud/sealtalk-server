package com.rcloud.server.sealtalk.interceptor;

import com.rcloud.server.sealtalk.configuration.SealtalkConfig;
import com.rcloud.server.sealtalk.util.RongCloudApiUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author jianzheng.li
 */
@Component
@Slf4j
public class RCSignInterceptor implements HandlerInterceptor {


    private static final String APP_KEY = "appKey";
    private static final String NONCE = "nonce";
    private static final String TIMESTAMP = "timestamp";
    private static final String SIGNATURE = "signature";

    private final static String RC_APP_KEY = "RC-App-Key";
    private final static String RC_TIMESTAMP = "RC-Timestamp";
    private final static String RC_NONCE = "RC-Nonce";
    private final static String RC_SIGNATURE = "RC-Signature";

    @Autowired
    private SealtalkConfig config;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {


        String appKey = tryGet(request.getParameter(APP_KEY), request.getHeader(RC_APP_KEY));
        if (!config.getRongcloudAppKey().equals(appKey)) {
            log.error("rc appKey error: {}", appKey);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("appKey error");
            return false;
        }

        String nonce = tryGet(request.getParameter(NONCE), request.getHeader(RC_NONCE));
        String timestamp = tryGet(request.getParameter(TIMESTAMP), request.getHeader(RC_TIMESTAMP));
        String signature = tryGet(request.getParameter(SIGNATURE), request.getHeader(RC_SIGNATURE));
        try {
            String sign = RongCloudApiUtil.hexSHA1(config.getRongcloudAppSecret() + nonce + timestamp);
            if (!sign.equals(signature)) {
                log.error("rc sign error, appKey:{}, nonce:{}, timestamp:{}, signature:{} ", appKey, nonce, timestamp, signature);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("signature error");
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("rc sign error", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("signature error");
            return false;
        }
    }

    private String tryGet(String s1, String s2) {
        if (StringUtils.isBlank(s1)) {
            return s2;
        }
        return s1;
    }

}
