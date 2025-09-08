package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.UltraGroup;
import org.apache.ibatis.annotations.Param;

public interface UltraGroupMapper {
    int insert(UltraGroup record);

    int deleteByPrimaryKey(@Param("id") Integer id);

    UltraGroup selectByPrimaryKey(@Param("id") Integer id);
}