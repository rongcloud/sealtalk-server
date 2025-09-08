package com.rcloud.server.sealtalk.entity;

import lombok.Data;

import java.util.Date;
@Data
public class VerificationViolations {
    /**
     * 获取验证码的ip
     */
    private String ip;

    /**
     * 获取验证码的时间
     */
    private Date time;

    /**
     * 获取验证码的次数
     */
    private Integer count;

}