package com.rcloud.server.sealtalk.service;

import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ConversationType;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.ErrorMsg;
import com.rcloud.server.sealtalk.constant.FriendShipStatus;
import com.rcloud.server.sealtalk.dao.ScreenStatusesMapper;
import com.rcloud.server.sealtalk.dto.SendMessageContent;
import com.rcloud.server.sealtalk.entity.Friendships;
import com.rcloud.server.sealtalk.entity.GroupMembers;
import com.rcloud.server.sealtalk.entity.ScreenStatuses;
import com.rcloud.server.sealtalk.entity.Users;
import com.rcloud.server.sealtalk.exception.RCloudHttpException;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.rongcloud.RongCloudClient;
import com.rcloud.server.sealtalk.rongcloud.message.CustomerConNtfMessage;
import com.rcloud.server.sealtalk.util.N3d;
import com.rcloud.server.sealtalk.util.ThreadFactoryUtil;
import io.rong.messages.ImgTextMessage;
import io.rong.models.message.GroupMessage;
import io.rong.models.message.PrivateMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MiscService {

    @Autowired
    private RongCloudClient rongCloudClient;

    @Autowired
    private FriendshipsService friendshipsService;

    @Autowired
    private GroupsService groupsService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private ScreenStatusesMapper screenStatusesMapper;


    /**
     * 调用Server api发送消息,目前只支持ImgTextMessage 消息类型
     */
    public void sendMessage(Integer currentUserId, String conversationType, Integer targetId, String objectName, SendMessageContent sendMessageContent, String pushContent, String encodedTargetId) throws ServiceException {
        if (Constants.CONVERSATION_TYPE_PRIVATE.equals(conversationType)) {
            //如果会话类型是单聊
            Friendships friendships = friendshipsService.queryByUserIdAndFriendId(currentUserId, targetId);
            if (friendships == null || !FriendShipStatus.AGREED.getStatus().equals(friendships.getStatus())) {
                throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_FRIEND);
            }
            ThreadFactoryUtil.ofVirtual(()->{
                ImgTextMessage imgTextMessage = new ImgTextMessage(sendMessageContent.getContent(),sendMessageContent.getExtra(),sendMessageContent.getTitle(),sendMessageContent.getImageUri(),sendMessageContent.getUrl());
                //调用融云接口发送单聊消息
                PrivateMessage privateMessage = new PrivateMessage()
                        .setSenderId(N3d.encode(currentUserId))
                        .setTargetId(new String[]{encodedTargetId})
                        .setObjectName(objectName)
                        .setContent(imgTextMessage)
                        .setPushContent(pushContent);
                rongCloudClient.sendPrivateMessage(privateMessage);
            });
            return;
        }

        if (Constants.CONVERSATION_TYPE_GROUP.equals(conversationType)) {
            //如果会话类型是群组
            GroupMembers groupMembers = groupsService.queryGroupMember(targetId, currentUserId);

            if (groupMembers == null) {
                throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), ErrorMsg.ONLY_GROUP_MEMBER);
            }
            ThreadFactoryUtil.ofVirtual(() -> {
                ImgTextMessage imgTextMessage = new ImgTextMessage(sendMessageContent.getContent(), sendMessageContent.getExtra(), sendMessageContent.getTitle(), sendMessageContent.getImageUri(), sendMessageContent.getUrl());
                GroupMessage groupMessage = new GroupMessage();
                groupMessage.setSenderId(N3d.encode(currentUserId))
                        .setTargetId(new String[]{encodedTargetId})
                        .setObjectName(objectName)
                        .setContent(imgTextMessage)
                        .setPushContent(pushContent);
                //发送群组消息
                rongCloudClient.sendGroupMessage(groupMessage);
            });
            return;
        }
        throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), "Unsupported type.");
    }

    /**
     * 设置截屏通知状态
     */
    public void setScreenCapture(Integer currentUserId, Integer targetId, Integer conversationType, Integer noticeStatus) throws Exception {
        String operateId = String.valueOf(targetId);
        String statusContent = noticeStatus == 0 ? "closeScreenNtf" : "openScreenNtf";
        if (conversationType == 1) {
            operateId = currentUserId < targetId ? currentUserId + "_" + targetId : targetId + "_" + currentUserId;
        }
        Users users = usersService.queryById(currentUserId);
        if (users != null) {
            throw ServiceException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "user");
        }
        var screenStatus = new ScreenStatuses();
        screenStatus.setOperateId(operateId);
        screenStatus.setConversationType(conversationType);
        screenStatus.setStatus(noticeStatus);
        screenStatusesMapper.upsert(screenStatus);

        //发送截屏消息
        sendScreenMsg0(currentUserId, targetId, conversationType, statusContent);

    }

    /**
     * 发送截屏通知消息
     *
     */
    private void sendScreenMsg0(Integer currentUserId, Integer targetId, Integer conversationType, String operation) throws ServiceException, RCloudHttpException {

        String encodeUserId = N3d.encode(currentUserId);
        String encodeTargetId = N3d.encode(targetId);
        CustomerConNtfMessage customerConNtfMessage = new CustomerConNtfMessage();
        customerConNtfMessage.setOperatorUserId(encodeUserId);
        customerConNtfMessage.setOperation(operation);

        if (ConversationType.PRIVATE.getCode().equals(conversationType)) {
            PrivateMessage privateMessage = new PrivateMessage()
                    .setSenderId(encodeUserId)
                    .setTargetId(new String[]{encodeTargetId})
                    .setObjectName(customerConNtfMessage.getType())
                    .setContent(customerConNtfMessage);
            rongCloudClient.sendPrivateMessage(privateMessage);
        } else if (ConversationType.GROUP.getCode().equals(conversationType)) {
            GroupMessage groupMessage = new GroupMessage();
            groupMessage.setTargetId(new String[]{encodeTargetId});
            groupMessage.setSenderId(encodeUserId);
            groupMessage.setObjectName(customerConNtfMessage.getType());
            groupMessage.setContent(customerConNtfMessage);
            groupMessage.setIsIncludeSender(1);
            rongCloudClient.sendGroupMessage(groupMessage);
        }
    }

    /**
     * 获取截屏通知状态
     */
    public ScreenStatuses getScreenCapture(Integer currentUserId, Integer targetId, Integer conversationType) {
        String operateId = String.valueOf(targetId);
        if (conversationType == 1) {
            operateId = currentUserId < targetId ? currentUserId + "_" + targetId : targetId + "_" + currentUserId;
        }
        return screenStatusesMapper.selectByOperateIdAndConversationType(operateId,conversationType);
    }


    /**
     * 发送截屏消息
     *
     */
    public void sendScreenCaptureMsg(Integer currentUserId, Integer targetId, Integer conversationType) throws Exception {
        sendScreenMsg0(currentUserId, targetId, conversationType, "sendScreenNtf");
    }
}
