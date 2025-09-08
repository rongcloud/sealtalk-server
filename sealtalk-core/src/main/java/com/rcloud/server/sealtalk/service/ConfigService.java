package com.rcloud.server.sealtalk.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.WhiteListTypeEnum;
import com.rcloud.server.sealtalk.dao.ConfigListMapper;
import com.rcloud.server.sealtalk.entity.ConfigList;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ConfigService {

    @Autowired
    private ConfigListMapper configListMapper;

    @Value("classpath:region.json")
    private Resource regionResource;


    public static final Cache<String, String> CONFIG_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();


    /**
     * 获取区域信息
     */
    public Object getRegionList() throws ServiceException {
        String regionData = CONFIG_CACHE.get("region_list_data", k -> {
            try {
                return IOUtils.toString(regionResource.getInputStream(), StandardCharsets.UTF_8);
            } catch (IOException i) {
                log.error("", i);
            }
            return null;
        });
        return JacksonUtil.getJsonNode(regionData);
    }


    public String getConfig(String attKey) {
        return CONFIG_CACHE.get(attKey, k -> {
            var config = configListMapper.selectByKey(attKey);
            return config == null ? "" : config.getAttValue();
        });
    }


    public void saveOrUpdateConfig(String key, String value){
        var config = new ConfigList();
        config.setAttKey(key);
        config.setAttValue(value);
        configListMapper.insert(config);
        CONFIG_CACHE.invalidate(key);
    }


    public List<ConfigList> selectAll(){
        return configListMapper.selectAll();
    }

}
