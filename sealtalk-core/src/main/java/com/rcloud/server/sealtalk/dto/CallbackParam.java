package com.rcloud.server.sealtalk.dto;

import lombok.Data;

@Data
public class CallbackParam {
    private Integer result;
    private String content;
    private String serviceProvider;
    private String msgUID;
    private String resultDetail;
}
