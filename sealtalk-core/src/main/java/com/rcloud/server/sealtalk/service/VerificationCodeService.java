package com.rcloud.server.sealtalk.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.rcloud.server.sealtalk.configuration.SealtalkConfig;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.ErrorMsg;
import com.rcloud.server.sealtalk.constant.WhiteListTypeEnum;
import com.rcloud.server.sealtalk.dao.VerificationCodesMapper;
import com.rcloud.server.sealtalk.dao.VerificationViolationsMapper;
import com.rcloud.server.sealtalk.entity.VerificationCodes;
import com.rcloud.server.sealtalk.entity.VerificationViolations;
import com.rcloud.server.sealtalk.exception.ParamException;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.model.dto.PicCodeDTO;
import com.rcloud.server.sealtalk.sms.SmsService;
import com.rcloud.server.sealtalk.sms.SmsTemplateVO;
import com.rcloud.server.sealtalk.util.JacksonUtil;
import com.rcloud.server.sealtalk.util.MiscUtils;
import com.rcloud.server.sealtalk.util.RandomUtil;
import com.wf.captcha.SpecCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VerificationCodeService implements InitializingBean {

    @Autowired
    private VerificationCodesMapper verificationCodesMapper;
    @Autowired
    private VerificationViolationsMapper verificationViolationsMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("classpath:sms_template.json")
    private Resource smsTemplateResource;

    @Autowired
    private SealtalkConfig sealtalkConfig;

    @Autowired
    private WhiteListService whiteList;

    private Map<String, Pair<SmsTemplateVO, SmsService>> smsServiceMap = new HashMap<>();


    private final static Cache<String, String> PIC_CODE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    private final static Cache<String, AtomicInteger> SMS_CODE_FAIL_CACHE = CacheBuilder.newBuilder()
            .maximumSize(100000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Override
    public void afterPropertiesSet() throws Exception {
        String jsonData = IOUtils.toString(smsTemplateResource.getInputStream(), StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        List<SmsTemplateVO> smsTemplateVOList = objectMapper.readValue(jsonData, new TypeReference<List<SmsTemplateVO>>() {});
        log.info("INIT SMS TEMPLATE: {}", JacksonUtil.toJson(smsTemplateVOList));
        Map<String, SmsService> nameMap = applicationContext.getBeansOfType(SmsService.class)
                .values()
                .stream()
                .collect(Collectors.toMap(s -> s.getClass().getSimpleName(), v -> v, (v1, v2) -> v2));
        for (SmsTemplateVO templateVO : smsTemplateVOList){
            SmsService service = nameMap.get(templateVO.getService());
            if (service == null) {
                continue;
            }
            Pair<SmsTemplateVO, SmsService> value = Pair.of(templateVO,service);
            String[] regions = templateVO.getRegion().split(",");
            String[] devices = templateVO.getDevice().split(",");
            for (String region : regions){
                if (StringUtils.isBlank(region)){
                    continue;
                }
                for (String device : devices){
                    if (StringUtils.isBlank(device)){
                        continue;
                    }
                    String cacheKey = cacheKey(region,device);
                    smsServiceMap.put(cacheKey,value);
                }
            }
        }
    }


    private String cacheKey(String region, String device) {
        return region + "_" + device;
    }

    /**
     * 生成图片验证码
     */
    public PicCodeDTO pictureCode() {
        String picCodeId = RandomUtil.uuid();
        SpecCaptcha captcha = new SpecCaptcha();
        captcha.setLen(4);
        String picCode = captcha.text();
        String picBase64 = captcha.toBase64("");
        log.info("picture picCodeId:{}, code:{}", picCodeId, picCode);
        PIC_CODE_CACHE.put(picCodeId, picCode);
        PicCodeDTO dto = new PicCodeDTO();
        dto.setPicCodeId(picCodeId);
        dto.setPicCode(picBase64);
        return dto;
    }


    /**
     * 验证图片验证码
     */
    public boolean verifyPicCode(String picCodeId, String code) {
//        if (StringUtils.isBlank(picCodeId) || StringUtils.isBlank(code)) {
//            return true;
//        }
        String codeValue = PIC_CODE_CACHE.getIfPresent(picCodeId);
        PIC_CODE_CACHE.invalidate(picCodeId);
        return codeValue != null && codeValue.equalsIgnoreCase(code);
    }


    /**
     * 向手机发送验证码
     */
    public void sendSmsCode(String region, String phone, String device, String ip) throws Exception {
        log.info("send code. region:[{}] phone:[{}] ip:[{}] ", region, phone, ip);
        if (Constants.ENV_DEV.equals(sealtalkConfig.getConfigEnv())) {
            return;
        }
        region = MiscUtils.removeRegionPrefix(region);

        //检查并更新发送频率
        checkAndUpdateRateLimit(region, phone, ip);

        //发送短信
        String code ;
        if (whiteList.whiteCheck(region, phone, WhiteListTypeEnum.SMS)) {
            //白名单用户不发短信
            code = phone.length() > 6 ? phone.substring(phone.length() - 6) : phone;
        }else{
            Pair<SmsTemplateVO, SmsService> servicePair = chooseSmsService(region, device);
            code = servicePair.getRight().sendSms(region, phone, servicePair.getLeft());
        }

        var vCode = new VerificationCodes();
        vCode.setRegion(region);
        vCode.setPhone(phone);
        vCode.setSessionId(code);
        vCode.setToken(UUID.randomUUID().toString());
        vCode.setCreatedAt(new Date());
        vCode.setUpdatedAt(new Date());
        verificationCodesMapper.saveOrUpdate(vCode);
    }



    public boolean verifySmsCode(String region, String phone, String code) throws Exception {

        //如果是开发环境，且验证码是9999-》验证通过
        if (Constants.ENV_DEV.equals(sealtalkConfig.getConfigEnv()) && Constants.DEFAULT_VERIFY_CODE.equals(code)) {
            return true;
        }
        var verificationCodes = verificationCodesMapper.selectByRegionAndPhone(region, phone);
        //判断验证码记录是否存在
        if (verificationCodes == null) {
            throw ParamException.buildError(ErrorCode.PARAM_ERROR.getErrorCode(), ErrorMsg.NOT_EXIST, "verify code");
        }

        //验证码是否过期
        long updateTime = verificationCodes.getUpdatedAt().getTime();
        if (System.currentTimeMillis() - updateTime > Constants.TWO_MINUTES){
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), "verify code expired.");
        }

        //判断是否暴力破解
        String key = String.format("%s_%s", region, phone);
        AtomicInteger count = SMS_CODE_FAIL_CACHE.getIfPresent(key);
        //是否正在暴力破解
        boolean bruteforceFlag = count != null && count.get() >= 10;

        //判断验证码是否正确
        if (!bruteforceFlag && code.equalsIgnoreCase(verificationCodes.getSessionId())) {
            return true;
        } else {
            if (bruteforceFlag) {
                log.error("code auth fail count too much ");
            }
            count = SMS_CODE_FAIL_CACHE.get(key, AtomicInteger::new);
            count.incrementAndGet();
            throw new ServiceException(ErrorCode.SERVICE_ERROR.getErrorCode(), "fail too many.");
        }
    }





    private void checkAndUpdateRateLimit(String region, String phone, String ip) throws ServiceException {
        // 获取最近一次发送验证码记录
        var verificationCodes = verificationCodesMapper.selectByRegionAndPhone(region, phone);
        if (verificationCodes != null) {
            //1分钟内不能再发送
            long lastTime = verificationCodes.getUpdatedAt().getTime();
            if (System.currentTimeMillis() - lastTime < 60 * 1000L) {
                throw new ServiceException(ErrorCode.TOO_MANY_REQUEST.getErrorCode(), ErrorCode.TOO_MANY_REQUEST.getErrorMessage());
            }
        }

        var ipViolation = verificationViolationsMapper.selectByPrimaryKey(ip);
        //同一个ip。1小时内超过100次,不允许发送
        if (ipViolation != null && (System.currentTimeMillis() - ipViolation.getTime().getTime() < 60 * 60 * 1000L) && ipViolation.getCount() >= 100) {
            throw new ServiceException(ErrorCode.TOO_MANY_REQUEST.getErrorCode(), ErrorCode.TOO_MANY_REQUEST.getErrorMessage());
        }

        //记录或者更新ip的请求频率
        var newIpViolation = new VerificationViolations();
        newIpViolation.setIp(ip);
        if (ipViolation == null){
            newIpViolation.setTime(new Date());
            newIpViolation.setCount(1);
            verificationViolationsMapper.insert(newIpViolation);
        }else{
            if (System.currentTimeMillis() - ipViolation.getTime().getTime() >= 60 * 60 * 1000L){
                newIpViolation.setTime(new Date());
                newIpViolation.setCount(1);
            }else{
                newIpViolation.setCount(ipViolation.getCount() + 1);
            }
            verificationViolationsMapper.updateByPrimaryKey(newIpViolation);
        }
    }


    private Pair<SmsTemplateVO, SmsService> chooseSmsService(String region, String device) {
        String cacheKey = cacheKey(region, device);
        Pair<SmsTemplateVO, SmsService> servicePair = smsServiceMap.get(cacheKey);
        if (servicePair != null) {
            return servicePair;
        }
        cacheKey = cacheKey(region, Constants.DEFAULT);
        servicePair = smsServiceMap.get(cacheKey);
        if (servicePair != null) {
            return servicePair;
        }
        cacheKey = cacheKey(Constants.DEFAULT,Constants.DEFAULT);
        servicePair = smsServiceMap.get(cacheKey);
        return servicePair;
    }



}
