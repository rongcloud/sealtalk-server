package com.rcloud.server.sealtalk.service;

import com.rcloud.server.sealtalk.configuration.SealtalkConfig;
import com.rcloud.server.sealtalk.constant.AiUserCreateSourceEnum;
import com.rcloud.server.sealtalk.constant.AiUserCreateTypeEnum;
import com.rcloud.server.sealtalk.constant.BindType;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.ErrorMsg;
import com.rcloud.server.sealtalk.constant.LanguageEnum;
import com.rcloud.server.sealtalk.dao.AiTemplateMapper;
import com.rcloud.server.sealtalk.dao.AiUserMapper;
import com.rcloud.server.sealtalk.dao.AiUserI18nMapper;
import com.rcloud.server.sealtalk.dao.BotUserBindMapper;
import com.rcloud.server.sealtalk.entity.AiTemplate;
import com.rcloud.server.sealtalk.entity.AiUser;
import com.rcloud.server.sealtalk.entity.AiUserI18n;
import com.rcloud.server.sealtalk.entity.BotUserBind;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.model.dto.ai.AiSuggestionDTO;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserFlatDTO;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserPageDTO;
import com.rcloud.server.sealtalk.model.dto.ai.PageResult;
import com.rcloud.server.sealtalk.model.dto.ai.TemplateDetailDTO;
import com.rcloud.server.sealtalk.model.dto.ai.TemplateI18nItem;
import com.rcloud.server.sealtalk.rongcloud.RongCloudClient;
import com.rcloud.server.sealtalk.util.N3d;
import com.rcloud.server.sealtalk.util.RandomUtil;
import com.rcloud.server.sealtalk.entity.Users;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserMultilingualItemDTO;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserListItemDTO;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserDetailDTO;
import com.rcloud.server.sealtalk.model.dto.ai.AiUserLangDTO;
import com.rcloud.server.sealtalk.util.TimeUtil;
import io.rong.models.Result;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class AiUserService {

    @Autowired
    private AiUserMapper aiUserMapper;

    @Autowired
    private AiTemplateMapper aiTemplateMapper;

    @Autowired
    private AiUserI18nMapper aiUserI18nMapper;

    @Autowired
    private BotUserBindMapper botUserBindMapper;

    @Autowired
    private UsersService usersService;


    @Autowired
    private RongCloudClient rongCloudClient;

    @Autowired
    private SealtalkConfig sealtalkConfig;

    public String createWithI18n(AiUser aiUser, List<AiUserMultilingualItemDTO> multilingual, AiTemplate template, Long currentUserId) throws Exception {

        if (StringUtils.isBlank(aiUser.getAiUserId())) {
            int total = aiUserMapper.count(null, null);
            if (total >= Constants.ALL_AGENT_CNT_LIMIT) {
                throw ServiceException.buildError(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.NUM_EXCEED_LIMIT, "ai user");
            }
            if (currentUserId != null) {
                int userCnt = aiUserMapper.countByCreatorId(currentUserId);
                if (userCnt >= Constants.USER_AGENT_CNT_LIMIT) {
                    throw ServiceException.buildError(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.NUM_EXCEED_LIMIT, "ai user");
                }
            }
        }

        boolean update = false;
        if (StringUtils.isNotBlank(aiUser.getAiUserId())) {
            AiUser aiUserDb = aiUserMapper.selectByAiUserId(aiUser.getAiUserId());
            if (aiUserDb == null || (currentUserId != null && !currentUserId.equals(aiUserDb.getCreatorId()))) {
                throw ServiceException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "ai user");
            }
            update = true;
            // 删除主表记录
            aiUserMapper.deleteByUserId(aiUser.getAiUserId());
            // 删除多语言信息
            aiUserI18nMapper.deleteByAiUserId(aiUser.getAiUserId());
        }

        String aiUserId = StringUtils.isNotBlank(aiUser.getAiUserId()) ? aiUser.getAiUserId() : Constants.AI_USER_ID_PREFIX + RandomUtil.uuid();
        aiUser.setAiUserId(aiUserId);
        aiUser.fillDefaultsIfNull();
        aiUserMapper.insert(aiUser);

        AiUserI18n defaultI18n = null;
        if (multilingual != null && !multilingual.isEmpty()) {
            List<AiUserI18n> list = new ArrayList<>();
            for (AiUserMultilingualItemDTO m : multilingual) {
                AiUserI18n i18n = new AiUserI18n();
                i18n.setAiUserId(aiUserId);
                i18n.setLanguage(m.getLanguage());
                i18n.setNickname(m.getNickname());
                i18n.setArea(m.getArea());
                i18n.setProfession(m.getProfession());
                i18n.setIntroduction(m.getIntroduction());
                i18n.setTags(m.getTags() == null ? "" : String.join(",", m.getTags()));
                i18n.setOpeningRemark(m.getOpeningRemark());
                i18n.fillDefaultsIfNull();
                list.add(i18n);
                if (defaultI18n == null || LanguageEnum.ZH.getLanguage().equals(m.getLanguage())) {
                    defaultI18n = i18n;
                }
            }
            aiUserI18nMapper.batchInsert(list);
        }

        String systemPrompt = aiUser.getSystemPrompt();
        if (aiUser.getCreateSource() == AiUserCreateSourceEnum.USER.getType() && aiUser.getCreateType() == AiUserCreateTypeEnum.TEMPLATE_CREATE.getType() && template != null) {
            systemPrompt = template.getSystemPrompt().replace("{R}", systemPrompt);
        }
        boolean likeHuman = false;
        if (aiUser.getCreateSource() == AiUserCreateSourceEnum.ADMIN.getType() || aiUser.getCreateType() == AiUserCreateTypeEnum.TEMPLATE_CREATE.getType()) {
            likeHuman = true;
        }

        if (update) {
            rongCloudClient.updateAgent(aiUser, defaultI18n, sealtalkConfig.getAiAgentModel(), systemPrompt);
            rongCloudClient.updateBot_Agent(aiUser, defaultI18n, likeHuman);
        } else {
            Result result = rongCloudClient.createAgent(aiUser, defaultI18n, sealtalkConfig.getAiAgentModel(), systemPrompt);
            if (Constants.CODE_OK.equals(result.getCode())) {
                rongCloudClient.createBot_Agent(aiUser, defaultI18n, likeHuman);
            }
        }
        return aiUserId;
    }

    public AiUserPageDTO page(Integer createSource, Boolean open, int page, int size) throws ServiceException {
        int offset = (page - 1) * size;
        int total = aiUserMapper.count(open, createSource);
        List<AiUser> list = aiUserMapper.pageQuery(open, createSource, offset, size);

        AiUserPageDTO resp = new AiUserPageDTO();
        resp.setTotal(total);
        // 批量查询 i18n 并聚合为 map
        List<String> ids = list.stream().map(AiUser::getAiUserId).collect(Collectors.toList());
        Map<String, Map<String, AiUserLangDTO>> multiMap = new HashMap<>();
        if (!ids.isEmpty()) {
            List<AiUserI18n> i18ns = aiUserI18nMapper.selectByAiUserIds(ids);
            for (AiUserI18n i : i18ns) {
                Map<String, AiUserLangDTO> langMap = multiMap.computeIfAbsent(i.getAiUserId(), k -> new java.util.HashMap<>());
                AiUserLangDTO dto = langMap.computeIfAbsent(i.getLanguage(), k -> new AiUserLangDTO());
                dto.setNickname(i.getNickname());
                dto.setArea(i.getArea());
                dto.setProfession(i.getProfession());
                dto.setIntroduction(i.getIntroduction());
                String tagsStr = i.getTags();
                dto.setTags(tagsStr == null || tagsStr.isEmpty() ? java.util.Collections.emptyList() : java.util.Arrays.asList(tagsStr.split(",")));
                dto.setOpeningRemark(i.getOpeningRemark());
                dto.setLanguage(i.getLanguage());
            }
        }
        List<AiUserListItemDTO> outList = new ArrayList<>();
        for (AiUser u : list) {
            AiUserListItemDTO item = new AiUserListItemDTO();
            item.setAiUserId(u.getAiUserId());
            item.setAvatar(u.getAvatar());
            item.setGender(u.getGender());
            item.setAge(u.getAge());
            item.setOpen(u.getOpen());
            item.setWhiteSize(0);
            item.setUpdateTime(u.getUpdatedTime() == null ? null : u.getUpdatedTime().getTime());
            if (u.getCreatorId() != null && u.getCreatorId() > 0) {
                item.setCreatorId(N3d.encode(u.getCreatorId()));
            }
            item.setCreateSource(u.getCreateSource());
            item.setCreateType(u.getCreateType());
            item.setSystemPrompt(u.getSystemPrompt());
            item.setTemplateId(u.getTemplateId());
            item.setMultilingual(multiMap.getOrDefault(u.getAiUserId(), java.util.Collections.emptyMap()));
            outList.add(item);
        }
        resp.setList(outList);
        return resp;
    }


    /**
     * 开放的ai角色
     *
     * @param userId
     */
    public AiUserPageDTO openAiUsers(long userId) {

        List<AiUser> openAiUser = aiUserMapper.queryAllByType(true, AiUserCreateSourceEnum.ADMIN.getType(), null, 1000);
        List<BotUserBind> binds = botUserBindMapper.selectByUserId(userId, BindType.AI_USER.getType());
        if (binds != null && !binds.isEmpty()) {
            List<AiUser> privateAiUsers = aiUserMapper.selectByAiUserIds(binds.stream().map(BotUserBind::getBotId).collect(Collectors.toList()));
            openAiUser.addAll(privateAiUsers);
        }
        AiUserPageDTO resp = new AiUserPageDTO();
        resp.setTotal(openAiUser.size());
        // 批量查询 i18n 并聚合为 map
        List<String> ids = openAiUser.stream().map(AiUser::getAiUserId).collect(Collectors.toList());
        Map<String, Map<String, AiUserLangDTO>> multiMap = new HashMap<>();
        if (!ids.isEmpty()) {
            List<AiUserI18n> i18ns = aiUserI18nMapper.selectByAiUserIds(ids);
            for (AiUserI18n i : i18ns) {
                Map<String, AiUserLangDTO> langMap = multiMap.computeIfAbsent(i.getAiUserId(), k -> new java.util.HashMap<>());
                AiUserLangDTO dto = langMap.computeIfAbsent(i.getLanguage(), k -> new AiUserLangDTO());
                dto.setNickname(i.getNickname());
                dto.setArea(i.getArea());
                dto.setProfession(i.getProfession());
                dto.setIntroduction(i.getIntroduction());
                String tagsStr = i.getTags();
                dto.setTags(tagsStr == null || tagsStr.isEmpty() ? java.util.Collections.emptyList() : java.util.Arrays.asList(tagsStr.split(",")));
                dto.setLanguage(i.getLanguage());
            }
        }
        List<AiUserListItemDTO> outList = new ArrayList<>();
        for (AiUser u : openAiUser) {
            AiUserListItemDTO item = new AiUserListItemDTO();
            item.setAiUserId(u.getAiUserId());
            item.setAvatar(u.getAvatar());
            item.setGender(u.getGender());
            item.setAge(u.getAge());
            item.setMultilingual(multiMap.getOrDefault(u.getAiUserId(), java.util.Collections.emptyMap()));
            outList.add(item);
        }
        resp.setList(outList);
        return resp;
    }


    public PageResult<AiUserFlatDTO> userCreateList(long userId) {
        List<AiUser> openAiUser = aiUserMapper.queryByCreator(userId);
        PageResult<AiUserFlatDTO> resp = new PageResult();
        resp.setTotal(openAiUser.size());
        List<String> ids = openAiUser.stream().map(AiUser::getAiUserId).collect(Collectors.toList());
        Map<String, AiUserFlatDTO> multiMap = new HashMap<>();
        if (!ids.isEmpty()) {
            List<AiUserI18n> i18ns = aiUserI18nMapper.selectByAiUserIds(ids);
            for (AiUserI18n i : i18ns) {
                AiUserFlatDTO dto = multiMap.computeIfAbsent(i.getAiUserId(), k -> new AiUserFlatDTO());
                dto.setAiUserId(i.getAiUserId());
                dto.setNickname(i.getNickname());
                dto.setLanguage(i.getLanguage());
            }
        }
        List<String> templateIds = openAiUser.stream()
                .filter(au -> StringUtils.isNotBlank(au.getTemplateId()) && au.getCreateType() != null && au.getCreateType() == AiUserCreateTypeEnum.TEMPLATE_CREATE.getType())
                .map(AiUser::getTemplateId)
                .collect(Collectors.toList());

        Map<String, TemplateDetailDTO> templateI18nMap = new HashMap<>();
        if (!templateIds.isEmpty()) {
            List<AiTemplate> templates = aiTemplateMapper.selectByTemplateIds(templateIds);
            templateI18nMap = templateI18n(templates, true);
        }

        List<AiUserFlatDTO> outList = new ArrayList<>();
        for (AiUser u : openAiUser) {
            AiUserFlatDTO dto = multiMap.computeIfAbsent(u.getAiUserId(), k -> new AiUserFlatDTO());
            dto.setAiUserId(u.getAiUserId());
            dto.setAvatar(u.getAvatar());
            dto.setSystemPrompt(u.getSystemPrompt());
            dto.setTemplateId(u.getTemplateId());
            dto.setCreateType(u.getCreateType());
            TemplateDetailDTO templateDetailDTO = u.getTemplateId() == null ? null : templateI18nMap.get(u.getTemplateId());
            dto.setTemplateI18n(templateDetailDTO == null ? null : templateDetailDTO.getI18n());
            outList.add(dto);
        }
        resp.setList(outList);
        return resp;
    }


    public AiUserDetailDTO getDetail(String aiUserId) {
        AiUser u = aiUserMapper.selectByAiUserId(aiUserId);
        if (u == null) {
            return null;
        }
        AiUserDetailDTO dto = new AiUserDetailDTO();
        dto.setAiUserId(u.getAiUserId());
        dto.setAvatar(u.getAvatar());
        dto.setGender(u.getGender());
        dto.setAge(u.getAge());
        dto.setOpen(u.getOpen());
        dto.setSystemPrompt(u.getSystemPrompt());
        dto.setCreateSource(u.getCreateSource());
        dto.setCreateType(u.getCreateType());
        List<AiUserI18n> i18ns = aiUserI18nMapper.selectByAiUserId(aiUserId);
        Map<String, AiUserLangDTO> ml = new HashMap<>();
        AiUserLangDTO tmp = null;
        for (AiUserI18n i : i18ns) {
            AiUserLangDTO v = new AiUserLangDTO();
            v.setNickname(i.getNickname());
            v.setArea(i.getArea());
            v.setProfession(i.getProfession());
            v.setIntroduction(i.getIntroduction());
            String tags = i.getTags();
            v.setTags(tags == null || tags.isEmpty() ? java.util.Collections.emptyList() : java.util.Arrays.asList(tags.split(",")));
            v.setOpeningRemark(i.getOpeningRemark());
            v.setLanguage(i.getLanguage());
            tmp = v;
            ml.put(i.getLanguage(), v);
        }
        if (AiUserCreateSourceEnum.USER.getType() == u.getCreateSource()) {
            //端上要显示多语言
            ml.putIfAbsent(LanguageEnum.ZH.getLanguage(), tmp);
            ml.putIfAbsent(LanguageEnum.EN.getLanguage(), tmp);
            ml.putIfAbsent(LanguageEnum.AR.getLanguage(), tmp);
        }


        dto.setMultilingual(ml);
        return dto;
    }

    public void delete(String aiUserId) throws Exception {
        // 删除主表记录
        aiUserMapper.deleteByUserId(aiUserId);
        // 删除多语言信息
        aiUserI18nMapper.deleteByAiUserId(aiUserId);
        // 删除绑定的用户（复用 bot_user_bind，按 bot_id 删除）
        botUserBindMapper.deleteByBotId(aiUserId);

        rongCloudClient.deleteAgent(aiUserId);
        rongCloudClient.delBot(aiUserId);

    }

    public List<Map<String, Object>> whitelist(String aiUserId) throws ServiceException {
        List<BotUserBind> binds = botUserBindMapper.selectByBotId(aiUserId);
        if (binds == null || binds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Integer> uids = binds.stream()
                .map(BotUserBind::getUserId)
                .filter(java.util.Objects::nonNull)
                .map(Long::intValue)
                .collect(Collectors.toList());
        if (uids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Users> users = usersService.queryByIds(uids);
        java.util.Map<Integer, String> id2name = new java.util.HashMap<>();
        for (Users u : users) {
            id2name.put(u.getId(), u.getNickname());
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Integer uid : uids) {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            String userIdStr = N3d.encode(uid);
            item.put("userId", userIdStr);
            item.put("name", id2name.getOrDefault(uid, ""));
            result.add(item);
        }
        return result;
    }

    public void whitelistAdd(String aiUserId, List<Long> bindUserIds) throws ServiceException {
        // 仅非公开 AI 用户可添加白名单
        AiUser ai = aiUserMapper.selectByAiUserId(aiUserId);
        if (ai == null) {
            throw ServiceException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "ai user");
        }
        if (Boolean.TRUE.equals(ai.getOpen())) {
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), "public ai user cannot add whitelist");
        }
        List<BotUserBind> list = new java.util.ArrayList<>();
        java.util.Date now = new java.util.Date();
        for (Long uid : bindUserIds) {
            if (uid == null) continue;
            BotUserBind b = new BotUserBind();
            b.setBotId(aiUserId);
            b.setUserId(uid);
            b.setBindType(BindType.AI_USER.getType());
            b.setCreatedTime(now);
            b.setUpdatedTime(now);
            list.add(b);
        }
        if (!list.isEmpty()) {
            botUserBindMapper.batchInsert(list);
        }
    }

    public void whitelistDelete(String aiUserId, List<Long> bindUserIds) {
        if (bindUserIds == null || bindUserIds.isEmpty()) {
            return;
        }
        java.util.List<Long> ids = bindUserIds.stream().filter(java.util.Objects::nonNull).collect(Collectors.toList());
        if (!ids.isEmpty()) {
            botUserBindMapper.batchDeleteByBotIdAndUserIds(aiUserId, ids);
        }
    }

    public String templateCreate(String templateId, List<TemplateI18nItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items is empty");
        }
        templateId = StringUtils.isBlank(templateId) ? RandomUtil.uuid() : templateId;
        List<AiTemplate> list = new java.util.ArrayList<>();
        for (TemplateI18nItem item : items) {
            if (item == null) continue;
            AiTemplate t = new AiTemplate();
            t.setTemplateId(templateId);
            t.setTemplateName(item.getTemplateName());
            t.setSystemPrompt(item.getSystemPrompt());
            t.setLanguage(item.getLanguage());
            list.add(t);
        }
        if (!list.isEmpty()) {
            aiTemplateMapper.batchInsert(list);
        }
        return templateId;
    }

    public void templateUpdate(String templateId, List<TemplateI18nItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        // 先删除，再批量新建
        templateDelete(templateId);
        templateCreate(templateId, items);
    }

    public void templateDelete(String templateId) {
        aiTemplateMapper.deleteByTemplateId(templateId);
    }

    public Map<String, Object> templatePage(String search, boolean onlyName) {
        List<AiTemplate> list = aiTemplateMapper.pageQuery(search, 1000);
        Map<String, TemplateDetailDTO> templateMap = templateI18n(list, onlyName);
        return java.util.Collections.unmodifiableMap(new java.util.HashMap<String, Object>() {{
            put("total", templateMap.size());
            put("list", templateMap.values());
        }});
    }

    private Map<String, TemplateDetailDTO> templateI18n(List<AiTemplate> templates, boolean onlyName) {
        Map<String, TemplateDetailDTO> templateMap = new LinkedHashMap<>();
        templates.forEach(t -> {
            TemplateDetailDTO dto = templateMap.computeIfAbsent(t.getTemplateId(), k -> new TemplateDetailDTO());
            dto.setTemplateId(t.getTemplateId());
            TemplateI18nItem item = new TemplateI18nItem();
            item.setLanguage(t.getLanguage());
            item.setTemplateName(t.getTemplateName());
            if (!onlyName) {
                item.setSystemPrompt(t.getSystemPrompt());
            }
            dto.getI18n().put(t.getLanguage(), item);
        });
        return templateMap;
    }


    public TemplateDetailDTO templateById(String templateId) {
        List<AiTemplate> template = aiTemplateMapper.selectByTemplateId(templateId);
        Map<String, TemplateDetailDTO> templateMap = templateI18n(template, false);
        return templateMap.get(templateId);
    }


    public AiTemplate getTemplate(String templateId, String language) {
        return aiTemplateMapper.selectByTemplateIdAndLanguage(templateId, language);
    }


    public List<String> suggestion(long currentId, String currentIdStr, AiSuggestionDTO dto) throws Exception {

        Users currentUser = usersService.queryById((int) currentId);
        AiUser aiUser = aiUserMapper.selectByAiUserId(dto.getAiUserId());
        List<AiUserI18n> aiUserI18ns = aiUserI18nMapper.selectByAiUserId(dto.getAiUserId());

        AiUserI18n defaultI18n = null;
        for (AiUserI18n aiUserI18n : aiUserI18ns) {
            if (defaultI18n == null || LanguageEnum.ZH.getLanguage().equals(aiUserI18n.getLanguage())) {
                defaultI18n = aiUserI18n;
            }
        }

        StringBuilder aiUserInfo = new StringBuilder();
        aiUserInfo.append("聊天对象: ")
                .append(defaultI18n.getNickname())
                .append(" ");
        if (aiUser.getAge() != null && aiUser.getAge() > 0){
            aiUserInfo.append(aiUser.getAge()).append(" ");
        }
        if (StringUtils.isNotBlank(defaultI18n.getProfession())){
            aiUserInfo.append(defaultI18n.getProfession());
        }
        aiUserInfo.append("\n");

        StringBuilder userInfo = new StringBuilder();
        userInfo.append("回复推荐者: ").append(currentUser.getNickname()).append("\n");

        Map<String,String> nameMap = new HashMap<>();
        nameMap.put(currentIdStr,currentUser.getNickname());
        nameMap.put(dto.getAiUserId(), defaultI18n.getNickname());


        StringBuilder msg = new StringBuilder();
        for (AiSuggestionDTO.Msg m : dto.getMsgs()) {
            String name = nameMap.getOrDefault(m.getSenderId(), m.getSenderId());
            msg.append(name).append(" ").append(TimeUtil.format(m.getTime())).append(" :").append(m.getContent()).append("\n");
        }
        String queryMsg = aiUserInfo.toString() + userInfo + msg;
        String agentAnswer = rongCloudClient.agentChat(currentIdStr, Constants.SUGGESTION_AGENT_ID, queryMsg);
        return Arrays.asList(agentAnswer.split("\n"));
    }


}


