package com.rcloud.server.sealtalk.entity;

import lombok.Data;
import java.util.Date;

/**
 * 入群前待审核表
 */
@Data
public class GroupReceivers {

    public static final Integer GROUP_RECEIVE_STATUS_IGNORE = 0;    //忽略
    public static final Integer GROUP_RECEIVE_STATUS_AGREED = 1;    //同意
    public static final Integer GROUP_RECEIVE_STATUS_WAIT = 2;      //等待
    public static final Integer GROUP_RECEIVE_STATUS_EXPIRED = 3;   //过期

    public static final Integer GROUP_RECEIVE_TYPE_MEMBER = 1;      //群普通成员
    public static final Integer GROUP_RECEIVE_TYPE_MANAGER = 2;     //群管理者


    private Integer id;

    private Integer userId;

    private Integer groupId;

    private String groupName;

    private String groupPortraitUri;

    private Integer requesterId;

    private Integer receiverId;

    private Integer type;

    private Integer status;

    private Date createdAt;

    private Date updatedAt;

    private Users requester;
    private Users receiver;
    private Groups group;
}