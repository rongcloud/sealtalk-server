package com.rcloud.server.sealtalk.model.dto.ai;

import lombok.Data;
import java.util.List;

@Data
public class WhitelistReq {
    private String aiUserId;
    private List<String> userIds;
}


