package com.rcloud.server.sealtalk.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class AiUserI18n {

    private Long id;

    private String aiUserId;

    private String language;

    private String nickname;

    private String area;

    private String profession;

    private String introduction;

    private String tags;

    private String openingRemark;

    private Date createdTime;

    private Date updatedTime;


    public void fillDefaultsIfNull() {
        if (nickname == null) nickname = "";
        if (area == null) area = "";
        if (profession == null) profession = "";
        if (introduction == null) introduction = "";
        if (tags == null) tags = "";
        if (openingRemark == null) openingRemark = "";
    }
}


