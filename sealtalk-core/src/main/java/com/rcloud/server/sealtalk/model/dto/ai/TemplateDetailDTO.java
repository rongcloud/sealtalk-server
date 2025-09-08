package com.rcloud.server.sealtalk.model.dto.ai;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class TemplateDetailDTO {


    private String templateId;

    private Map<String, TemplateI18nItem> i18n;

    public TemplateDetailDTO() {
        this.i18n = new HashMap<>();
    }
}
