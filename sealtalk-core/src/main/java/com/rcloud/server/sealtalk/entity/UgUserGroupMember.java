package com.rcloud.server.sealtalk.entity;

import lombok.Data;

/**
 * @author jianzheng.li
 */
@Data
public class UgUserGroupMember {

    private Long id;

    /**
     * 用户组id
     */
    private Long userGroupId;

    /**
     * 用户id
     */
    private Integer memberId;
}
