package com.rcloud.server.sealtalk.util;

import java.util.Random;
import java.util.UUID;

public class RandomUtil {

    private static Random random = new Random();

    /**
     * 生成随机数，区间[min,max]
     *
     * @param min
     * @param max
     * @return
     */
    public static int randomBetween(Integer min, Integer max) {
        return random.nextInt(max - min + 1) + min;
    }

    /**
     * 获取uuid
     */
    public static String uuid(){
        return UUID.randomUUID().toString().replace("-","");
    }


}
