package com.rcloud.server.sealtalk.exception;

import com.rcloud.server.sealtalk.constant.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 *
 */
public class RCloudHttpException extends DemoException{


    private static final Integer HTTP_CODE = HttpStatus.SERVICE_UNAVAILABLE.value();


    public RCloudHttpException(String message) {
        super(ErrorCode.CALL_RC_SERVER_ERROR.getErrorCode(), message, HTTP_CODE);
    }

}
