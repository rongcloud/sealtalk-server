package com.rcloud.server.sealtalk.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * 获取服务单独的配置信息
 */
@Data
@ConfigurationProperties(prefix = "sealtalk-config")
@Component
public class SealtalkConfig {

    private String authCookieName;
    private String authCookieKey;
    private String authCookieMaxAge;
    private String authCookieDomain;

    private String configEnv;

    private String rongcloudAppKey;

    private String rongcloudAppSecret;

    private String rongcloudApiUrl;

    private String rongcloudDefaultPortraitUrl;         //默认头像地址

    private String qiniuAccessKey;

    private String qiniuSecretKey;

    private String qiniuBucketName;

    private String qiniuBucketDomain;

    private String n3dKey;

    private String[] corsHosts;

    private String aliSmsAccessKeyId;

    private String aliSmsAccessKeySecret;

    private String aiApiUrl;

    private String shumeiAccessKey;

    private String shumeiAppId;

    private String shumeiApiUrl;

    private String dbHost;

    private String dbUser;

    private String dbPassword;

    private String dbPort;

    private String dbName;

    private String aiAgentModel;

    private String adminAuthKey;

}
