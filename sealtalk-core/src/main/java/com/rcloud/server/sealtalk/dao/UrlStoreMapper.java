package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.UrlStore;

import java.util.List;

public interface UrlStoreMapper {
    int insert(UrlStore record);
    int deleteByPrimaryKey(Long id);
    List<UrlStore> selectAll();
}


