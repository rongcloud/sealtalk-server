package com.rcloud.server.sealtalk.entity;

import lombok.Data;

/**
 * @author jianzheng.li
 */
@Data
public class UgChannelUserGroup {


    private Long id;

    /**
     * 频道id
     */
    private String channelId;

    /**
     * 用户组id
     */
    private Long userGroupId;

}
