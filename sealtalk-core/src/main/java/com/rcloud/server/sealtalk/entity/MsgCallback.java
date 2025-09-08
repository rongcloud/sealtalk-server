package com.rcloud.server.sealtalk.entity;

import lombok.Data;
import java.util.Date;

@Data
public class MsgCallback {
    private Integer id;

    private Integer userId;

    private String targetId;

    private String channelType;

    private String msgType;

    private String msgId;

    private Date msgTime;

    private String strategy;

    private String riskStatus;

    private Date createdAt;

    private byte[] msgContent;

}