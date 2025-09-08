package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

@Data
public class GroupExitedListDTO {


    private String quitUserId;

    private String quitNickname;

    private String quitPortraitUri;

    private Integer quitReason;

    private Long quitTime;

    private String operatorId;

    private String operatorName;
}
