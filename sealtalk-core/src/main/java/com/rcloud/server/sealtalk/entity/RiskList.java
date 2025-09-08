package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

@Data
public class RiskList {
    private Integer id;

    private Integer userId;

    private String riskStatus;

    private String ip;

    private String deviceId;

    private String detail;

    private String otherDetail;

    private Date createdAt;

}