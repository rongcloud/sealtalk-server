package com.rcloud.server.sealtalk.controller;

import com.rcloud.server.sealtalk.constant.AiUserCreateSourceEnum;
import com.rcloud.server.sealtalk.constant.AiUserCreateTypeEnum;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.entity.AiTemplate;
import com.rcloud.server.sealtalk.entity.AiUser;
import com.rcloud.server.sealtalk.entity.UrlStore;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.service.AiUserService;
import com.rcloud.server.sealtalk.model.ServerApiParams;
import com.rcloud.server.sealtalk.model.dto.ai.AiSuggestionDTO;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserFlatDTO;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserMultilingualItemDTO;
import com.rcloud.server.sealtalk.model.dto.ai.PageResult;
import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import com.rcloud.server.sealtalk.model.dto.ai.TemplateCreateUserReq;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserSimpleCreateReq;
import com.rcloud.server.sealtalk.model.dto.ai.AiCreateResultDTO;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserPageDTO;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserDetailDTO;
import com.rcloud.server.sealtalk.service.UrlStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/ai")
public class AiUserController extends BaseController {

    @Autowired
    private AiUserService aiUserService;

    @Autowired
    private UrlStoreService urlStoreService;

    // POST /ai/user/templateCreate
    @PostMapping("/user/templateCreate")
    public APIResult<AiCreateResultDTO> userTemplateCreate(@RequestBody TemplateCreateUserReq req) throws Exception{
        AiTemplate t = aiUserService.getTemplate(req.getTemplateId(), req.getLanguage());
        if (t == null) {
            throw new ServiceException(ErrorCode.PARAM_ERROR.getErrorCode(), "template not exist");
        }

        long currentId = getCurrentUserId().longValue();
        AiUser u = new AiUser();
        //新增的时候没有aiUserId, 修改的时候有aiUserId
        u.setAiUserId(req.getAiUserId());
        u.setTemplateId(req.getTemplateId());
        u.setAvatar(req.getAvatar());
        // 以模板 systemPrompt 为主，若前端传入则覆盖
        u.setSystemPrompt(req.getSystemPrompt());
        u.setOpen(false);
        u.setCreatorId(getCurrentUserId() == null ? null : getCurrentUserId().longValue());
        u.setCreateSource(AiUserCreateSourceEnum.USER.getType());
        u.setCreateType(AiUserCreateTypeEnum.TEMPLATE_CREATE.getType());

        AiUserMultilingualItemDTO i18nDTO = new AiUserMultilingualItemDTO();
        i18nDTO.setLanguage(req.getLanguage());
        i18nDTO.setNickname(req.getNickname());

        String aiUserId = aiUserService.createWithI18n(u, Collections.singletonList(i18nDTO),t,currentId);
        AiCreateResultDTO result = new AiCreateResultDTO();
        result.setAiUserId(aiUserId);
        return APIResultWrap.ok(result);
    }

    // POST /ai/user/create
    @PostMapping("/user/create")
    public APIResult<AiCreateResultDTO> userCreate(@RequestBody AiUserSimpleCreateReq req) throws Exception {

        long currentId = getCurrentUserId().longValue();
        AiUser u = new AiUser();
        //新增的时候没有aiUserId, 修改的时候有aiUserId
        u.setAiUserId(req.getAiUserId());
        u.setAvatar(req.getAvatar());
        u.setSystemPrompt(req.getSystemPrompt());
        u.setOpen(false);
        u.setCreatorId(getCurrentUserId().longValue());
        u.setCreateSource(AiUserCreateSourceEnum.USER.getType());
        u.setCreateType(AiUserCreateTypeEnum.ALL_CREATE.getType());
        AiUserMultilingualItemDTO i18nDTO = new AiUserMultilingualItemDTO();
        i18nDTO.setLanguage(req.getLanguage());
        i18nDTO.setNickname(req.getNickname());

        String aiUserId = aiUserService.createWithI18n(u, Collections.singletonList(i18nDTO), null, currentId);
        AiCreateResultDTO result = new AiCreateResultDTO();
        result.setAiUserId(aiUserId);
        return APIResultWrap.ok(result);
    }

    // GET /ai/user/page
    @GetMapping("/user/page")
    public APIResult<AiUserPageDTO> publicPage() {

        long currentId = getCurrentUserId().longValue();
        return APIResultWrap.ok(aiUserService.openAiUsers(currentId));
    }

    // GET /ai/user/private/page
    @GetMapping("/user/private/page")
    public APIResult<PageResult<AiUserFlatDTO>> myPage() {
        // 简化：暂复用公开接口返回结构，按需可扩展服务层支持按 creatorId 查询
        long currentId = getCurrentUserId().longValue();
        return APIResultWrap.ok(aiUserService.userCreateList(currentId));
    }

    // GET /ai/user/{userId}
    @GetMapping("/user/{aiUserId}")
    public APIResult<AiUserDetailDTO> userDetail(@PathVariable("aiUserId") String userId) {
        return APIResultWrap.ok(aiUserService.getDetail(userId));
    }

    // GET /ai/tempalte/page （按文档拼写保留）
    @GetMapping("/tempalte/page")
    public APIResult<Map<String, Object>> templatePage() {
        return APIResultWrap.ok(aiUserService.templatePage(null, true));
    }


    @PostMapping("/suggestion")
    public APIResult<List<String>> suggestion(@RequestBody AiSuggestionDTO suggestionDTO) throws Exception {
        ServerApiParams param = getServerApiParams();
        return APIResultWrap.ok(aiUserService.suggestion(param.getCurrentUserId(), param.getCurrentUserIdStr(), suggestionDTO));
    }




    @GetMapping("/avatar/list")
    public APIResult<List<Map<String,Object>>> avatarList() {
        List<UrlStore> all = urlStoreService.getAll();
        List<Map<String,Object>> list = new ArrayList<>();
        if (all != null) {
            for (UrlStore u : all) {
                Map<String,Object> tmp = new HashMap<>();
                tmp.put("id",u.getId());
                tmp.put("url",u.getUrl());
                list.add(tmp);
            }
        }
        return APIResultWrap.ok(list);
    }






    
}


