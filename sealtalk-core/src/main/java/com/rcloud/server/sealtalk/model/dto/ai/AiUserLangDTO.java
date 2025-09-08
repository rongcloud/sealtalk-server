package com.rcloud.server.sealtalk.model.dto.ai;

import java.util.List;
import lombok.Data;

@Data
public class AiUserLangDTO {
    private String nickname;
    private String area;
    private String profession;
    private String introduction;
    private List<String> tags;
    private String openingRemark;
    private String language;
}


