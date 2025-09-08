package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

@Data
public class GroupMemberDTO {

    private Boolean isDeleted;
    private String groupNickname;
    private String region;
    private String phone;
    private String WeChat;
    private String Alipay;
    //memberDesc 返回给前端是json对象格式，不是String
    private Object memberDesc;

}
