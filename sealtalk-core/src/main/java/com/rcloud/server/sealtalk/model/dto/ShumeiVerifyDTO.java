package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

@Data
public class ShumeiVerifyDTO {

    private String riskLevel;

    public ShumeiVerifyDTO(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}
