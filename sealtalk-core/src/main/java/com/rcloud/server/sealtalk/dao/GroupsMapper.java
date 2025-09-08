package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.Groups;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GroupsMapper {
    int insertSelective(Groups record);

    int updateByPrimaryKeySelective(Groups record);

    int deleteByPrimaryKey(@Param("id") Integer id);

    Groups selectByPrimaryKey(@Param("id") Integer id);

    List<Groups> selectByIds(@Param("ids") List<Integer> ids);
}