package com.rcloud.server.sealtalk.configuration;

import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.exception.ParamException;
import com.rcloud.server.sealtalk.exception.RCloudHttpException;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import com.rcloud.server.sealtalk.util.JacksonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestController
@RestControllerAdvice
@Slf4j
public class GlobalControllerExceptionHandler {

    private static final String CHARSET = "UTF-8";


    /**
     * 参数必传异常
     */
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public APIResult missingServletRequestParameterExceptionHandler(HttpServletRequest request, HttpServletResponse response, MissingServletRequestParameterException e) {
        String url = request.getRequestURI();
        String parameter = e.getParameterName();
        String errorMsg = String.format("The parameter %s is required.", parameter);
        log.info("[{}] [{}]", url, errorMsg);
        return APIResultWrap.error(ErrorCode.PARAM_ERROR.getErrorCode(), errorMsg);
    }

    @ExceptionHandler(value = ServiceException.class)
    public void serviceExceptionHandler(HttpServletRequest request, HttpServletResponse response, ServiceException e) throws Exception {
        String url = request.getRequestURI();
        String contentType = "application/json;charset=" + CHARSET;
        response.addHeader("Content-Type", contentType);
        log.info("[{}] [{}]", url, e.getMessage());
        response.setStatus(e.getHttpStatusCode());
        response.getWriter().write(JacksonUtil.toJson(APIResultWrap.error(e.getCode(), e.getMessage())));
    }

    @ExceptionHandler(value = ParamException.class)
    public void paramException(HttpServletRequest request, HttpServletResponse response, ParamException e) throws Exception {
        String url = request.getRequestURI();
        String contentType = "application/json;charset=" + CHARSET;
        response.addHeader("Content-Type", contentType);
        log.info("[{}] [{}]", url, e.getMessage());
        response.setStatus(e.getHttpStatusCode());
        response.getWriter().write(JacksonUtil.toJson(APIResultWrap.error(e.getCode(), e.getMessage())));
    }

    @ExceptionHandler(value = RCloudHttpException.class)
    public void rcloudException(HttpServletRequest request, HttpServletResponse response, RCloudHttpException e) throws Exception {
        String url = request.getRequestURI();
        String contentType = "application/json;charset=" + CHARSET;
        response.addHeader("Content-Type", contentType);
        log.info("[{}] [{}]", url, e.getMessage());
        response.setStatus(e.getHttpStatusCode());
        response.getWriter().write(JacksonUtil.toJson(APIResultWrap.error(e.getCode(), e.getMessage())));
    }

    /**
     * ValidLocation 验证参数异常
     */
    @ExceptionHandler(value = BindException.class)
    public APIResult bindExceptionHandler(HttpServletRequest request, BindException e) {
        log.error("Error found:", e);
        BindingResult bindingResult = e.getBindingResult();
        List<ObjectError> objectErrors = bindingResult.getAllErrors();
        ObjectError objectError = objectErrors.get(0);
        String errorMsg = objectError.getDefaultMessage();
        return APIResultWrap.error(ErrorCode.PARAM_ERROR.getErrorCode(), errorMsg);
    }

    /**
     * 参数类型不匹配异常
     */
    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public APIResult methodArgumentTypeExceptionHandler(HttpServletRequest request, MethodArgumentTypeMismatchException e) {
        log.error("Error found:", e);
        String parameter = e.getName();
        String errorMsg = String.format("Argument %s type mismatch!", parameter);
        return APIResultWrap.error(ErrorCode.PARAM_ERROR.getErrorCode(), errorMsg);
    }


}
