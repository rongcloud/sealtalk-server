package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

@Data
public class MessageVerifyItemDTO {

    private String userId;
    private String channelType;
    private String msgType;
    private long msgTime;
    private String status;
    private String msgContent;
    private String msgId;
    private String strategy;
    private int blockStatus;
    private String targetId;
}
