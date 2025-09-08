package com.rcloud.server.sealtalk.entity;

import lombok.Data;

/**
 * @author jianzheng.li
 */
@Data
public class FreezeData {

    private Long id;

    private String phone;

    private String region;

    private Long userId;

    private Long freezeTime;

    private String data;

}
