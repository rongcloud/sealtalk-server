package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.VerificationCodes;
import org.apache.ibatis.annotations.Param;

public interface VerificationCodesMapper {
    VerificationCodes selectByRegionAndPhone(@Param("region") String region, @Param("phone") String phone);
    int saveOrUpdate(VerificationCodes record);
}