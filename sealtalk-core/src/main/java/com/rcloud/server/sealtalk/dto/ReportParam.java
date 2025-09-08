package com.rcloud.server.sealtalk.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jianzheng.li
 */
@Data
@NoArgsConstructor
public class ReportParam {


    private String targetId;

    private Integer channelType;

    private String levelF;

    private String levelS;

    private List<String> pics;

    private String content;

}
