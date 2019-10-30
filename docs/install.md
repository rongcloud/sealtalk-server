# 配置安装与开发调试

## 安装 Git

[Git 官网](https://git-scm.com/downloads)

## 安装 Node

[Node 官网](https://nodejs.org), Node 版本需 4.0 - 8.0(可使用 nvm 管理多 Node)

## 安装 MySQL

[MySQL 官网](https://www.mysql.com/)

## 项目配置

请修改 [conf.js](../src/conf.js) 文件中的相关配置, 详细请参看 [conf.js](../src/conf.js) 中的注释和示例

```js
module.exports = {
  // 认证 Cookie 名称, 可根据业务自行定义
  AUTH_COOKIE_NAME: 'rong_auth_cookie', 
  // 认证 Cookie 加密密钥, 可自行定义, 任意字母数字组合
  NICKNAME_COOKIE_NAME: '', 
  // 认证 Cookie 过期时间, 单位为毫秒, 2592000000 毫秒 = 30 天
  AUTH_COOKIE_MAX_AGE: '2592000000', 
  // 融云颁发的 App Key, 访问融云开发者后台获取：https://developer.rongcloud.cn
  RONGCLOUD_APP_KEY: '8ljko22vuee', // 此处为假数据
  // 融云颁发的 App Secret, 访问融云开发者后台获取：https://developer.rongcloud.cn
  RONGCLOUD_APP_SECRET: 'y0je2id4h1LWz', // 此处为假数据
  // 融云短信服务提供的注册用户短信模板 Id, 对应接口 send_code、verify_code. 不使用此接口可不填(客户端默认使用云片短信服务)
  RONGCLOUD_SMS_REGISTER_TEMPLATE_ID: '6iYv6rln4agT3tIPJCS2', // 此处为假数据
  // 云片颁发的 API Key, 访问云片开发者后台获取: https://www.yunpian.com/admin/main, 对应接口 send_code_yp、verify_code_yp. 不使用此接口可不填
  YUNPIAN_API_KEY: '123iijmbbi9kn4bsp92odd21d213q', // 此处为假数据
  // 七牛颁发的 Access Key, 访问七牛开发者后台获取：https://portal.qiniu.com. 使用上传服务需配置此项
  QINIU_ACCESS_KEY: 'kokjii22n2dentEiMxpQ8Qskkookn2ddnX', // 此处为假数据
  // 七牛颁发的 Secret Key, 访问七牛开发者后台获取：https://portal.qiniu.com. 使用上传服务需配置此项
  QINIU_SECRET_KEY: '0pknd92neke9dm1plsn20C6Hni3TIVgjw5', // 此处为假数据
  // 七牛创建的空间名称, 访问七牛开发者后台获取：https://portal.qiniu.com. 使用上传服务需配置此项
  QINIU_BUCKET_NAME: 'devtalk-image', 
  // 七牛创建的空间域名, 访问七牛开发者后台获取：https://portal.qiniu.com. 使用上传服务需配置此项
  QINIU_BUCKET_DOMAIN: 'self.domain.com', // 此处为假数据
  // N3D 密钥, 用来加密所有的 Id 数字, 不小于 5 位的字母数字组合
  N3D_KEY: '11EdDIaqpcim', 
  // 认证 Cookie 主域名, 如没有正式域名, 可直接填写 ip. 必须和 CORS_HOSTS 配置项在相同的顶级域下. 比如 Server 地址为: api.sealtalk.im, Web 地址为: web.sealtalk.im, 此项需填写: sealtalk.im
  AUTH_COOKIE_DOMAIN: 'devtalk.im',
  // 跨域支持所需配置的域名信息, 包括请求服务器的域名和端口号, 如果是 80 端口可以省略端口号. 比如: http://web.sealtalk.im. 如没有正式域名, 可直接填写 ip. 如没有 web 端, 此项可不填
  CORS_HOSTS: 'http://web.devtalk.im',
  // 本服务启动后占用的 HTTP 端口号
  SERVER_PORT: '8585', 
  // MySQL 数据库名称
  DB_NAME: 'sealtalk', 
  // MySQL 数据库用户名
  DB_USER: 'devtalk', 
  // MySQL 数据库密码
  DB_PASSWORD: 'devtalk', 
  // MySQL 数据库服务器地址
  DB_HOST: '127.0.0.1', 
  // MySQL 数据库服务端口号
  DB_PORT: '3306' 
};
```

> 如果您熟悉项目中使用的 Sequelize 数据库框架, 也可以自行安装配置其他类型的数据库。但需要修改 db.js 中相应的 SQL 语句。

## 初始化

项目根目录下执行：

```
node install.js
```

## 设置环境量                       
                                             
Windows   : `set NODE_ENV=development`    
                                         
Mac/Linux : `export NODE_ENV=development`

## 开发环境调试

```
grunt nodemon
```

## 生产环境部署

### 修改环境变量

生产环境下请设置 `NODE_ENV=production`。

### 启动服务

请在部署路径中用 `PM2` 等工具启动 `index.js` 文件。或直接使用 `node index.js` 启动（不推荐）。

如果未安装 `pm2`, 可使用 `npm` 安装:

```
npm install -g pm2
```

启动服务

```
pm2 start src/index.js --name sealtalk-server
```

查看服务列表

```
pm2 list
```

## 注意事项(必读)

1、配置七牛空间, 必须为`华东`存储空间. 如需其他空间, 需修改 SealTalk Server 源码 `src/routes/user.js` get_image_token 接口获取七牛 token 参数

2、客户端默认调用云片短信服务(send_code_yp、verify_code_yp), 如需修改短信服务, 可参考 `src/util/sms.js`

3、开发环境下(NODE_ENV 为 development), 不需要收真实短信, 点击发送验证码后, 输入 `9999` 即可注册

4、`AUTH_COOKIE_DOMAIN` 和 `CORS_HOSTS` 配置项必须按照上述说明配置正确

## 业务数据配置 (无需求略过)

client_version.json : 配置 SealTalk 移动端的最新 App 版本号、下载地址等信息。

squirrel.json : 配置 SealTalk Desktop 端的最新 App 版本号、下载地址等信息。

demo_square.json : 配置 SealTalk 移动端“发现”频道中的默认聊天室和群组数据。