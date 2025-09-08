package com.rcloud.server.sealtalk.dto;


import lombok.Data;

@Data
public class RiskWhiteListParam {

    private Integer type;

    private String region;

    private String phone;

    private Integer delete;
}
