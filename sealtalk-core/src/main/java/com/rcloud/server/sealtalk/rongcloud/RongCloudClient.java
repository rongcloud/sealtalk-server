package com.rcloud.server.sealtalk.rongcloud;

import com.rcloud.server.sealtalk.configuration.SealtalkConfig;
import com.rcloud.server.sealtalk.entity.AiUser;
import com.rcloud.server.sealtalk.entity.AiUserI18n;
import com.rcloud.server.sealtalk.exception.RCloudHttpException;
import com.rcloud.server.sealtalk.model.JwtTokenResult;
import com.rcloud.server.sealtalk.model.dto.RCBotDTO;
import com.rcloud.server.sealtalk.model.dto.RCBotDTO.Integration;
import com.rcloud.server.sealtalk.service.HttpService;
import com.rcloud.server.sealtalk.util.JacksonUtil;
import com.rcloud.server.sealtalk.util.RongCloudApiUtil;
import io.rong.RongCloud;
import io.rong.messages.BaseMessage;
import io.rong.methods.message._private.Private;
import io.rong.methods.message.system.MsgSystem;
import io.rong.methods.user.User;
import io.rong.methods.user.blacklist.Blacklist;
import io.rong.models.Result;
import io.rong.models.agent.AgentConfig;
import io.rong.models.agent.AgentModel;
import io.rong.models.agent.ChatModel;
import io.rong.models.agent.Model;
import io.rong.models.agent.ModelOptions;
import io.rong.models.agent.Prompt;
import io.rong.models.bot.ChatbotInfoModel;
import io.rong.models.bot.ChatbotIntegration;
import io.rong.models.bot.SetChatbotIntegration;
import io.rong.models.group.GroupMember;
import io.rong.models.group.GroupModel;
import io.rong.models.message.ChatroomMessage;
import io.rong.models.message.GroupMessage;
import io.rong.models.message.MentionMessage;
import io.rong.models.message.PrivateMessage;
import io.rong.models.message.SystemMessage;
import io.rong.models.message.UltraGroupMessage;
import io.rong.models.response.BlackListResult;
import io.rong.models.response.ChatAgentResult;
import io.rong.models.response.TokenResult;
import io.rong.models.response.UserResult;
import io.rong.models.ultragroup.UltraGroupMember;
import io.rong.models.ultragroup.UltraGroupModel;
import io.rong.models.user.UserIdListModel;
import io.rong.models.user.UserModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


@Slf4j
@Service
public class RongCloudClient implements InitializingBean {

    @Autowired
    private HttpService httpService;

    @Autowired
    private SealtalkConfig sealtalkConfig;

    private RongCloud rongCloud;

    private User User;
    private Blacklist BlackList;

    private Private Private;
    private MsgSystem system;




    private static final String ADD_USERGROUP = "/ultragroup/usergroup/add.json";
    private static final String DEL_USERGROUP = "/ultragroup/usergroup/del.json";
    private static final String USERGROUP_BIND_USER = "/ultragroup/usergroup/user/add.json";
    private static final String USERGROUP_UNBIND_USER = "/ultragroup/usergroup/user/del.json";
    private static final String CHANNEL_BING_USERGROUP = "/ultragroup/channel/usergroup/bind.json";
    private static final String CHANNEL_UNBING_USERGROUP = "/ultragroup/channel/userGroup/unbind.json";


    @Override
    public void afterPropertiesSet() throws Exception {
        List<String> hosts = Stream.of(sealtalkConfig.getRongcloudApiUrl().split(","))
                .filter(StringUtils::isNotBlank)
                .map(host -> host.startsWith("http://") || host.startsWith("https://") ? host : "http://" + host)
                .toList();
        log.info("INIT RC HOST {}", hosts);
        String mainHost = hosts.get(0);
        String[] backHosts = hosts.size() > 1 ? hosts.subList(1, hosts.size()).toArray(new String[0]) : new String[0];
        rongCloud = RongCloud.getInstance(sealtalkConfig.getRongcloudAppKey(), sealtalkConfig.getRongcloudAppSecret(), mainHost, backHosts);
        User = rongCloud.user;
        BlackList = rongCloud.user.blackList;
        Private = rongCloud.message.msgPrivate;
        system = rongCloud.message.system;
    }

    public TokenResult register(String userId, String name, String portrait) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(()->{
            UserModel user = new UserModel().setId(userId).setName(name).setPortrait(portrait);
            return User.register(user);
        });
    }

    public Result updateUser(String userId, String name, String portrait) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(()->{
            UserModel user = new UserModel().setId(userId).setName(name).setPortrait(portrait);
            return User.update(user);
        });
    }

    public UserResult getUserInfo(String userId) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(()->{
            UserModel user = new UserModel().setId(userId);
            return User.get(user);
        });
    }

    public Result addUserBlackList(String userId, List<String> targetIds) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(() -> {
            UserModel[] blacks = targetIds.stream().map(id -> new UserModel().setId(id)).toList().toArray(new UserModel[0]);
            return BlackList.add(new UserModel().setId(userId).setBlacklist(blacks));
        });
    }

    public BlackListResult queryUserBlackList(String userId) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(() -> BlackList.getList(new UserModel().setId(userId)));
    }


    public void userBlock(String userId, Integer minute) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> {
            UserModel user = new UserModel().setId(userId).setMinute(minute);
            return User.block.add(user);
        });
    }

    public void userBlockRemove(String userId) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> User.block.remove(userId));
    }

    public Result removeUserBlackList(String userId, List<String> targetIds) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(() -> {
            UserModel[] blacks = targetIds.stream().map(id -> new UserModel().setId(id)).toList().toArray(new UserModel[0]);
            return BlackList.remove(new UserModel().setId(userId).setBlacklist(blacks));
        });
    }



    public void sendSystemMessage(String senderId, List<String> targetIds, BaseMessage baseMessage) throws RCloudHttpException{
        RCExecuteWrapper.executeSDK(() -> {
            SystemMessage systemMessage = new SystemMessage()
                    .setSenderId(senderId)
                    .setTargetId(targetIds.toArray(new String[0]))
                    .setObjectName(baseMessage.getType())
                    .setContent(baseMessage);
            return system.send(systemMessage);
        });
    }

    public void sendPrivateMessage(PrivateMessage privateMessage) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> Private.send(privateMessage));
    }

    public void sendGroupMessage(GroupMessage groupMessage) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> rongCloud.message.group.send(groupMessage));
    }

    public void sendGroupMentionMessage(MentionMessage mentionMessage) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> rongCloud.message.group.sendMention(mentionMessage));
    }

    public void sendUltraGroupMessage(UltraGroupMessage ultraGroupMessage) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> rongCloud.message.ultraGroup.send(ultraGroupMessage));
    }

    public void sendChatroomMessage(ChatroomMessage message) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> rongCloud.message.chatroom.send(message));
    }

    public void createGroup(String groupId,  String name, List<String> memberIds) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> {
            GroupModel group = new GroupModel().setId(groupId)
                    .setMembers(memberIds.stream().map(m -> new GroupMember().setId(m)).toList().toArray(new GroupMember[0]))
                    .setName(name);
            return rongCloud.group.create(group);
        });
    }


    public void clearHistoryMessage( String fromUserId, String targetId,String conversationType, String msgTimestamp) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> rongCloud.message.history.clean(conversationType, fromUserId, targetId, msgTimestamp));
    }

    public Result joinGroup(String groupId, String groupName, List<String> memberIds) throws RCloudHttpException {

        return RCExecuteWrapper.executeSDK(() -> {
            GroupModel group = new GroupModel().setId(groupId)
                    .setMembers(memberIds.stream().map(m -> new GroupMember().setId(m)).toList().toArray(new GroupMember[0]))
                    .setName(groupName);
            return rongCloud.group.join(group);
        });
    }

    public void refreshGroupName(String groupId, String name) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> rongCloud.group.update(new GroupModel().setId(groupId).setName(name)));
    }

    public Result addGroupWhitelist(String groupId, List<String> memberIds) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(() -> {
            GroupModel group = new GroupModel().setId(groupId)
                    .setMembers(memberIds.stream().map(m -> new GroupMember().setId(m)).toList().toArray(new GroupMember[0]));
            return rongCloud.group.ban.whitelist.user.add(group);
        });
    }

    public Result removeGroupWhiteList(String groupId, List<String> memberIds) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(() -> {
            GroupModel group = new GroupModel().setId(groupId)
                    .setMembers(memberIds.stream().map(m -> new GroupMember().setId(m)).toList().toArray(new GroupMember[0]));
            return rongCloud.group.ban.whitelist.user.remove(group);
        });
    }


    public Result dismiss(String userId, String groupId) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(() -> {
            GroupModel group = new GroupModel().setId(groupId)
                    .setMembers(List.of( new GroupMember().setId(userId)).toArray(new GroupMember[0]));
            return rongCloud.group.dismiss(group);
        });
    }

    public Result quitGroup(String groupId, String groupName, List<String> memberIds) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(() -> {
            GroupModel group = new GroupModel().setId(groupId)
                    .setMembers(memberIds.stream().map(m -> new GroupMember().setId(m)).toList().toArray(new GroupMember[0]))
                    .setName(groupName);
            return rongCloud.group.quit(group);
        });
    }

    public void setMuteStatus(List<String> groupIds) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> rongCloud.group.ban.add(groupIds.toArray(new String[0])));
    }

    public void removeMuteStatus(List<String> groupIds) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> rongCloud.group.ban.remove(groupIds.toArray(new String[0])));
    }

    public void createUltraGroup(String userId, String groupId, String groupName) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> {
            UltraGroupModel model = new UltraGroupModel();
            model.setId(groupId);
            model.setName(groupName);
            model.setUserId(userId);
            return rongCloud.ultraGroup.create(model);
        });
    }

    public void dismissUltraGroup(String groupId) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> rongCloud.ultraGroup.dis(groupId));
    }

    public void joinUltraGroup(String groupId, String userId) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> {
            UltraGroupModel model = new UltraGroupModel();
            model.setId(groupId);
            model.setUserId(userId);
            return rongCloud.ultraGroup.join(model);
        });
    }

    public void quitUltraGroup(String groupId, String userId) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> {
            UltraGroupModel model = new UltraGroupModel();
            model.setId(groupId);
            model.setUserId(userId);
            return rongCloud.ultraGroup.quit(model);
        });
    }


    public void createUltraGroupChannel(String groupId, String channelId, Integer type) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> {
            UltraGroupModel model = new UltraGroupModel();
            model.setId(groupId);
            model.setBusChannel(channelId);
            model.setType(type);
            return rongCloud.ultraGroup.busChannel.add(model);
        });
    }



    public void ultragruopChannelChangeType(String groupId, String channelId, Integer type) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> {
            UltraGroupModel model = new UltraGroupModel();
            model.setId(groupId);
            model.setBusChannel(channelId);
            model.setType(type);
            return rongCloud.ultraGroup.busChannel.change(model);
        });
    }

    public Result ultraGroupChannelMemberAdd(String groupId, String channelId, List<String> members) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(() -> {
            UltraGroupModel model = new UltraGroupModel();
            model.setId(groupId);
            model.setBusChannel(channelId);
            model.setMembers(members.stream().map(m -> new UltraGroupMember().setId(m)).toList().toArray(new UltraGroupMember[0]));
            return rongCloud.ultraGroup.busChannel.privateUserAdd(model);
        });
    }

    public Result ultraGroupChannelMemberRemove(String groupId, String channelId, List<String> members) throws RCloudHttpException {
       return RCExecuteWrapper.executeSDK(() -> {
            UltraGroupModel model = new UltraGroupModel();
            model.setId(groupId);
            model.setBusChannel(channelId);
            model.setMembers(members.stream().map(m -> new UltraGroupMember().setId(m)).toList().toArray(new UltraGroupMember[0]));
            return rongCloud.ultraGroup.busChannel.privateUserRemove(model);
        });
    }


    public UserIdListModel ultraGroupChannelMemberQuery(String groupId, String channelId, Integer page, Integer pageSize) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(() -> rongCloud.ultraGroup.busChannel.privateUserGet(groupId, channelId, page, pageSize));
    }

    public void ultraGroupDeleteChannel(String groupId, String channelId) throws RCloudHttpException {
        RCExecuteWrapper.executeSDK(() -> rongCloud.ultraGroup.busChannel.remove( new UltraGroupModel().setId(groupId).setBusChannel(channelId)));
    }

    public void ugUserGroupAdd(String groupId, String userGroupId) throws RCloudHttpException {
        RCExecuteWrapper.executeHttp(() -> {
            String apiUrl = rongCloud.getConfig().getDomain() + ADD_USERGROUP;
            Map<String, Object> userGroup = new HashMap<>();
            userGroup.put("userGroupId", userGroupId);
            Map<String, Object> body = new HashMap<>();
            body.put("groupId", groupId);
            body.put("userGroups", List.of(userGroup));
            return httpService.postJson(apiUrl, JacksonUtil.toJson(body),
                    RongCloudApiUtil.signHeader(sealtalkConfig.getRongcloudAppKey(), sealtalkConfig.getRongcloudAppSecret()));
        });
    }

    public void ugUserGroupDel(String groupId, String userGroupId) throws RCloudHttpException {
        RCExecuteWrapper.executeHttp(() -> {
            String apiUrl = rongCloud.getConfig().getDomain() + DEL_USERGROUP;
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("groupId", groupId);
            body.add("userGroupIds", userGroupId);
            return httpService.postForm(apiUrl,body,RongCloudApiUtil.signHeader(sealtalkConfig.getRongcloudAppKey(), sealtalkConfig.getRongcloudAppSecret()));
        });
    }

    public void ugUserGroupMemberAdd(String groupId, String userGroupId, List<String> memberIds) throws RCloudHttpException {
        RCExecuteWrapper.executeHttp(() -> {
            String apiUrl = rongCloud.getConfig().getDomain() + USERGROUP_BIND_USER;
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("groupId", groupId);
            body.add("userGroupId", userGroupId);
            body.addAll("userIds", memberIds);
            return httpService.postForm(apiUrl,body,RongCloudApiUtil.signHeader(sealtalkConfig.getRongcloudAppKey(), sealtalkConfig.getRongcloudAppSecret()));
        });
    }

    public void ugUserGroupMemberDel(String groupId, String userGroupId, List<String> memberIds) throws RCloudHttpException {
        RCExecuteWrapper.executeHttp(() -> {
            String apiUrl = rongCloud.getConfig().getDomain() + USERGROUP_UNBIND_USER;
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("groupId", groupId);
            body.add("userGroupId", userGroupId);
            body.addAll("userIds", memberIds);
            return httpService.postForm(apiUrl,body,RongCloudApiUtil.signHeader(sealtalkConfig.getRongcloudAppKey(), sealtalkConfig.getRongcloudAppSecret()));
        });
    }

    public void ugChannelBindUserGroup(String groupId, String channelId, List<String> userGroupIds) throws RCloudHttpException {
        RCExecuteWrapper.executeHttp(() -> {
            String apiUrl = rongCloud.getConfig().getDomain() + CHANNEL_BING_USERGROUP;
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("groupId", groupId);
            body.add("busChannel", channelId);
            body.addAll("userGroupIds", userGroupIds);
            return httpService.postForm(apiUrl,body,RongCloudApiUtil.signHeader(sealtalkConfig.getRongcloudAppKey(), sealtalkConfig.getRongcloudAppSecret()));
        });
    }

    public void ugChannelUnBindUserGroup(String groupId, String channelId, List<String> userGroupIds) throws RCloudHttpException {
        RCExecuteWrapper.executeHttp(() -> {
            String apiUrl = rongCloud.getConfig().getDomain() + CHANNEL_UNBING_USERGROUP;
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("groupId", groupId);
            body.add("busChannel", channelId);
            body.addAll("userGroupIds", userGroupIds);
            return httpService.postForm(apiUrl,body,RongCloudApiUtil.signHeader(sealtalkConfig.getRongcloudAppKey(), sealtalkConfig.getRongcloudAppSecret()));
        });
    }


    public Result createBot(RCBotDTO rcBotDTO) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(() -> {
            String body = JacksonUtil.toJson(rcBotDTO);
            ChatbotInfoModel model = JacksonUtil.fromJson(body,ChatbotInfoModel.class);
            return rongCloud.chatbot.create(model);
        });
    }

    public Result delBot(String botId) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(() -> rongCloud.chatbot.delete(botId));

    }

    public void updateBot(RCBotDTO rcBotDTO) throws RCloudHttpException {
        if (rcBotDTO.getName() != null || rcBotDTO.getProfileUrl() != null || rcBotDTO.getType() != null) {
            ChatbotInfoModel tmp = new ChatbotInfoModel();
            tmp.setUserId(rcBotDTO.getUserId());
            tmp.setName(rcBotDTO.getName());
            tmp.setType(rcBotDTO.getType());
            tmp.setProfileUrl(rcBotDTO.getProfileUrl());
            RCExecuteWrapper.executeSDK(() -> rongCloud.chatbot.update(tmp));
        }
        if (rcBotDTO.getIntegrations() != null) {
            for (Integration integration : rcBotDTO.getIntegrations()) {
                RCExecuteWrapper.executeSDK(() -> {
                    String tmp = JacksonUtil.toJson(integration);
                    SetChatbotIntegration i = JacksonUtil.fromJson(tmp, SetChatbotIntegration.class);
                    i.setUserId(rcBotDTO.getUserId());
                    return rongCloud.chatbot.updateIntegration(i);
                });
            }
        }
    }

    public Result createAgent(AiUser aiUser, AiUserI18n aiUserI18n, String modelName, String promptMsg) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(() -> {
            Prompt prompt = new Prompt();
            prompt.setInstructions(promptMsg);
            prompt.setOpeningStatement(aiUserI18n.getOpeningRemark());

            ModelOptions modelOption = new ModelOptions();
            modelOption.setTemperature(1.0F);
            modelOption.setTopP(0.7F);

            Model model = new Model();
            model.setName(modelName);
            model.setOptions(modelOption);

            AgentConfig agentConfig = new AgentConfig();
            agentConfig.setModel(model);
            agentConfig.setPrompt(prompt);

            AgentModel agentModel = new AgentModel();
            agentModel.setAgentId(aiUser.getAiUserId());
            agentModel.setName(aiUserI18n.getNickname());
            agentModel.setDescription(aiUserI18n.getIntroduction());
            agentModel.setType("chat");
            agentModel.setStatus("active");
            agentModel.setAgentConfig(agentConfig);
            return rongCloud.agent.create(agentModel);
        });
    }


    public Result updateAgent(AiUser aiUser, AiUserI18n aiUserI18n, String modelName, String promptMsg) throws RCloudHttpException {

        return RCExecuteWrapper.executeSDK(() -> {
            Prompt prompt = new Prompt();
            prompt.setInstructions(promptMsg);
            prompt.setOpeningStatement(aiUserI18n.getOpeningRemark());

            ModelOptions modelOption = new ModelOptions();
            modelOption.setTemperature(1.0F);
            modelOption.setTopP(0.7F);

            Model model = new Model();
            model.setName(modelName);
            model.setOptions(modelOption);

            AgentConfig agentConfig = new AgentConfig();
            agentConfig.setModel(model);
            agentConfig.setPrompt(prompt);

            AgentModel agentModel = new AgentModel();
            agentModel.setAgentId(aiUser.getAiUserId());
            agentModel.setName(aiUserI18n.getNickname());
            agentModel.setDescription(aiUserI18n.getIntroduction());
            agentModel.setType("chat");
            agentModel.setStatus("active");
            agentModel.setAgentConfig(agentConfig);
            return rongCloud.agent.update(agentModel);
        });

    }


    public Result deleteAgent(String aiUserId) throws RCloudHttpException {
        return RCExecuteWrapper.executeSDK(() -> rongCloud.agent.delete(aiUserId));
    }


    public String agentChat(String userId, String agentId, String msgs) throws RCloudHttpException {
        ChatAgentResult result = RCExecuteWrapper.executeSDK(() -> {
            ChatModel chatModel = new ChatModel();
            chatModel.setMemory(false);
            chatModel.setAgentId(agentId);
            chatModel.setQuery(msgs);
            chatModel.setUser(userId);
            return rongCloud.agent.chat(chatModel);
        });
        return result.getAnswer();
    }

    public Result createBot_Agent(AiUser aiUser, AiUserI18n aiUserI18n, boolean likeHuman) throws RCloudHttpException {

        return RCExecuteWrapper.executeSDK(() -> {

            AgentModel agentModel = new AgentModel();
            agentModel.setAgentId(aiUser.getAiUserId());

            ChatbotIntegration chatbotIntegration = new ChatbotIntegration();
            chatbotIntegration.setType("rc_agentchat");
            chatbotIntegration.setObjectNames(List.of("RC:TxtMsg"));
            chatbotIntegration.setEvents(List.of("message:private"));
            chatbotIntegration.setStream(false);
            chatbotIntegration.setHandleResponse(true);
            chatbotIntegration.setAgent(agentModel);
            if (likeHuman){
                chatbotIntegration.setWaitForInputCompletion(true);
                chatbotIntegration.setGentleResponse(true);
            }

            ChatbotInfoModel chatBot = new ChatbotInfoModel();
            chatBot.setUserId(aiUser.getAiUserId());
            chatBot.setName(aiUserI18n.getNickname());
            chatBot.setType("BOT");
            chatBot.setProfileUrl(aiUser.getAvatar());
            chatBot.setIntegrations(List.of(chatbotIntegration));
            return rongCloud.chatbot.create(chatBot);
        });
    }

    public Result updateBot_Agent(AiUser aiUser, AiUserI18n aiUserI18n, boolean likeHuman) throws RCloudHttpException {

        return RCExecuteWrapper.executeSDK(() -> {

            AgentModel agentModel = new AgentModel();
            agentModel.setAgentId(aiUser.getAiUserId());

            ChatbotIntegration chatbotIntegration = new ChatbotIntegration();
            chatbotIntegration.setType("rc_agentchat");
            chatbotIntegration.setObjectNames(List.of("RC:TxtMsg"));
            chatbotIntegration.setEvents(List.of("message:private"));
            chatbotIntegration.setStream(false);
            chatbotIntegration.setHandleResponse(true);
            chatbotIntegration.setAgent(agentModel);
            if (likeHuman){
                chatbotIntegration.setWaitForInputCompletion(true);
                chatbotIntegration.setGentleResponse(true);
            }

            ChatbotInfoModel chatBot = new ChatbotInfoModel();
            chatBot.setUserId(aiUser.getAiUserId());
            chatBot.setName(aiUserI18n.getNickname());
            chatBot.setType("BOT");
            chatBot.setProfileUrl(aiUser.getAvatar());
            chatBot.setIntegrations(List.of(chatbotIntegration));
            return rongCloud.chatbot.update(chatBot);
        });
    }


    public JwtTokenResult getJwtToken(String userId) throws RCloudHttpException {
        return RCExecuteWrapper.executeHttp(() -> {
            String host = sealtalkConfig.getAiApiUrl();
            host = host.startsWith("http://") || host.startsWith("https://") ? host : "http://" + host;
            String requestUrl = host + "/jwt/getToken.json";
            Map<String, String> header = RongCloudApiUtil.signHeader(sealtalkConfig.getRongcloudAppKey(), sealtalkConfig.getRongcloudAppSecret());
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("userId", userId);
            String respBody = httpService.postForm(requestUrl, body, header);
            return JacksonUtil.fromJson(respBody, JwtTokenResult.class);
        });
    }

}

