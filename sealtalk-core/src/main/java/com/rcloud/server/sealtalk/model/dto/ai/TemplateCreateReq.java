package com.rcloud.server.sealtalk.model.dto.ai;

import lombok.Data;
import java.util.List;

@Data
public class TemplateCreateReq {
    // 批量按语言创建，同一个 templateId 下多语言
    private List<TemplateI18nItem> items;
}


