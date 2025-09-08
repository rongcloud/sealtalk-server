package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.ConfigList;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ConfigListMapper {
    int insert(ConfigList record);

    ConfigList selectByKey(@Param("attKey") String attKey);

    int deleteByKey(@Param("attKey") String attKey);

    List<ConfigList> selectAll();

}