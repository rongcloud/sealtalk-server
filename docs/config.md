# 配置说明

##  [sealtalk-config.yml](../sealtalk-start/src/main/resources/sealtalk-config.yml) 各配置说明

| 字段                               | 说明                                                           | 示例                                              |
|----------------------------------|--------------------------------------------------------------|-------------------------------------------------|
| `config_env`                     | 运行环境标识，影响短信等行为。`dev`不发短信,默认短信`9999`。本地调试使用`dev`即可            | `dev`                                           |
| `auth_cookie_name`               | 认证 Cookie 名称，用于登录校验                                          | `testName`                                      |
| `auth_cookie_key`                | 认证 Cookie 加密密钥, 用户加密cookie数据                                 | `aaaabbbb`                                      |
| `auth_cookie_max_age`            | 认证 Cookie 过期秒数                                               | `8640000`                                       |
| `auth_cookie_domain`             | 认证 Cookie 主域名                                                | `test.com`                                      |
| `rongcloud_app_key`              | 融云App Key，请访问[融云开发者后台](https://developer.rongcloud.cn)获取     | `xxxxx`                                         |
| `rongcloud_app_secret`           | 融云App Secret ，请访问[融云开发者后台](https://developer.rongcloud.cn)获取 | `xxxxx`                                         |
| `rongcloud_api_url`              | 融云 API 域名（逗号分隔，首个为主域名）                                       | `https://api.test.com`,`https://api-b.test.com` |
| `ai_api_url`                     | 融云翻译 服务 URL（可选）                                              | `https://test.test.com`                         |
| `rongcloud_default_portrait_url` | 默认头像地址                                                       | `xxxxx`                                         |
| `qiniu_access_key`               | 七牛 Access Key，（可选）请访问[七牛开发者后台](https://portal.qiniu.com)获取   | `QINIU_ACCESS_KEY`                              |
| `qiniu_secret_key`               | 七牛 Secret Key   （可选）                                         | `QINIU_SECRET_KEY`                              |
| `qiniu_bucket_name`              | 七牛空间名（可选）                                                    | `QINIU_BUCKET_name`                             |
| `qiniu_bucket_domain`            | 七牛空间域名（可选）                                                   | `xxxx.com`                                      |
| `ali_sms_access_key_id`          | 阿里短信 Access Key Id（可选）                                       | `ALI_SMS_ACCESS_KEY_ID`                         |
| `ali_sms_access_key_secret`      | 阿里短信 Access Key Secret（可选）                                   | `ALI_SMS_ACCESS_KEY_SECRET`                     |
| `sms_limited_time`               | 短信频控时间窗口（小时）                                                 | `1`                                             |
| `sms_limited_count`              | 时间窗口内验证码发送上限                                                 | `100`                                           |
| `shumei_access_key`              | 数美 Access Key （可选）                                           | `SHUMEI_ACCESS_KEY`                             |
| `shumei_app_id`                  | 数美 App Id （可选）                                               | `SHUMEI_APP_ID`                                 |
| `shumei_api_url`                 | 数美 API URL  （可选）                                             | `xxxx`                                          |
| `n3d_key`                        | N3D 加密密钥（≥5 位字母数字），用于加密id将数字转换为字符串                           | `1a2b3c4d5e`                                    |
| `cors_hosts`                     | CORS 白名单（`*` 或逗号分隔域），请正确配置否则页面访问会有跨越问题，或者使用其他方式提前避免跨域        | `*` 或 `http://web.example.com`                  |
| `server_port`                    | 服务监听端口                                                       | `8080`                                          |
| `db_name`                        | 数据库名                                                         | `sealtalkdb`                                    |
| `db_user`                        | 数据库用户名                                                       | `developer`                                     |
| `db_password`                    | 数据库密码                                                        | `111111`                                        |
| `db_host`                        | 数据库主机                                                        | `127.0.0.1`                                     |
| `db_port`                        | 数据库端口                                                        | `3306`                                          |
| `ai_agent_model`                 | AI Agent 模型名称（可选）                                            | `your_model_name`                               |
| `admin_auth_key`                 | sealtalk-admin后台管理授权 Key （可选），不配置就不进行验证                      | `admin-secret`                                  |





##  [sms_template.json](../sealtalk-start/src/main/resources/sms_template.json) 配置说明

| 字段                | 说明                                     | 示例                      |
|-------------------|----------------------------------------|-------------------------|
| `region`          | 区号， 配置后对应区号的短信使用对应的配置信息，`default`为默认选项 | `default`               |
| `device`          | 设备信息（依赖客户端发送短信时传递到服务端）                 | `ios`，`HarmonyOS`       |
| `templateCode`    | 短信模板id （需要在阿里云短信平台配置）                  | `template_1`            |
| `service`         | 发送短信实现类，默认是阿里云短信实现                     | `AliSmsService`         |
| `url`             | 短信平台地址,默认阿里云短信平台地址                     | `dysmsapi.aliyuncs.com` |
| `signName`        | 短信签名，短信内容中使用的签名信息                      | `test`                  |

示例1:
- 所有的短信都使用模版`t1`、签名`test1`；
  配置如下：
```json
[
  {
    "region": "default",
    "device": "default",
    "templateCode": "t1",
    "service": "AliSmsService",
    "url": "dysmsapi.aliyuncs.com",
    "signName": "test1"
  }
]

```


示例2:  
- `86`区号并且`IOS`手机使用模版`t1`、签名`test1`；
- 其余`86`区号手机使用模版`t2`、签名`test2`；
- 非`86`区号手机统一使用模版`t3`、签名`test3`
配置如下：
```json
[
  {
    "region": "86",
    "device": "IOS",
    "templateCode": "t1",
    "service": "AliSmsService",
    "url": "dysmsapi.aliyuncs.com",
    "signName": "test1"
  },
  {
    "region": "86",
    "device": "default",
    "templateCode": "t2",
    "service": "AliSmsService",
    "url": "dysmsapi.aliyuncs.com",
    "signName": "test2"
  },
  {
    "region": "default",
    "device": "default",
    "templateCode": "t3",
    "service": "AliSmsService",
    "url": "dysmsapi.aliyuncs.com",
    "signName": "test3"
  }
]

```
