package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

/**
 * 机器人用户绑定表
 */
@Data
public class BotUserBind {

    private Long id;

    private String botId;

    private Long userId;

    private Integer bindType;

    private Date createdTime;

    private Date updatedTime;
} 