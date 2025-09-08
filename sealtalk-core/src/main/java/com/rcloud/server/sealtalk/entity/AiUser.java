package com.rcloud.server.sealtalk.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class AiUser {

    private Long id;

    private String aiUserId;

    private String templateId;

    private String avatar;

    private Integer gender;

    private Integer age;
    private Boolean open;

    private String systemPrompt;

    private Integer createSource;

    private Integer createType;

    private Long creatorId;

    private Date createdTime;

    private Date updatedTime;

    /**
     * 获取一个带默认值的 AI 用户对象。
     * 字符串为 ""，数值为 0，布尔为 false，时间为当前时间；自增主键 id 设为 null。
     */
    public static AiUser defaultInstance() {
        AiUser u = new AiUser();
        u.templateId = "";
        u.avatar = "";
        u.gender = 0;
        u.age = 0;
        u.open = true;
        u.systemPrompt = "";
        u.createSource = 0;
        u.createType = 0;
        u.creatorId = 0L;
        return u;
    }

    /**
     * 将当前对象中为 null 的属性设置为默认值。
     * 字符串: ""；整数: 0；布尔: true；creatorId: 0L。
     * 不处理 id/aiUserId/时间字段。
     */
    public void fillDefaultsIfNull() {
        if (templateId == null) templateId = "";
        if (avatar == null) avatar = "";
        if (gender == null) gender = 0;
        if (age == null) age = 0;
        if (open == null) open = true;
        if (systemPrompt == null) systemPrompt = "";
        if (createSource == null) createSource = 0;
        if (createType == null) createType = 0;
        if (creatorId == null) creatorId = 0L;
    }

}
