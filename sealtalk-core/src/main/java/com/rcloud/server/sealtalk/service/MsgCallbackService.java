package com.rcloud.server.sealtalk.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.exception.RCloudHttpException;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.param.MsgRouteParam;
import com.rcloud.server.sealtalk.param.MsgRouteParam.ChannelType;
import com.rcloud.server.sealtalk.rongcloud.RongCloudClient;
import io.rong.messages.InfoNtfMessage;
import io.rong.models.message.ChatroomMessage;
import io.rong.models.message.GroupMessage;
import io.rong.models.message.PrivateMessage;
import io.rong.models.message.UltraGroupMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class MsgCallbackService {

    private static final String RISK_STATUS = "PASS";
    private static final String STRATEGY = "empty";
    private static final String SM_STRATEGY = "sm_empty";
    private static final Set<String> WARNING_OBJECT_NAME ;
    static {
        WARNING_OBJECT_NAME = new HashSet<>();
        WARNING_OBJECT_NAME.add("RC:TxtMsg");
        WARNING_OBJECT_NAME.add("RC:HQVCMsg");
        WARNING_OBJECT_NAME.add("RC:ImgMsg");
        WARNING_OBJECT_NAME.add("RC:GIFMsg");
        WARNING_OBJECT_NAME.add("RC:FileMsg");
        WARNING_OBJECT_NAME.add("RC:LBSMsg");
        WARNING_OBJECT_NAME.add("RC:SightMsg");
        WARNING_OBJECT_NAME.add("RC:ReferenceMsg");
        WARNING_OBJECT_NAME.add("RC:CombineMsg");
    }

    @Autowired
    private RongCloudClient rongCloudClient;
    @Autowired
    private UsersService usersService;


    private static final Cache<String, Long> WARNING_MSG_CACHE = Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();


    public void sendWarningNotify(MsgRouteParam msgRoute) throws Exception {
        if (!WARNING_OBJECT_NAME.contains(msgRoute.getObjectName())){
            return;
        }
        String relationId = relationId(msgRoute);
        if (relationId == null){
            return;
        }
        long lastWarnTime = WARNING_MSG_CACHE.get(relationId, k -> 0L);
        if (System.currentTimeMillis() - lastWarnTime <= 30 * Constants.ONE_SECONDS){
            return;
        }
        WARNING_MSG_CACHE.put(relationId,System.currentTimeMillis());
        InfoNtfMessage message = new InfoNtfMessage("请在聊天中注意人身财产安全,谨防诈骗",null);
        if (ChannelType.PERSON == msgRoute.getCType()) {
            PrivateMessage privateMessage = new PrivateMessage()
                    .setSenderId(msgRoute.getFromUserId())
                    .setTargetId(new String[]{msgRoute.getToUserId()})
                    .setObjectName(message.getType())
                    .setContent(message)
                    .setIsIncludeSender(0)
                    .setIsPersisted(0)
                    .setIsCounted(0);
            rongCloudClient.sendPrivateMessage(privateMessage);
            return;
        }

        if (ChannelType.GROUP == msgRoute.getCType()){
            GroupMessage groupMessage = new GroupMessage()
                    .setSenderId(msgRoute.getFromUserId())
                    .setTargetId(new String[]{msgRoute.getToUserId()})
                    .setObjectName(message.getType())
                    .setContent(message).setIsIncludeSender(0).setIsPersisted(0);
            rongCloudClient.sendGroupMessage(groupMessage);
            return;
        }
        if (ChannelType.TEMPGROUP == msgRoute.getCType()){
            ChatroomMessage chatroomMessage = new ChatroomMessage()
                    .setSenderId(msgRoute.getFromUserId())
                    .setTargetId(new String[]{msgRoute.getToUserId()})
                    .setObjectName(message.getType())
                    .setContent(message);
            rongCloudClient.sendChatroomMessage(chatroomMessage);
            return;
        }
        //没升级sdk,没有超级群相关方法,先这么用吧
        if (ChannelType.ULTRAGROUP == msgRoute.getCType()){
            UltraGroupMessage ultraGroupMessage = new UltraGroupMessage();
            ultraGroupMessage.setSenderId(msgRoute.getFromUserId());
            ultraGroupMessage.setTargetId(new String[]{msgRoute.getToUserId()});
            ultraGroupMessage.setContent(message);
            ultraGroupMessage.setIsPersisted(0);
            ultraGroupMessage.setObjectName(message.getType());
            ultraGroupMessage.setIsCounted(0);
            ultraGroupMessage.setBusChannel(msgRoute.getBusChannel());
            rongCloudClient.sendUltraGroupMessage(ultraGroupMessage);
            return;
        }
        log.warn("unknown warning msg type: {}", msgRoute.getChannelType());
    }


    private String relationId(MsgRouteParam msgRoute){
        if (ChannelType.PERSON == msgRoute.getCType()) {
            return msgRoute.getChannelType() + "_" + msgRoute.getFromUserId() + "_" + msgRoute.getToUserId();
        }
        if (ChannelType.GROUP == msgRoute.getCType()
                || ChannelType.TEMPGROUP == msgRoute.getCType()
                || ChannelType.ULTRAGROUP == msgRoute.getCType()) {
            return msgRoute.getChannelType() + "_"  + msgRoute.getToUserId() +"_"+ msgRoute.getBusChannel();
        }
        return null;
    }
}
