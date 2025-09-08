package com.rcloud.server.sealtalk.model.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiSuggestionDTO {


    private String aiUserId;


    List<Msg> msgs;


    @Data
    public static class Msg{

        private String senderId;

        private Long time;

        private String content;

    }


}
