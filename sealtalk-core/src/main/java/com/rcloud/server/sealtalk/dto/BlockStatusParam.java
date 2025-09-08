package com.rcloud.server.sealtalk.dto;

import lombok.Data;

@Data
public class BlockStatusParam {

    private String userId;
    // true 封禁，false解封
    private boolean status;
    private Integer minute;
}
