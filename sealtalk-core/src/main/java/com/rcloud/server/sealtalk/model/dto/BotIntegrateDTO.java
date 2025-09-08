package com.rcloud.server.sealtalk.model.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jianzheng.li
 */
@Data
@NoArgsConstructor
public class BotIntegrateDTO {

    private String integrateType;
    private String callbackUrl;
    private Boolean stream;
    private Boolean handleResponse;
    private List<String> objectNames;
    private String apiKey;

}
