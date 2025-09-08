package com.rcloud.server.sealtalk.model.dto.ai;

import java.util.List;
import lombok.Data;

@Data
public class AiUserPageDTO {
    private Integer total;
    private List<AiUserListItemDTO> list;
}


