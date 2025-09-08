package com.rcloud.server.sealtalk.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class UserStatusDTO {


    private String id;

    private int status;

    //服务内部使用,不对外
    @JsonIgnore
    private Integer userId;


    public UserStatusDTO() {
    }

    public UserStatusDTO(Integer userId, int status) {
        this.status = status;
        this.userId = userId;
    }
}
