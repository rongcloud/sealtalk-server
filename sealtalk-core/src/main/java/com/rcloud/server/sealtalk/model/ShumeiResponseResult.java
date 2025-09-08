package com.rcloud.server.sealtalk.model;

import lombok.Data;

@Data
public class ShumeiResponseResult {

    private int code;
    private String message;
    private String requestId;
    private String riskLevel;
    private ShumeiResponseDetail detail;
}
