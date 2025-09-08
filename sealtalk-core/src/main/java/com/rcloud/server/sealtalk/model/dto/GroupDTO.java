package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class GroupDTO {
    private String id;
    private String name;
    private String portraitUri;
    private Integer memberCount;
    private Integer maxMemberCount;
    private String creatorId;
    private String bulletin;
    private Integer isMute;
    private Integer certiStatus;
    private Integer memberProtection;

}
