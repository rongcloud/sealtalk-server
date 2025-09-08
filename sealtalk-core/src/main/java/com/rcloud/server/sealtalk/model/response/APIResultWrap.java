package com.rcloud.server.sealtalk.model.response;

import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.exception.ServiceException;
import org.apache.commons.lang3.StringUtils;

public class APIResultWrap {

    public static final String SUCCESS_CODE = "200";


    private APIResultWrap() {
    }


    public static <T> APIResult<T> ok(T data) {
        return new APIResult<>(SUCCESS_CODE, StringUtils.EMPTY, data);
    }

    public static <T> APIResult<T> ok() {
        return new APIResult<>(SUCCESS_CODE, StringUtils.EMPTY, null);
    }

    public static <T> APIResult<T> ok(T data, String message) {
        return new APIResult<>(SUCCESS_CODE, message, data);
    }

    public static APIResult error(int code, String msg) {
        return new APIResult(String.valueOf(code), msg);
    }

    public static APIResult error(String code, String msg) {
        return new APIResult(code, msg);
    }
}
