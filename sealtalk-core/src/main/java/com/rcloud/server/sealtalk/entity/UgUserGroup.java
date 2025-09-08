package com.rcloud.server.sealtalk.entity;

import lombok.Data;

/**
 * @author jianzheng.li
 */
@Data
public class UgUserGroup {


    private Long id;

    /**
     * 用户组名称
     */
    private String name;

    /**
     * 群ID
     */
    private Integer groupId;

    /**
     * 创建者
     */
    private Integer creatorId;
}
