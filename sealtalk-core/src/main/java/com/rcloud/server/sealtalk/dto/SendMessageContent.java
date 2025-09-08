package com.rcloud.server.sealtalk.dto;

import lombok.Data;

@Data
public class SendMessageContent {

    private String title;
    private String content;
    private String imageUri;
    private String url;
    private String extra;
}
