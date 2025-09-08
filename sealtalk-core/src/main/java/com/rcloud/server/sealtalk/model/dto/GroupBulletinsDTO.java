package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class GroupBulletinsDTO {

    private String id;
    private String groupId;

//    private Integer id;
//    private Integer groupId;

    private Long timestamp;
    //公告内容
    private String content;
}
