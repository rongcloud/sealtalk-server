package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

@Data
public class MemberDTO {

    private String groupNickname;
    private Integer role;
    private String createdAt;
    private Long createdTime;
    private String updatedAt;
    private Long updatedTime;
    private String displayName;

    private UserDTO user;
}
