package com.rcloud.server.sealtalk.entity;

import lombok.Data;
import java.util.Date;

/**
 * @author jianzheng.li
 */
@Data
public class LoginToken {

    private Long id;

    private Integer userId;

    private Long loginTime;


    private Date createTime;

    private Date updateTime;

}
