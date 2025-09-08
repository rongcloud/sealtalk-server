package com.rcloud.server.sealtalk.util;

import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.ErrorMsg;
import com.rcloud.server.sealtalk.exception.ParamException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

public class ValidateUtils {


    public static final int NICKNAME_MIN_LENGTH = 1;

    public static final int NICKNAME_MAX_LENGTH = 32;

    public static final int FRIEND_REQUEST_MESSAGE_MIN_LENGTH = 0;

    public static final int FRIEND_REQUEST_MESSAGE_MAX_LENGTH = 64;

    public static final int FRIEND_DISPLAY_NAME_MIN_LENGTH = 1;

    public static final int FRIEND_DISPLAY_NAME_MAX_LENGTH = 32;

    public static final int GROUP_NAME_MIN_LENGTH = 2;

    public static final int GROUP_NAME_MAX_LENGTH = 32;

    public static final int GROUP_BULLETIN_MAX_LENGTH = 1024;

    public static final int PORTRAIT_URI_MIN_LENGTH = 12;

    public static final int PORTRAIT_URI_MAX_LENGTH = 256;

    public static final int GROUP_MEMBER_DISPLAY_NAME_MAX_LENGTH = 32;

    public static final int DEFAULT_MAX_GROUP_MEMBER_COUNT = 500;

    public static final int MAX_USER_GROUP_OWN_COUNT = 500;

    public static final int DEFAULT_MAX_ULTRA_GROUP_MEMBER_COUNT = 100;

    public static final int ULTRA_GROUP_SUMMARY_MAX_LENGTH = 2000;
    public static final int ULTRA_GROUP_CHANNEL_NAME_MAX_LENGTH = 32;

    private static final String URL_REGEX = "(https?)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]";

    public static void notNull(Object o, String name) throws ParamException {
        if (o == null) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_REQUIRED, name);
        }
    }

    public static void notBlank(String param, String name) throws ParamException {
        if (StringUtils.isBlank(param)) {
            String msg = ErrorMsg.formatMsg(ErrorMsg.PARAM_REQUIRED, name);
            throw new ParamException(ErrorCode.PARAM_ERROR.getErrorCode(), msg);
        }
    }

    public static void checkLength(String[] arr, int minLength, int maxLength, String name) throws ParamException {
        if (arr == null) {
            if (minLength < 1) {
                return;
            }
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_LENGTH, name, minLength, maxLength);
        }
        if (arr.length < minLength || arr.length > maxLength) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_LENGTH, name, minLength, maxLength);
        }
    }

    public static <T> void checkLength(List<T> arr, int minLength, int maxLength, String name) throws ParamException {
        if (arr == null) {
            if (minLength < 1) {
                return;
            }
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_LENGTH, name, minLength, maxLength);
        }
        if (arr.size() < minLength || arr.size() > maxLength) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_LENGTH, name, minLength, maxLength);
        }
    }

    public static void checkLength(String str, int minLength, int maxLength, String name) throws ParamException {
        if (str == null) {
            if (minLength < 1) {
                return;
            }
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_LENGTH, name, minLength, maxLength);
        }
        if (str.length() < minLength || str.length() > maxLength) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_LENGTH, name, minLength, maxLength);
        }
    }

    public static void checkNumberStr(String param, String name) throws ParamException {
        notBlank(param, name);
        if (!param.matches("\\d+")) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_ILLEGAL, name);
        }
    }

    public static void checkURLFormat(String portraitUri, String name) throws ParamException {
        if (!Pattern.matches(URL_REGEX, portraitUri)) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_ILLEGAL, name);
        }
    }


    public static void checkBooleanNum(Integer num, String name, boolean notNull) throws ParamException {
        if (notNull){
            notNull(num, name);
        }
        if (num == null) {
            return;
        }
        if (!num.equals(Constants.TRUE) && !num.equals(Constants.FALSE)) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_ILLEGAL, name);
        }
    }

    public static void check_boolean_num(Integer num, String name) throws ParamException {
        checkBooleanNum(num, name, true);
    }

    public static void checkGender(String gender, String name) throws ParamException {
        if (StringUtils.isBlank(gender)) {
            return;
        }
        if (!"male".equals(gender) && !"female".equals(gender)) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_ILLEGAL, name);
        }
    }

    public static void checkStAccount(String stAccount, String name) throws ParamException {
        if (StringUtils.isBlank(stAccount)) {
            return;
        }
        if (stAccount.length() < 6 || stAccount.length() > 20) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_ILLEGAL, name);
        }
        String regex = "^[a-zA-Z][a-zA-Z0-9_-]*$";
        if (!Pattern.matches(regex, stAccount)) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_ILLEGAL, name);
        }
    }



    /**
     * 判断 value是否在rangeList 区间范围内
     */
    public static <T> void valueOf(T value, List<T> rangeList, String name) throws ParamException {
        if (!rangeList.contains(value)){
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.PARAM_ILLEGAL, name);
        }
    }

}
