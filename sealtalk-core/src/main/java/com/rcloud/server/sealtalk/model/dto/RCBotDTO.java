package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RCBotDTO {

    private String userId;
    private String name;
    private String type;
    private String profileUrl;
    private List<Integration> integrations;
    @Data
    @NoArgsConstructor
    public static class Integration {
        private Boolean enabled;
        private String type;
        private String callbackUrl;
        private List<String> objectNames;
        private List<String> events;
        private Boolean stream;
        private Boolean handleResponse;
        private Auth auth;
        private Agent agent;
        private Boolean waitForInputCompletion;
        private Boolean gentleResponse;
    }

    @Data
    @NoArgsConstructor
    public static class Auth {
        private String apiKey;
    }

    @Data
    @NoArgsConstructor
    public static class Agent {
        private String agentId;
    }


}
