package com.rcloud.server.sealtalk.constant;

import java.util.stream.Stream;

public enum WhiteListTypeEnum {

    /**
     * 短信白名单
     */
    SMS(1),
    /**
     * 登录时的风险认证
     */
    LOGIN_RISK_VERIFY(2),

    /**
     * ip黑名单
     */
    BLOCK_IP(3),

    /**
     * 短信地区黑名单
     */
    SMS_BLOCK_REGION(4),

    ;
    private final int type;

    WhiteListTypeEnum(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static WhiteListTypeEnum fromType(Integer source) {
        if (source == null){
            return null;
        }
        return Stream.of(WhiteListTypeEnum.values()).filter(x -> x.getType() == source).findFirst().orElse(null);
    }
}
