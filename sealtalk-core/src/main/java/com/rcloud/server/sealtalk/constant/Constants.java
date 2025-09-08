package com.rcloud.server.sealtalk.constant;

public class Constants {


    public final static String REGION_NAME = "zh-CN";
    public static final String STRING_ADD = "+";

    public static final String ENV_DEV = "dev";

    public static final String DEFAULT_VERIFY_CODE = "9999";
    public static final String DEFAULT = "default";


    public static final String CONTACT_OPERATION_ACCEPT_RESPONSE = "AcceptResponse";

    public static final String CONTACT_OPERATION_REQUEST = "Request";


    public static final String CONVERSATION_TYPE_PRIVATE = "PRIVATE";
    public static final String CONVERSATION_TYPE_GROUP = "GROUP";

    public static final int GROUP_MAX_MEMBER_CNT = 500;
    public static final int MAX_USER_GROUP_OWN_COUNT = 500;
    public static final int MAX_USER_ULTRA_GROUP_OWN_COUNT = 20;
    public static final int MAX_USER_ULTRA_GROUP_CHANNEL_OWN_COUNT = 20;
    /**
     * 超级群下用户组最大数量
     */
    public static final int MAX_UG_USERGROUP_COUNT = 50;
    /**
     * 频道绑定的用户组最大数量
     */
    public static final int MAX_CHANNEL_USERGROUP_COUNT = 50;
    /**
     * 用户组成员数最大数量
     */
    public static final int MAX_USERGROUP_MEMBER_COUNT = 1000;


    public static final String SEPARATOR_ESCAPE = "\\|";
    public static final String SEPARATOR_NO = "|";


    //群组申请消息 GrpApplyMessage默认fromUserId标示
    public static final String GrpApplyMessage_fromUserId = "__group_apply__";

    /**
     * 发送群组通知时，一种默认的发送者ID，固定指定为__system__
     * /group/kick
     * /group/rename
     * /group/copy_group
     * /group/quit
     * /group/join
     * /group/dismiss
     * /group/creator
     * /group/transfer
     * /group/agree
     * /group/add
     * /group/remove_manager
     * /group/set_manager
     */
    public static final String GroupNotificationMessage_fromUserId = "__system__";


    public static final Integer CODE_OK = 200;
    public static final String DATE_FORMATR_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String ULTRA_GROUP_DEFAUT_CHANNEL_ID = "RCDefault";
    public static final String ULTRA_GROUP_DEFAUT_CHANNEL_NAME = "综合";

    public static final int TRUE = 1;
    public static final int FALSE = 0;

    public static final long ONE_SECONDS =  1000L;
    public static final long ONE_HOUR_MILLION_SECONDS = 60 * 60 * ONE_SECONDS;
    public static final long ONE_DAY_MILLION_SECONDS = 24 * ONE_HOUR_MILLION_SECONDS;

    public static final String HELLO_CONTENT = "欢迎体验融云 IM，应用内提供私聊、群聊沟通场景，支持文本、表情、图片、语音、视频、位置消息、实时音视频等丰富的沟通方式，快来体验吧。";


    public static final long ONE_MINUTES = 60 * ONE_SECONDS;
    public static final long TWO_MINUTES = 2 * ONE_MINUTES;

    public static final int BOT_CNT_LIMIT = 100;
    public static final String UG_BS_ID_PREFIX = "UGB";
    public static final String BOT_ID_PREFIX = "BOT";
    public static final String AI_USER_ID_PREFIX = "AI";


    public static final int ALL_AGENT_CNT_LIMIT = 2000;
    public static final int USER_AGENT_CNT_LIMIT = 20;


    public static final String SUGGESTION_AGENT_ID = "AI0000000000";


}
