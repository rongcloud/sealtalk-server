package com.rcloud.server.sealtalk.entity;

import java.util.Date;

import lombok.Data;

/**
 * 机器人信息实体类
 * @author jianzheng.li
 */
@Data
public class BotInfo {



    private Long id;

    private String botId;

    private String name;

    private String portraitUri;

    private String openingMessage;

    private Integer botType;

    private Integer botStatus;

    private Date createdTime;

    private Date updatedTime;

} 