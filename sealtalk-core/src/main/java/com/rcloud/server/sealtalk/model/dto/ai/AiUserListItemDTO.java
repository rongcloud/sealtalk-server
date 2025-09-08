package com.rcloud.server.sealtalk.model.dto.ai;

import java.util.Map;
import lombok.Data;

@Data
public class AiUserListItemDTO {
    private String aiUserId;
    private String avatar;
    private Integer gender;
    private Integer age;
    private Boolean open;
    private Integer whiteSize;
    private Long updateTime;
    private String creatorId;
    private Integer createType;
    private Integer createSource;
    private String systemPrompt;
    private String templateId;
    private Map<String, AiUserLangDTO> multilingual;
}


