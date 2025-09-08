package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.VerificationViolations;
import org.apache.ibatis.annotations.Param;

public interface VerificationViolationsMapper {
    VerificationViolations selectByPrimaryKey(@Param("ip") String ip);
    int insert(VerificationViolations record);
    int updateByPrimaryKey(VerificationViolations record);
}