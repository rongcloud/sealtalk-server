package com.rcloud.server.sealtalk.constant;

public enum ErrorCode {


    NOT_LOGIN(1000, "Not loged in."),
    PARAM_ERROR(1001, "Parameter error,Please check."),
    ID_FORMAT_ERROR(1002, "ID format error."),
    TOO_MANY_REQUEST(1003,"Too many request"),
    OVER_LIMIT(1004, "Exceed the limit."),
    INVALID_SMS_CODE(1005, "Invalid sms code."),
    SERVICE_ERROR(1006, "Service error."),

    CALL_RC_SERVER_ERROR(2000, ""),

    UNKNOWN_ERROR(5000,"unknown error")
    ;

    private int errorCode;
    private String errorMessage;

    ErrorCode(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
