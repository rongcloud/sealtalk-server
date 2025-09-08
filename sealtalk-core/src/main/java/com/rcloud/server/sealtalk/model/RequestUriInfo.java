package com.rcloud.server.sealtalk.model;

import lombok.Data;

@Data
public class RequestUriInfo {

    private String method;
    private String uri;
    private String remoteAddress;
    private String ip;
    private Integer port;
    private String userAgent;
}
