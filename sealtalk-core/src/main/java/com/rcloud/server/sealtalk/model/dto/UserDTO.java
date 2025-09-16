package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

@Data
public class UserDTO {

    private String id;
    private String nickname;
    private String region;
    private String phone;
    private String portraitUri;
    private String gender;
    private String stAccount;
    private Integer block;



}
