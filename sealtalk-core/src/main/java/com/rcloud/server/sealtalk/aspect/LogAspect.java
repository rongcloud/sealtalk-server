package com.rcloud.server.sealtalk.aspect;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rcloud.server.sealtalk.interceptor.ServerApiParamHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Configuration
@Slf4j
@Order(1)
public class LogAspect {

    private final static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }


    // 定义切点Pointcut
    @Pointcut("execution(* com.rcloud.server.sealtalk.controller..*Controller.*(..))")
    public void executeService() {
    }

    @Before("executeService()")
    public void doBefore(JoinPoint joinPoint) {

        try {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            UnLogAspect unAspect = methodSignature.getMethod().getAnnotation(UnLogAspect.class);
            if (unAspect != null) {
                return;
            }
            String target = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();
            String[] paramsName = methodSignature.getParameterNames();

            Map<String, Object> paramMap = new HashMap<>();
            if (args != null && paramsName != null && args.length > 0 && paramsName.length > 0) {
                for (int i = 0; i < paramsName.length; i++) {
                    String paramName = paramsName[i];
                    Object paramVal = args[i];
                    if (!(paramVal instanceof HttpServletResponse) && !(paramVal instanceof HttpServletRequest)) {
                        paramMap.put(paramName, args[i]);
                    }
                }
            }

            String uri = ServerApiParamHolder.getURI();
            String uid = ServerApiParamHolder.getEncodedCurrentUserId();
            log.info("[{}] [{}] [{}] [{}]", uri, target, objectMapper.writeValueAsString(paramMap), uid);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }


}
