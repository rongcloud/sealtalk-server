package com.rcloud.server.sealtalk.exception;

import org.springframework.http.HttpStatus;

public class ServiceException extends DemoException {


    private static final Integer HTTP_CODE = HttpStatus.OK.value();


    public ServiceException(int code, String message) {
        super(code, message, HTTP_CODE);
    }


    public static ServiceException buildError(int code, String template, Object... args) {
        String message = String.format(template, args);
        return new ServiceException(code, message);
    }

}
