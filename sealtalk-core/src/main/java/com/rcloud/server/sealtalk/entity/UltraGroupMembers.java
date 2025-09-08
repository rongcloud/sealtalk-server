package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

@Data
public class UltraGroupMembers {


    private Integer id;

    private Integer ultraGroupId;

    private String groupNickname;

    private Integer memberId;

    private Integer role;

    private Date createdAt;

    private Date updatedAt;

    private UltraGroup groups;

    private Users users;
}