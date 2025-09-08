package com.rcloud.server.sealtalk.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jianzheng.li
 */

@Data
public class CustomerReport {

    private Long id;

    private String reportId;

    private String targetId;

    private Integer channelType;

    private String reportLevelFirst;

    private String reportLevelSecond;


    private String pic;

    private String content;

}