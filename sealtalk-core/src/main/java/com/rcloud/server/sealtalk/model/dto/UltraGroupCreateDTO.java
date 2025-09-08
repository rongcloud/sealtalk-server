package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

@Data
public class UltraGroupCreateDTO {

    private String groupId;
    private String defaultChannelId;
    private String defaultChannelName;

}
