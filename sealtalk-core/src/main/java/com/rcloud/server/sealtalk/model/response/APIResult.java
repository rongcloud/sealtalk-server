package com.rcloud.server.sealtalk.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class APIResult<T> {

    protected Integer code;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    protected String message;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    protected T result;

    public APIResult(String code, String message) {
        this.code = Integer.valueOf(code);
        this.message = message;
    }

    public APIResult(String code, String message, T result) {
        this.code = Integer.valueOf(code);
        this.message = message;
        this.result = result;
    }
}
