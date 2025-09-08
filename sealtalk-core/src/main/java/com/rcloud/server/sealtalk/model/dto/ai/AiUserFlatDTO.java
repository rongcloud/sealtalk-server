package com.rcloud.server.sealtalk.model.dto.ai;

import lombok.Data;

import java.util.Map;


@Data
public class AiUserFlatDTO {


    private String aiUserId;
    private String nickname;
    private String avatar;
    private String systemPrompt;
    private String templateId;
    private Map<String, TemplateI18nItem> templateI18n;
    private Integer createType;
    private String language;
}
