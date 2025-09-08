package com.rcloud.server.sealtalk.dto;

import lombok.Data;

@Data
public class RegisterInfoDTO {

    private String region;
    private String phone;
    private String channel;
    private String version;
    private String os;
    private String ip;

}
