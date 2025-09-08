package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.Users;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UsersMapper {
    Users selectByPrimaryKey(@Param("id")Integer id);

    int deleteById(@Param("id")Integer id);
    List<Users> selectByIds(@Param("ids") List<Integer> ids);
    Users selectByRegionAndPhone(@Param("region") String region, @Param("phone") String phone);
    Users selectByStAccount(@Param("stAccount") String stAccount);
    int insertSelective(Users record);
    int updateByPrimaryKeySelective(Users record);
    List<Users> selectByPhones(@Param("phones") List<String> phones);
}