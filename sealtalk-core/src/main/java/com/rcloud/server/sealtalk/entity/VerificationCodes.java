package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;

/**
 * 验证码
 */
@Data
public class VerificationCodes {
    private Integer id;

    private String region;

    private String phone;

    private String sessionId;

    private String token;

    private Date createdAt;

    private Date updatedAt;
}