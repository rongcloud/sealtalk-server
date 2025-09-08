package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

@Data
public class GroupFavs {
    private Integer id;

    private Integer userId;

    private Integer groupId;

    private Date createdAt;

    private Date updatedAt;

    private Groups groups;

}