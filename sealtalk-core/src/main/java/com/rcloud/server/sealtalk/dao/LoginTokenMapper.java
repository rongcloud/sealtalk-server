package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.LoginToken;
import org.apache.ibatis.annotations.Param;

public interface LoginTokenMapper {
    int insert(LoginToken record);

    LoginToken selectByPrimaryKey(@Param("id") Long id);

    int deleteByPrimaryKey(@Param("id") Long id);
}