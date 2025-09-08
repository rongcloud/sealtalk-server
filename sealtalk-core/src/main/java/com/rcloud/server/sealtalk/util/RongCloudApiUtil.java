package com.rcloud.server.sealtalk.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.codec.binary.Hex;

public class RongCloudApiUtil {


    public static Map<String, String> signHeader(String appKey, String appSecret){
        long timeStamp = System.currentTimeMillis();
        int nonce  = RandomUtil.randomBetween(100000, 999999);
        String signature = sign(appSecret,String.valueOf(nonce),String.valueOf(timeStamp));
        String uuid = UUID.randomUUID().toString().replaceAll("-","");
        Map<String, String> header =  new HashMap<>();
        header.put("App-Key",appKey);
        header.put("Nonce",String.valueOf(nonce));
        header.put("Timestamp",String.valueOf(timeStamp));
        header.put("Signature",signature);
        header.put("X-Request-Id",uuid);
        return header;
    }

    public static String sign(String secret,String nonce, String timestamp ){
        StringBuilder toSign = new StringBuilder(secret).append(nonce).append(
            timestamp);
        String sign = hexSHA1(toSign.toString());
        return sign;
    }

    public static String hexSHA1(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(value.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            return Hex.encodeHexString(digest);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }



}
