package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.BlackLists;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface BlackListsMapper {
    List<BlackLists> selectBlackListsWithFriendUsers(@Param("userId") Integer userId);

    BlackLists selectByUserIdAndFriendId(@Param("userId") Integer userId, @Param("friendId") Integer friendId);
    void deleteByUserIdAndFriendId(@Param("userId") Integer userId, @Param("friendId") Integer friendId);
    void deleteByUserIdOrFriendId(@Param("userId") Integer userId);

    int saveOrUpdate(BlackLists record);
}