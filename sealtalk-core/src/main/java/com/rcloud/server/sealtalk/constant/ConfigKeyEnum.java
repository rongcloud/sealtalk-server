package com.rcloud.server.sealtalk.constant;

import lombok.Getter;

import java.util.stream.Stream;

@Getter
public enum ConfigKeyEnum {


    /**
     * 数美开关
     */
    SHU_MEI_STATUS("shumeiOpenEnable"),
    /**
     * 客户端版本
     */
    MOBILE_VERSION("mobileVersion"),

    /**
     * demo 广场
     */
    DEMO_SQUARE("demoSquare"),

    ;

    private final String key;

    ConfigKeyEnum(String key) {
        this.key = key;
    }

    public static ConfigKeyEnum fromKey(String k) {
        return Stream.of(ConfigKeyEnum.values()).filter(c -> c.getKey().equals(k)).findFirst().orElse(null);
    }


}
