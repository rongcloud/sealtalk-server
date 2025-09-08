package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

/**
 * 群公告
 */
@Data
public class GroupBulletins {
    private Integer id;

    private Integer groupId;

    private Date createdAt;

    private Date updatedAt;
    //公告内容
    private String content;

}