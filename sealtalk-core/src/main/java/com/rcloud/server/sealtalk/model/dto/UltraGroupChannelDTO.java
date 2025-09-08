package com.rcloud.server.sealtalk.model.dto;

import java.util.Date;
import lombok.Data;

/**
 * @author Jianlu.Yu
 */
@Data
public class UltraGroupChannelDTO {

    private String channelId;
    private String channelName;
    private Integer type;
    private Date createdAt;

}
