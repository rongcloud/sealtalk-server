package com.rcloud.server.sealtalk.sms;

import com.rcloud.server.sealtalk.util.RandomUtil;

public interface SmsService {


    default int code() {
        return RandomUtil.randomBetween(100000, 999999);
    }

    String sendSms(String region, String phone, SmsTemplateVO template) throws Exception;

}
