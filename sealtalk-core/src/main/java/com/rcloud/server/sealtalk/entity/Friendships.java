package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Friendships{


    private Integer id;

    private Integer userId;

    private Integer friendId;

    private String displayName;

    private String message;

    private Integer status;

    private String region;

    private String phone;

    private String description;

    private String imageUri;

    private Date createdAt;

    private Date updatedAt;

    private Users users;
}