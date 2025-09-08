package com.rcloud.server.sealtalk.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
public class GroupAddStatusDTO {

    private String id;
    private List<UserStatusDTO> userStatus;


    //服务内部使用不对外
    @JsonIgnore
    private Integer groupId;


    public GroupAddStatusDTO(Integer groupId, List<UserStatusDTO> userStatus) {
        this.userStatus = userStatus;
        this.groupId = groupId;
    }

    public GroupAddStatusDTO() {
    }
}
