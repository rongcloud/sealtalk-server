package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.WhiteList;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author jianzheng.li
 */
public interface WhiteListMapper {

    int exist(@Param("region") String region, @Param("phone") String phone, @Param("type") Integer type);
    int insert(@Param("region") String region, @Param("phone") String phone, @Param("type") Integer type);
    int delete(@Param("region") String region, @Param("phone") String phone, @Param("type") Integer type);

    List<WhiteList> queryByType(@Param("type") Integer type);
}
