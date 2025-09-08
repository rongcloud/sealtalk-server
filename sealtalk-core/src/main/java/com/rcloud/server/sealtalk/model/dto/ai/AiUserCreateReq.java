package com.rcloud.server.sealtalk.model.dto.ai;

import lombok.Data;
import java.util.List;

@Data
public class AiUserCreateReq {
    private List<AiUserMultilingualItemDTO> multilingual;
    private String avatar;
    private Integer gender;
    private Integer age;
    private Boolean open;
    private String systemPrompt;
    private String aiUserId;
}


