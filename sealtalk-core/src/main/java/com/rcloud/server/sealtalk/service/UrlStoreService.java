package com.rcloud.server.sealtalk.service;

import com.rcloud.server.sealtalk.dao.UrlStoreMapper;
import com.rcloud.server.sealtalk.entity.UrlStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UrlStoreService {

    @Autowired
    private UrlStoreMapper mapper;

    public List<UrlStore> getAll() {
        return mapper.selectAll();
    }


    public void saveUrl(String url) {
        UrlStore urlStore = new UrlStore();
        urlStore.setUrl(url);
        mapper.insert(urlStore);
    }

    public void delUrlById(Long id) {

        mapper.deleteByPrimaryKey(id);
    }

}


