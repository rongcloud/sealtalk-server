package com.rcloud.server.sealtalk.model.dto.ai;

import java.util.Map;
import lombok.Data;

@Data
public class AiUserDetailDTO {
    private String aiUserId;
    private String avatar;
    private Integer gender;
    private Integer age;
    private Boolean open;
    private String systemPrompt;
    private Map<String, AiUserLangDTO> multilingual;
    private Integer createSource;
    private Integer createType;
}


