package com.rcloud.server.sealtalk.sms;

import lombok.Data;

@Data
public class SmsTemplateVO {
    private String region;
    private String device;
    private String templateCode;
    private String service;
    private String url;
    private String signName;
    private String keyId;
    private String keySecret;
}
