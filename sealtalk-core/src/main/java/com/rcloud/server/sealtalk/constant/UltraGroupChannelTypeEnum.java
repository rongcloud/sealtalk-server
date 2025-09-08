package com.rcloud.server.sealtalk.constant;

import java.util.stream.Stream;

/**
 * 超级群 频道属性
 */
public enum UltraGroupChannelTypeEnum {

    /**
     * 公有
     */
    PUBLIC(0),
    /**
     * 私有
     */
    PRIVATE(1);

    private final int type;


    UltraGroupChannelTypeEnum(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static UltraGroupChannelTypeEnum channelType(Integer targetType) {
        if (targetType == null) {
            return null;
        }
        return Stream.of(UltraGroupChannelTypeEnum.values())
            .filter(typeEnum -> typeEnum.type == targetType)
            .findFirst()
            .orElse(null);
    }

}
