package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

import java.sql.Timestamp;
import java.util.Date;

@Data
public class FriendShipDTO {

    private String displayName;
    private String message;
    private Integer status;
    private String updatedAt;
    private Long updatedTime;

    private UserDTO user;

}
