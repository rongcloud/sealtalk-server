package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

@Data
public class UltraGroupMemberDTO {

    private String memberName;
    private Integer role;
    private Long createdTime;
    private Long updatedTime;
    private String createdAt;
    private String updatedAt;
    private String groupNickname;


    private UserDTO user;
}
