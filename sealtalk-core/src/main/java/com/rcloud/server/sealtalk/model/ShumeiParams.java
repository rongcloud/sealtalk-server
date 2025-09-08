package com.rcloud.server.sealtalk.model;

import lombok.Data;

@Data
public class ShumeiParams<T> {

    private String accessKey;
    private String appId;
    private String eventId;
    private T data;

    public ShumeiParams() {
    }

    public ShumeiParams(String accessKey, String appId, String eventId, T data) {
        this.accessKey = accessKey;
        this.appId = appId;
        this.eventId = eventId;
        this.data = data;
    }

}
