package com.rcloud.server.sealtalk.constant;

/**
 *
 */
public class ErrorMsg {

    public static final String PARAM_REQUIRED = "The parameter %s is a required field.";
    public static final String PARAM_ILLEGAL = "Illegal argument %s";
    public static final String PARAM_LENGTH = "The length of the parameter %s should be between %s and %s.";

    public static final String NUM_EXCEED_LIMIT ="The number of %s exceeds the limit.";
    public static final String NOT_EXIST ="The %s does not exist.";
    public static final String ONLY_FRIEND ="Only friends can perform this action.";
    public static final String USER_JOIN_GROUP_OVER ="The number of groups the user has joined exceeds the limit.";
    public static final String GROUP_MEMBER_OVER ="The number of group members exceeds the limit.";
    public static final String UG_CHANNEL_OVER ="The number of group channel exceeds the limit.";
    public static final String UG_USER_GROUP_OVER ="The number of group usergroup exceeds the limit.";
    public static final String UG_CHANNEL_BIND_USERGROUP_OVER ="The number of user groups bound to the channel exceeds the limit.";
    public static final String UG_USER_GROUP_MEMBER_OVER ="The number of group usergroup members exceeds the limit.";
    public static final String ONLY_GROUP_MEMBER ="Only group members can perform this action.";
    public static final String ONLY_GROUP_CREATOR ="Only the group creator can perform this action.";
    public static final String ONLY_GROUP_MANAGER ="Only the group creator or group administrator can perform this action.";
    public static final String MUST_NOT_GROUP_CREATOR ="The group creator cannot perform this action.";
    public static final String MUST_NOT_SELF ="Cannot perform this action on yourself.";

    public static String formatMsg(String template, Object... param) {
        return String.format(template, param);
    }

}
