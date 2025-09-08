package com.rcloud.server.sealtalk.util;

import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;

@Slf4j
public class MiscUtils {


    /**
     * 地区去掉前缀 "+"
     *
     * @param region +86
     * @return 86
     */
    public static String removeRegionPrefix(String region) {
        if (region.startsWith(Constants.STRING_ADD)) {
            region = region.substring(1);
        }
        return region;
    }



    /**
     * 文本xss处理
     *
     * @param str
     * @param maxLength
     * @return
     */
    public static String xss(String str, int maxLength) {
        String result = "";
        if (StringUtils.isEmpty(str)) {
            return result;
        }
        result = StringEscapeUtils.escapeHtml4(str);
        if (result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }
        return result;
    }

    public static String xss_null(String str, int maxLength) {
        if (StringUtils.isBlank(str)) {
            return str;
        }
        String result = StringEscapeUtils.escapeHtml4(str);
        if (result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }
        return result;
    }

    public static List<String> batchEncodeIds(Integer[] ids) throws ServiceException {
        List<String> idStrs = new ArrayList<>();
        for (Integer i : ids) {
            idStrs.add(N3d.encode(i));
        }
        return idStrs;
    }

    public static List<String> batchEncodeIds(List<Integer> ids) throws ServiceException {
        List<String> idStrs = new ArrayList<>();
        for (Integer i : ids) {
            idStrs.add(N3d.encode(i));
        }
        return idStrs;
    }


    public static List<String> batchEncodeLongIds(List<Long> ids) throws ServiceException {
        List<String> idStrs = new ArrayList<>();
        for (Long i : ids) {
            idStrs.add(N3d.encode(i));
        }
        return idStrs;
    }


    public static List<Integer> batchDecodeIds(String[] ids) throws ServiceException {
        List<Integer> idStrs = new ArrayList<>();
        for (String i : ids) {
            idStrs.add(N3d.decode(i));
        }
        return idStrs;
    }

    public static List<Integer> batchDecodeIds(List<String> ids) throws ServiceException {
        List<Integer> idStrs = new ArrayList<>();
        for (String i : ids) {
            idStrs.add(N3d.decode(i));
        }
        return idStrs;
    }

    public static List<Long> batchDecodeLongIds(List<String> ids) throws ServiceException {
        List<Long> idStrs = new ArrayList<>();
        for (String i : ids) {
            idStrs.add(N3d.decode2Long(i));
        }
        return idStrs;
    }

    public static boolean isNumberStr(String str) {
        return str.matches("\\d+");
    }

}
