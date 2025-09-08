package com.rcloud.server.sealtalk.controller;
import com.rcloud.server.sealtalk.entity.UrlStore;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.service.AiUserService;
import com.rcloud.server.sealtalk.service.UrlStoreService;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserDetailDTO;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserPageDTO;
import com.rcloud.server.sealtalk.model.dto.ai.TemplateDetailDTO;
import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import com.rcloud.server.sealtalk.entity.AiUser;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserCreateReq;
import com.rcloud.server.sealtalk.model.dto.ai.WhitelistReq;
import com.rcloud.server.sealtalk.model.dto.ai.TemplateCreateReq;
import com.rcloud.server.sealtalk.model.dto.ai.TemplateUpdateReq;
import com.rcloud.server.sealtalk.util.N3d;
import com.rcloud.server.sealtalk.util.ValidateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/admin/ai")
public class AiAdminController extends BaseController {

    @Autowired
    private AiUserService aiUserService;
    @Autowired
    private UrlStoreService urlStoreService;

    @PostMapping("/user/create")
    public APIResult<Map<String, Object>> create(@RequestBody AiUserCreateReq req) throws Exception{
        AiUser u = AiUser.defaultInstance();
        //新增的时候没有aiUserId, 修改的时候有aiUserId
        u.setAiUserId(req.getAiUserId());
        u.setAvatar(req.getAvatar());
        u.setGender(req.getGender());
        u.setAge(req.getAge());
        u.setOpen(req.getOpen() != null && req.getOpen());
        u.setSystemPrompt(req.getSystemPrompt());
        String userId = aiUserService.createWithI18n(u, req.getMultilingual(), null, null);
        Map<String, Object> result = new HashMap<>();
        result.put("aiUserId", userId);
        return APIResultWrap.ok(result);
    }

    @GetMapping("/user/page")
    public APIResult<AiUserPageDTO> page(@RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                         @RequestParam(value = "size", required = false, defaultValue = "10") int size,
                                         @RequestParam(value = "search", required = false) String search,
                                         @RequestParam(value = "source", required = false, defaultValue = "0") Integer createSource

    ) throws Exception {
        return APIResultWrap.ok(aiUserService.page(createSource, null, page, size));
    }

    @GetMapping("/user/{aiUserId}")
    public APIResult<AiUserDetailDTO> detail(@PathVariable("aiUserId") String aiUserId) {
        return APIResultWrap.ok(aiUserService.getDetail(aiUserId));
    }

    @DeleteMapping("/user/{aiUserId}")
    public APIResult<Void> delete(@PathVariable("aiUserId") String userId) throws Exception {
        aiUserService.delete(userId);
        return APIResultWrap.ok();
    }

    @GetMapping("/user/whitelist/{aiUserId}")
    public APIResult<List<Map<String, Object>>> whitelist(@PathVariable("aiUserId") String aiUserId) throws ServiceException {
        return APIResultWrap.ok(aiUserService.whitelist(aiUserId));
    }

    @PostMapping("/user/whitelist/add")
    public APIResult<Void> whitelistAdd(@RequestBody WhitelistReq req) throws ServiceException {
        List<Long> userIds = req.getUserIds().stream().map(x -> {
            try {
                return N3d.decode(x);
            } catch (Exception e) {
                log.warn("error userId : {}", x);
                return null;
            }
        }).filter(Objects::nonNull).map(Long::valueOf).collect(Collectors.toList());
        aiUserService.whitelistAdd(req.getAiUserId(), userIds);
        return APIResultWrap.ok();
    }

    @DeleteMapping("/user/whitelist/delete")
    public APIResult<Void> whitelistDelete(@RequestBody WhitelistReq req) {
        List<Long> userIds = req.getUserIds().stream().map(x -> {
            try {
                return N3d.decode(x);
            } catch (Exception e) {
                log.warn("error userId : {}", x);
                return null;
            }
        }).filter(Objects::nonNull).map(Long::valueOf).collect(Collectors.toList());
        aiUserService.whitelistDelete(req.getAiUserId(), userIds);
        return APIResultWrap.ok();
    }

    @PostMapping("/template/create")
    public APIResult<Map<String, Object>> templateCreate(@RequestBody TemplateCreateReq req) {
        String templateId = aiUserService.templateCreate(null, req.getItems());
        Map<String, Object> result = new HashMap<>();
        result.put("tempalteId", templateId);
        return APIResultWrap.ok(result);
    }

    @GetMapping("/template/{templateId}")
    public APIResult<TemplateDetailDTO> templateById(@PathVariable("templateId") String templateId) {
        TemplateDetailDTO template = aiUserService.templateById(templateId);
        return APIResultWrap.ok(template);
    }

    @PostMapping("/template/update")
    public APIResult<Void> templateUpdate(@RequestBody TemplateUpdateReq req) throws Exception {

        ValidateUtils.notBlank(req.getTemplateId(),"templateId");
        aiUserService.templateUpdate(req.getTemplateId(), req.getItems());
        return APIResultWrap.ok();
    }

    @DeleteMapping("/template/delete/{templateId}")
    public APIResult<Void> templateDelete(@PathVariable("templateId") String templateId) {
        aiUserService.templateDelete(templateId);
        return APIResultWrap.ok();
    }

    @GetMapping("/template/page")
    public APIResult<Map<String, Object>> templatePage(@RequestParam(value = "search", required = false) String search) {
        return APIResultWrap.ok(aiUserService.templatePage(search, false));
    }

    @PostMapping(value = "/upload")
    public APIResult<String> upload(@RequestBody Map<String,Object> body) throws Exception {
        String fileUrl = (String) body.get("fileUrl");
        ValidateUtils.notBlank(fileUrl, "fileUrl");
        urlStoreService.saveUrl(fileUrl);
        return APIResultWrap.ok(fileUrl);
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

    @DeleteMapping("/avatar/{avatarId}")
    public APIResult<Void> deleteAvatar(@PathVariable("avatarId") Long avatarId) {
        urlStoreService.delUrlById(avatarId);
        return APIResultWrap.ok();
    }

}


