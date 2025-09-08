package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RCAgent {


    private String agentId;
    private String name;
    private String description;
    private String type;
    private String status;
    private Config agentConfig;


    @Data
    public static class Config {

        private Model model;
        private Prompt prompt;
        private Memory memory;
    }

    @Data
    public static class Model {
        private String provider;
        private String name;
        private ModelOption options;

    }

    @Data
    public static class ModelOption {

        private String temperature;
        private String topP;
        private String maxTokens;
        private String frequencyPenalty;
        private String presencePenalty;
        private String stop;

    }

    @Data
    public static class Prompt {
        private Map<String,String> variables;
        private String id;
        private String instructions;
        private String openingStatement;
    }

    @Data
    public static class Memory {
        private String strategy;
        private String maxMessages;
        private String maxTokens;
//        private String model;
    }




}
