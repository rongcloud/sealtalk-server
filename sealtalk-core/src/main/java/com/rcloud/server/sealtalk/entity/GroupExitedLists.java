package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

@Data
public class GroupExitedLists {

    public static final Integer QUITE_REASON_CREATOR = 0;   //群主踢出
    public static final Integer QUITE_REASON_MANAGER = 1;   //管理员踢出
    public static final Integer QUITE_REASON_SELF = 2;      //主动退出

    private Integer id;

    private Integer groupId;

    private Integer quitUserId;

    private String quitNickname;

    private String quitPortraitUri;

    private Integer quitReason;

    private Long quitTime;

    private Integer operatorId;

    private String operatorName;

    private Date createdAt;

    private Date updatedAt;

}