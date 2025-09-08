package com.rcloud.server.sealtalk.constant;

public enum AiUserCreateSourceEnum {

    /**
     * admin后台创建
     */
    ADMIN(0),

    /**
     * 用户创建
     */
    USER(1);

    private final int type;

    AiUserCreateSourceEnum(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

}
