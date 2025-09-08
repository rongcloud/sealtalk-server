package com.rcloud.server.sealtalk.model.dto.ai;

import lombok.Data;

@Data
public class AiUserSimpleCreateReq {
    private String avatar;
    private String nickname;
    private String systemPrompt;
    private String language;
    private String aiUserId;
}


