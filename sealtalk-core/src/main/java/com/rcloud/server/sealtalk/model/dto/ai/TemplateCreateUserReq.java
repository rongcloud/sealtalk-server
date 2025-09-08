package com.rcloud.server.sealtalk.model.dto.ai;

import lombok.Data;

@Data
public class TemplateCreateUserReq {
    private String templateId;
    private String avatar;
    private String nickname;
    private String systemPrompt;
    private String language;
    private String aiUserId;
}


