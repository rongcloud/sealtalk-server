package com.rcloud.server.sealtalk.model.dto.admin;

import lombok.Data;
import java.util.List;

@Data
public class AgentDTO {


    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 职业/身份。
     */
    private String profession;

    /**
     * 地区。
     */
    private String area;

    /**
     * 介绍。
     */
    private String introduction;

    /**
     * 标签集合。
     */
    private List<String> tags;

    /**
     * 头像地址。
     */
    private String avatar;

    /**
     * 性别。0/1/2 等枚举值。
     */
    private Integer gender;

    /**
     * 年龄。
     */
    private Integer age;

    /**
     * 是否公开（true 为公开，false 为私有）。
     */
    private Boolean open;

    /**
     * 系统提示词（System Prompt）。
     */
    private String systemPrompt;

    /**
     * 开场白。
     */
    private String openingRemark;

    /**
     * 支持的语言种类。
     */
    private String language;

    /**
     * 白名单（如用户 ID、账号或其他标识）。
     */
    private List<String> whitelist;


}
