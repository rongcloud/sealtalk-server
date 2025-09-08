package com.rcloud.server.sealtalk.constant;

import lombok.Getter;

@Getter
public enum UltraGroupRole {

    /**
     * 创建者
     */
    CREATOR(0),
    /**
     * 普通成员
     */
    MEMBER(1);


    private final Integer role;

    UltraGroupRole(Integer role) {
        this.role = role;
    }

}
