package com.rcloud.server.sealtalk.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

    public static String format(long time) {
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
