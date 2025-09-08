package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.AiUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiUserMapper {

    int insert(AiUser record);

    int deleteByUserId(@Param("aiUserId") String userId);

    AiUser selectByAiUserId(@Param("aiUserId") String aiUserId);
    List<AiUser> selectByAiUserIds(@Param("aiUserIds") List<String> userIds);

    List<AiUser> pageQuery(@Param("open") Boolean open,
                            @Param("createSource") Integer createSource,
                            @Param("offset") int offset,
                            @Param("limit") int limit);

    List<AiUser> queryAllByType(@Param("open") Boolean open,
                       @Param("createSource") Integer createSource,
                       @Param("createType") Integer createType,
                       @Param("limit") int limit);

    List<AiUser> queryByCreator(@Param("creatorId")Long userId);



    int count(@Param("open") Boolean open,
              @Param("createSource") Integer createSource);

    int countByCreatorId(@Param("creatorId") Long creatorId);

    int updateByAiUserId(AiUser record);
}


