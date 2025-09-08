package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

@Data
public class InviteDTO {

    private String action;

    private String message;

    public InviteDTO(String action, String message) {
        this.action = action;
        this.message = message;
    }
}
