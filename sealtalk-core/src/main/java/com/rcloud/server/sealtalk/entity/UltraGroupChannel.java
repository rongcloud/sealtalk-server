package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

@Data
public class UltraGroupChannel {


    private Integer id;

    private Integer ultraGroupId;

    private String channelId;

    private String channelName;

    private Integer type;

    private Integer creatorId;

    private Date createdAt;

}