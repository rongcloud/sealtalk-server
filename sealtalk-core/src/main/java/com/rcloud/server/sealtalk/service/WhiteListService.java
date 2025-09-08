package com.rcloud.server.sealtalk.service;

import com.rcloud.server.sealtalk.constant.WhiteListTypeEnum;
import com.rcloud.server.sealtalk.dao.WhiteListMapper;
import com.rcloud.server.sealtalk.entity.WhiteList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class WhiteListService {


    @Autowired
    private WhiteListMapper whiteListMapper;


    public void saveWhite(String region, String phone, WhiteListTypeEnum whiteType) {
        whiteListMapper.insert(region, phone, whiteType.getType());
    }

    public void deleteWhite(String region, String phone, WhiteListTypeEnum whiteType) {
        whiteListMapper.delete(region, phone, whiteType.getType());
    }

    public boolean whiteCheck(String region, String phone, WhiteListTypeEnum whiteType) {
        return whiteListMapper.exist(region, phone, whiteType.getType()) > 0;
    }

    public List<WhiteList> queryByType(WhiteListTypeEnum type){
        return whiteListMapper.queryByType(type.getType());
    }

}
