package com.rcloud.server.sealtalk.model.dto.ai;

import lombok.Data;
import java.util.List;

@Data
public class TemplateUpdateReq {
    private String templateId;
    // 批量更新多语言内容
    private List<TemplateI18nItem> items;
}


