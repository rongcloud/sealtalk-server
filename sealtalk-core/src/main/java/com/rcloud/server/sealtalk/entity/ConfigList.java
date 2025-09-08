package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ConfigList {
    private Integer id;

    private String attKey;

    private String attValue;

    private Date updateAt;

    private Date createdAt;

}