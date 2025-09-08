package com.rcloud.server.sealtalk.exception;

import lombok.Getter;

@Getter
public class DemoException extends Exception {

    private final int code;
    private final String message;
    private final int httpStatusCode;


    public DemoException(int code, String message, int httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}
