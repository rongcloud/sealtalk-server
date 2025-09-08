package com.rcloud.server.sealtalk.sms.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.rcloud.server.sealtalk.configuration.SealtalkConfig;
import com.rcloud.server.sealtalk.sms.SmsService;
import com.rcloud.server.sealtalk.sms.SmsTemplateVO;

import java.util.HashMap;
import java.util.Map;

import com.rcloud.server.sealtalk.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AliSmsService implements SmsService {


    @Autowired
    protected SealtalkConfig sealtalkConfig;

    @Override
    public String sendSms(String region, String phone, SmsTemplateVO template) throws Exception {
        Config config = new Config();
        config.setAccessKeyId(sealtalkConfig.getAliSmsAccessKeyId());
        config.setAccessKeySecret(sealtalkConfig.getAliSmsAccessKeySecret());
        String code = String.valueOf(code());
        Map<String, String> param = new HashMap<>();
        param.put("p1", code);
        SendSmsRequest request = new SendSmsRequest();
        request.setPhoneNumbers(region + phone);
        request.setSignName(template.getSignName());
        request.setTemplateCode(template.getTemplateCode());
        request.setTemplateParam(JacksonUtil.toJson(param));
        try {
            log.info("ali sms send:【{}】【{}】【{}】", region, phone, code);
            Client client = new Client(config);
            SendSmsResponse response = client.sendSms(request);
            log.info("ali sms send:【{}】", JacksonUtil.toJson(response.getBody()));
        } catch (Exception e) {
            log.error("短信发送失败:", e);
            return null;
        }
        return code;
    }
}
