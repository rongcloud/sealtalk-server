package com.rcloud.server.sealtalk.constant;

public enum AiUserCreateTypeEnum {


    /**
     * 完全自定义创建
     */
    ALL_CREATE(0),

    /**
     * 模板创建
     */
    TEMPLATE_CREATE(1),

    ;

    private final int type;

    AiUserCreateTypeEnum(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
