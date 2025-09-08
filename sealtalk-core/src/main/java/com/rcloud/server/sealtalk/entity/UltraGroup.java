package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;
@Data
public class UltraGroup {

    private Integer id;

    private String name;

    private String portraitUri;

    private Integer creatorId;

    private String summary;

    private Integer memberCount;

    private Date createdAt;

    private Date updatedAt;

}