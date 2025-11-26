package com.rcloud.server.sealtalk.rongcloud.message;

import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.util.JacksonUtil;
import lombok.Data;

@Data
public class RCUserProfile {


    private String uniqueId;
    private String name;
    private String portraitUri;
    private String email;
    private String birthday;
    private Integer gender;
    private String location;
    private Integer role;
    private Integer level;


    public String toJsonString() throws ServiceException {
        return JacksonUtil.toJson(this);
    }


}
