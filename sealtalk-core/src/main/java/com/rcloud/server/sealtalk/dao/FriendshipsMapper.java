package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.Friendships;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FriendshipsMapper {

    List<Friendships> getFriendShipListWithUsers(@Param("userId") Integer userId);

    Friendships getFriendShipWithUsers(@Param("userId") Integer userId,@Param("friendId") Integer friendId,@Param("status") Integer status);

    Friendships selectByUserIdAndFriendId(@Param("userId") Integer userId, @Param("friendId") Integer friendId);

    List<Friendships> selectByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") List<Integer> status);

    int updateByUserIdAndFriendIdSelective(Friendships record);

    int saveOrUpdate(Friendships record);

    void deleteByUserIdOrFriendId(@Param("userId") Integer userId);

    List<Friendships> selectByUserIdAndFriendIds(@Param("userId") Integer userId, @Param("friendIds") List<Integer> friendIds);

    int updateStatusByUserIdAndFriendIds(@Param("userId") Integer userId, @Param("friendIds") List<Integer> friendIds, @Param("status") Integer status);


    List<Friendships> scanById(@Param("id") Integer id, @Param("limit") int limit);
}