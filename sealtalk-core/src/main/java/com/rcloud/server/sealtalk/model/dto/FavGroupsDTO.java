package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class FavGroupsDTO {

    private Integer limit;
    private Integer offset;
    private Integer total;
    private List<FavGroupInfoDTO> list;
}
