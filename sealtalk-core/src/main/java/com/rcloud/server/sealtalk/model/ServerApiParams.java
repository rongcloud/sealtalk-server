package com.rcloud.server.sealtalk.model;

import lombok.Data;

@Data
public class ServerApiParams {

    private String traceId;

    private Integer currentUserId;

    private String currentUserIdStr;

    private Long tokenId;

    private RequestUriInfo requestUriInfo;
}
