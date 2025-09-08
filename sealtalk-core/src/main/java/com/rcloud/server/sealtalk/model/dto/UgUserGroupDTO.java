package com.rcloud.server.sealtalk.model.dto;

/**
 * @author jianzheng.li
 */
public class UgUserGroupDTO {

    /**
     * 用户组id
     */
    private String userGroupId;

    /**
     * 用户组名称
     */
    private String userGroupName;

    /**
     * 用户组下用户数
     */
    private Long memberCount;

    public String getUserGroupId() {
        return userGroupId;
    }

    public void setUserGroupId(String userGroupId) {
        this.userGroupId = userGroupId;
    }

    public String getUserGroupName() {
        return userGroupName;
    }

    public void setUserGroupName(String userGroupName) {
        this.userGroupName = userGroupName;
    }

    public Long getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Long memberCount) {
        this.memberCount = memberCount;
    }
}
