package com.rcloud.server.sealtalk.configuration;

import com.rcloud.server.sealtalk.interceptor.AdminAuthInterceptor;
import com.rcloud.server.sealtalk.interceptor.IpInterceptor;
import com.rcloud.server.sealtalk.interceptor.RCSignInterceptor;
import com.rcloud.server.sealtalk.interceptor.RequestInterceptor;
import com.rcloud.server.sealtalk.interceptor.RequestRateInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Autowired
    private RequestInterceptor requestInterceptor;

    @Autowired
    private IpInterceptor ipInterceptor;

    @Autowired
    private RCSignInterceptor rcSignInterceptor;

    @Autowired
    private RequestRateInterceptor requestRateInterceptor;

    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;

    @Autowired
    private SealtalkConfig sealtalkConfig;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(ipInterceptor)
                .addPathPatterns("/**")
                .order(1);
        registry.addInterceptor(requestRateInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/message/**")
                .order(2);
        registry.addInterceptor(rcSignInterceptor)
                .addPathPatterns("/message/**")
                .order(3);
        registry.addInterceptor(requestInterceptor)
                .addPathPatterns("/friendship/**")
                .addPathPatterns("/user/**")
                .addPathPatterns("/misc/**")
                .addPathPatterns("/group/**")
                .addPathPatterns("/shumei/**")
                .addPathPatterns("/ultragroup/**")
                .addPathPatterns("/customer/**")
                .addPathPatterns("/ai/**")
                .excludePathPatterns("/shumei/status")
                .excludePathPatterns("/user/pic_code", "/user/getJwtToken", "/user/send_code_yp", "/user/verify_code_register", "/user/regionlist")
                .excludePathPatterns("/misc/demo_square", "/misc/client_version", "/misc/mobile_version")
                .excludePathPatterns("/message/**")
                .order(4);
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")
                .order(5);
    }


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] corsHosts = sealtalkConfig.getCorsHosts();
        var mapping = registry.addMapping("/**")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);

        if (corsHosts != null && corsHosts.length > 0) {
            if (corsHosts.length == 1 && ("*".equals(corsHosts[0]) || StringUtils.isBlank(corsHosts[0]))) {
                mapping.allowedOriginPatterns("*");
            } else {
                mapping.allowedOriginPatterns(corsHosts);
            }
        }
    }
}
