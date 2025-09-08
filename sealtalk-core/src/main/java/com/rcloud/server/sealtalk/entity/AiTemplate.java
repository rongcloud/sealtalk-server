package com.rcloud.server.sealtalk.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class AiTemplate{

    private Long id;

    private String templateId;

    private String templateName;

    private String systemPrompt;

    private String language;

    private Date createdTime;

    private Date updatedTime;
}


