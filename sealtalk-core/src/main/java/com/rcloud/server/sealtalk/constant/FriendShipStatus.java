package com.rcloud.server.sealtalk.constant;

import lombok.Getter;

@Getter
public enum FriendShipStatus {

    /**
     * 好友请求发送中
     */
    REQUESTINT(10),

    /**
     * 被加好友请求
     */
    REQUESTED(11),

    /**
     * 已同意
     */
    AGREED(20),

    /**
     * 已忽略
     */
    IGNORED(21),

    /**
     * 已删除
     */
    DELETED(30),

    /**
     * 已拉黑
     */
    BLACK(31);


    private final Integer status;

    FriendShipStatus(Integer status) {
        this.status = status;
    }
}
