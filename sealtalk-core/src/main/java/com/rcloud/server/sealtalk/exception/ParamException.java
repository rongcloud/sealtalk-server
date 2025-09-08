package com.rcloud.server.sealtalk.exception;


import org.springframework.http.HttpStatus;

public class ParamException extends DemoException {


    private static final Integer HTTP_CODE = HttpStatus.BAD_REQUEST.value();

    public ParamException(int code, String message) {
        super(code, message, HTTP_CODE);
    }


    public static ParamException buildError(int code, String template, Object... args) {
        String message = String.format(template, args);
        return new ParamException(code, message);
    }
}
