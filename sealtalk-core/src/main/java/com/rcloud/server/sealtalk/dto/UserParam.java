package com.rcloud.server.sealtalk.dto;

import lombok.Data;

@Data
public class UserParam {
    private String region;
    private String phone;
    private String code;

    private String nickname;
    private String password;
    private String verification_token;

    private String oldPassword;
    private String newPassword;

    private String portraitUri;

    private String stAccount;

    private String friendId;
    private String version;
    private String gender;

    private Integer phoneVerify;
    private Integer stSearchVerify;
    private Integer friVerify;
    private Integer groupVerify;

    private Integer pokeStatus;

    private String channel;
    private String os;

    /**
     * 图片验证码id
     */
    private String picCodeId;
    /**
     * 图片验证码
     */
    private String picCode;

    private String device;

}

