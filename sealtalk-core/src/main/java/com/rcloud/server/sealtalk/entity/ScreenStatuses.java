package com.rcloud.server.sealtalk.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ScreenStatuses {
    private Integer id;

    private String operateId;

    private Integer conversationType;

    private Integer status;

    private Date createdAt;

    private Date updatedAt;

}