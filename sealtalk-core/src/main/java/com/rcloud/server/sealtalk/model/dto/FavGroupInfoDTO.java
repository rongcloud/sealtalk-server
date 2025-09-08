package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

@Data
public class FavGroupInfoDTO {

    private String id;
    private String name;
    private String portraitUri;
    private String creatorId;
    private Integer isMute;
    private Integer certiStatus;
    private Integer memberCount;
    private Integer memberProtection;
    private Integer maxMemberCount;
    private String createdAt;
    private String updatedAt;
    private Long updatedTime;
    private Long createdTime;


}
