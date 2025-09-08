package com.rcloud.server.sealtalk.controller;

import com.rcloud.server.sealtalk.constant.ConfigKeyEnum;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.ErrorMsg;
import com.rcloud.server.sealtalk.constant.WhiteListTypeEnum;
import com.rcloud.server.sealtalk.dto.BlockStatusParam;
import com.rcloud.server.sealtalk.dto.ConfigDTO;
import com.rcloud.server.sealtalk.dto.RiskWhiteListParam;
import com.rcloud.server.sealtalk.entity.WhiteList;
import com.rcloud.server.sealtalk.exception.ParamException;
import com.rcloud.server.sealtalk.interceptor.IpInterceptor;
import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import com.rcloud.server.sealtalk.service.ConfigService;
import com.rcloud.server.sealtalk.service.UsersService;
import com.rcloud.server.sealtalk.service.WhiteListService;
import com.rcloud.server.sealtalk.util.N3d;
import com.rcloud.server.sealtalk.util.ValidateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/config")
@Slf4j
public class AdminConfigController {



    @Autowired
    private ConfigService configService;
    @Autowired
    private WhiteListService whiteListService;

    @Autowired
    private UsersService usersService;


    @GetMapping("/id")
    public APIResult<Object> idStr(@RequestParam(value = "id") String id) throws Exception {

        if (id.matches("\\d+")){
            return APIResultWrap.ok(N3d.encode(Integer.parseInt(id)));
        }
        return APIResultWrap.ok(String.valueOf(N3d.decode(id)));
    }


    /**
     * 用户封禁/解禁
     * true 封禁，false解封
     */
    @PostMapping(value = "/block")
    public APIResult<Object> blockStatus (@RequestBody BlockStatusParam param) throws Exception {
        Integer userId = N3d.decode(param.getUserId());
        usersService.blockStatus(userId, param.isStatus(), param.getMinute());
        return APIResultWrap.ok();
    }


    /**
     * 添加/删除白名单
     */
    @PostMapping(value = "/whiteList")
    public APIResult<Void> whiteList(@RequestBody RiskWhiteListParam param) throws Exception {
        ValidateUtils.notBlank(param.getRegion(),"region");
        ValidateUtils.notBlank(param.getPhone(),"phone");
        WhiteListTypeEnum whiteType = WhiteListTypeEnum.fromType(param.getType());
        if (whiteType == null) {
            return APIResultWrap.ok();
        }
        if (param.getDelete() != null && Constants.TRUE == param.getDelete()) {
            whiteListService.deleteWhite(param.getRegion(), param.getPhone(), whiteType);
        } else {
            whiteListService.saveWhite(param.getRegion(), param.getPhone(), whiteType);
        }
        if (whiteType == WhiteListTypeEnum.BLOCK_IP){
            IpInterceptor.LOAD_OK.set(false);
        }

        return APIResultWrap.ok();
    }

    @GetMapping(value = "/whiteList")
    public APIResult<Object> whiteList(@RequestParam(value = "type") Integer type) throws Exception {
        List<Map<String,String>> result = new ArrayList<>();
        WhiteListTypeEnum whiteType = WhiteListTypeEnum.fromType(type);
        if (whiteType == null){
            return APIResultWrap.ok(Collections.emptyList());
        }
        List<WhiteList> whiteLists = whiteListService.queryByType(whiteType);
        if (whiteLists == null || whiteLists.isEmpty()){
            return APIResultWrap.ok(Collections.emptyList());
        }
        return APIResultWrap.ok(whiteLists.stream().map(l -> Map.of("region", l.getRegion(), "phone", l.getPhone())).toList());
    }



    /**
     * 设置配置kv
     */
    @PostMapping(value = "setKV")
    public APIResult<Object> setKv(@RequestBody ConfigDTO configDTO) throws Exception{
        ConfigKeyEnum k = ConfigKeyEnum.fromKey(configDTO.getAttKey());
        if (k == null){
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_ILLEGAL, "key");
        }
        configService.saveOrUpdateConfig(k.getKey(), configDTO.getAttVal());
        return APIResultWrap.ok();
    }

    /**
     * 查询配置kv
     */
    @GetMapping(value = "kv")
    public APIResult<List<ConfigDTO>> getKv() throws Exception{

        var list = configService.selectAll();
        if (list == null || list.isEmpty()){
            return APIResultWrap.ok(Collections.emptyList());
        }
        var result = list.stream().map(c ->{
            var dto = new ConfigDTO();
            dto.setAttKey(c.getAttKey());
            dto.setAttVal(c.getAttValue());
            dto.setUpdateTime(c.getUpdateAt().getTime());
            return dto;
        }).toList();
        return APIResultWrap.ok(result);
    }


}
