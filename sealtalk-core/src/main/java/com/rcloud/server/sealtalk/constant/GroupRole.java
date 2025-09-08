package com.rcloud.server.sealtalk.constant;

/**
 * 群组角色
 */
public enum GroupRole {

    /**
     * 创建者
     */
    CREATOR(0),

    /**
     * 成员
     */
    MEMBER(1),

    /**
     * 管理员
     */
    MANAGER(2);


    private final Integer role;

    GroupRole(Integer role) {
        this.role = role;
    }

    public Integer getRole() {
        return role;
    }

}
