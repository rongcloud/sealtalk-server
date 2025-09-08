package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Groups {

    //关闭群组认证状态标示
    public static final Integer CERTI_STATUS_CLOSED = 1;
    //开启群组认证状态标示
    public static final Integer CERTI_STATUS_OPENED = 0;

    //全员禁言状态 否
    public static final Integer MUTE_STATUS_CLOSE = 0;

    //全员禁言状态 是
    public static final Integer MUTE_STATUS_OPENED = 1;


    //copiedTime 默认值
    public static final Long COPIED_TIME_DEFAUT = 0L;

    //clearStatus 0 关闭 、3 清理 3 天前、 7 清理 7 天前、 36 清理 36 小时前
    public static final Integer CLEAR_STATUS_CLOSED = 0;
    public static final Integer CLEAR_STATUS_D_3 = 3;
    public static final Integer CLEAR_STATUS_D_7 = 7;
    public static final Integer CLEAR_STATUS_H_36 = 36;


    private Integer id;

    private String name;

    private String portraitUri;

    private Integer memberCount;

    private Integer maxMemberCount;

    private Integer creatorId;

    /**
     * 开启群认证 0-开启 1-关闭
     */
    private Integer certiStatus;

    private Integer isMute;

    /**
     * 开启/更新 清理群离线消息
     * 清理选项： 0 关闭、 3 清理 3 天前、 7 清理 7 天前、 36 清理 36 小时前
     */
    private Integer clearStatus;

    private Long clearTimeAt;

    /**
     * 开启群保护 0-关闭 1-开启
     */
    private Integer memberProtection;

    private Long copiedTime;


    private Date createdAt;

    private Date updatedAt;

    private String bulletin;

}