package com.rcloud.server.sealtalk.model;

import lombok.Data;

/**
 */
@Data
public class ShumeiRequestParams {

    private String tokenId;
    private String ip;
    private long timestamp;
    private String deviceId;

    public ShumeiRequestParams() {
    }

    public ShumeiRequestParams(String tokenId, String ip, long timestamp, String deviceId) {
        this.tokenId = tokenId;
        this.ip = ip;
        this.timestamp = timestamp;
        this.deviceId = deviceId;
    }
}
