package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.RiskList;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RiskListMapper {
    int insertSelective(RiskList record);

    int updateByUserIdAndDeviceIdSelective(RiskList record);

    RiskList selectByUserIdAndDeviceId(@Param("userId") Integer userId, @Param("deviceId") String deviceId);
}