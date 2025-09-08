package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

@Data
public class BlackLists {

    public static final Integer STATUS_VALID = 1;
    public static final Integer STATUS_INVALID = 0;

    private Integer id;

    private Integer userId;

    private Integer friendId;

    //黑名单状态 1 有效，0无效
    private Integer status;

    private Date createdAt;

    private Date updatedAt;

    private Users users;
}