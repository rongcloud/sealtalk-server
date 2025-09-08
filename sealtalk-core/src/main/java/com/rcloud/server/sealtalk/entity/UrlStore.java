package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

@Data
public class UrlStore{

    private Long id;

    private String url;

    private Date createdTime;

    private Date updatedTime;
}


