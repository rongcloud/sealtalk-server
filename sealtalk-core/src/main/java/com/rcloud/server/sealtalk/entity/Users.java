package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Users {

    //允许直接添加至群聊： 0 不允许 1 允许
    public static final Integer GROUP_VERIFY_NO_NEED = 1;
    public static final Integer GROUP_VERIFY_NEED = 0;

    //允许 通过手机号搜索到我： 0 不允许 1允许
    public static final Integer PHONE_VERIFY_NO_NEED = 1;
    public static final Integer PHONE_VERIFY_NEED = 0;
    //允许 SealTalk 号搜索到我： 0 不允许 1允许
    public static final Integer ST_SEARCH_VERIFY_NO_NEED = 1;
    public static final Integer ST_SEARCH_VERIFY_NEED = 0;

    //加好友需要验证环节
    public static final Integer FRI_VERIFY_NEED = 1;





    private Integer id;

    private String region;

    private String phone;

    private String nickname;

    private String portraitUri;

    private String rongCloudToken;

    private String gender;

    private String stAccount;

    private Integer phoneVerify;

    private Integer stSearchVerify;

    private Integer friVerify;

    private Integer groupVerify;

    private Integer pokeStatus;

    private Integer groupCount;

    private Date createdAt;

    private Date updatedAt;

    private Date deletedAt;

    private Integer blockStatus;

    private String lastIp;

    private Date blockStartTime;

    private Date blockEndTime;

    private Groups groups;

}