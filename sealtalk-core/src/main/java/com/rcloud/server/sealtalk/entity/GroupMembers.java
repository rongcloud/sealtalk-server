package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

@Data
public class GroupMembers {

    private Integer id;

    private Integer groupId;

    private Integer memberId;

    private String displayName;

    private Integer role;

    private String groupNickname;

    private String region;

    private String phone;

    private String weChat;

    private String alipay;

    private String memberDesc;

    private Long timestamp;

    private Date createdAt;

    private Date updatedAt;

    private Groups groups;

    private Users users;

}