package com.rcloud.server.sealtalk.dto;

import lombok.Data;

@Data
public class SendMessageParam {

    private String conversationType;
    private String targetId;
    private String objectName;
    private SendMessageContent content;
    private String pushContent;
}
