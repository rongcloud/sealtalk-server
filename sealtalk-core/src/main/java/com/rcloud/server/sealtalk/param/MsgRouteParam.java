package com.rcloud.server.sealtalk.param;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * @author jianzheng.li
 */
@Data
@NoArgsConstructor
public class MsgRouteParam {
    private String fromUserId;
    private String toUserId;
    private String objectName;
    private String busChannel;
    private String channelType;
    private String content;
    private String msgTimestamp;
    private String msgUID;
    private String originalMsgUID;
    private Integer sensitiveType;
    private String source;
    private List<String> groupUserIds;

    private ChannelType cType;

    public MsgRouteParam init(HttpServletRequest request) {
        this.fromUserId = request.getParameter("fromUserId");
        this.toUserId = request.getParameter("toUserId");
        this.objectName = request.getParameter("objectName");
        this.busChannel = request.getParameter("busChannel");
        this.channelType = request.getParameter("channelType");
        this.cType = channelType(this.channelType);
        this.content = request.getParameter("content");
        this.msgTimestamp = request.getParameter("msgTimestamp");
        this.msgUID = request.getParameter("msgUID");
        this.originalMsgUID = request.getParameter("originalMsgUID");
        String sensitiveTypeParam = request.getParameter("sensitiveType");
        this.sensitiveType = StringUtils.isBlank(sensitiveTypeParam) ? null : Integer.parseInt(sensitiveTypeParam);
        this.source = request.getParameter("source");
        String[] groupUserIdsParam = request.getParameterValues("groupUserIds");
        this.groupUserIds = groupUserIdsParam != null && groupUserIdsParam.length > 0 ? Arrays.asList(groupUserIdsParam) : null;
        return this;
    }

    private static ChannelType channelType(String channelType) {
        return Stream.of(ChannelType.values()).filter(c -> c.name().equals(channelType))
            .findFirst().orElse(null);
    }

    public enum ChannelType {
        /**
         * 单聊
         */
        PERSON,
        /**
         * 讨论组
         */
        PERSONS,
        /**
         * 群聊
         */
        GROUP,
        /**
         * 聊天室
         */
        TEMPGROUP,
        /**
         * 客服
         */
        CUSTOMERSERVICE,
        /**
         * 系统通知
         */
        NOTIFY,
        MC, MP,
        /**
         * 超级群
         */
        ULTRAGROUP

    }

}
