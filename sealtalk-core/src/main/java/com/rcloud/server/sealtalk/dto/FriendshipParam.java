package com.rcloud.server.sealtalk.dto;

import lombok.Data;

@Data
public class FriendshipParam {

    private String friendId;

    private String message;

    private String displayName;

    private String[] contactList;

    private String[] friendIds;

    private String region;
    private String phone;
    private String description;
    private String imageUri;


}
