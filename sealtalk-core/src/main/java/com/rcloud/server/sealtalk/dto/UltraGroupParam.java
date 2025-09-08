package com.rcloud.server.sealtalk.dto;

import java.util.List;
import lombok.Data;


@Data
public class UltraGroupParam {

    private String portraitUri;
    private String groupName;
    private String summary;
    private String groupId;
    private String channelName;
    /**
     * 频道类型：0公有 1私有
     */
    private Integer type;
    /**
     * 频道id
     */
    private String channelId;

    private String[] memberIds;

    private Integer pageNum;
    private Integer limit;

    /**
     * 用户组名称
     */
    private String userGroupName;
    /**
     * 用户组id
     */
    private String userGroupId;

    private List<String> userGroupIds;


}
