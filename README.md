# SealTalk Server

本项目是 SealTalk 系列应用的后端服务，提供了用户、好友、群组、黑名单相关接口和数据管理服务。

SealTalk 是使用[`融云 RongCloud`](http://www.rongcloud.cn)即时通讯云服务及其 SDK 打造的开源即时通讯应用，覆盖所有主流平台，包括：iOS、Android、Windows、Mac、Web。在融云官网注册开发者帐号后，您可以直接使用 SealTalk 打造您自己的 IM App。

> [!WARNING]
> 我们提供的demo源码旨在供您参考学习，它不包含完整的功能体验或用于开发测试。如果您在使用过程中遇到任何问题或需要进一步的技术支持，请随时提交[工单](https://developer.rongcloud.cn/signin?returnUrl=%2Fticket)，我们的专业团队将乐于为您提供帮助。

## 模块划分

- sealtalk-core：业务核心库（Controller/Service/DAO/Model）。
- sealtalk-start：启动模块， 包含启动所需的配置文件。

## 环境要求

- `Java 21+`
- `Maven 3.8+`
- `MySQL 8.x`

## 配置调整(必读)

请参见 [配置说明](docs/config.md) 文档

*注意：* 融云翻译地址、阿里云短信、数美验证都是可选项。不申请也可以体验使用。

**警告：** 项目表结构管理使用了flyway，体验使用时强烈建议分配一个新的db，防止对您现在db中的表造成影响。项目会自动建库建表，不需提前创建，手动创建可能造成版本检验失败，从而服务启动失败。


## 构建与运行

### 构建及运行步骤

```bash
# {project_dir} 为项目的目录
cd {project_dir}

#打包
mvn -DskipTests clean package

#package产物及启动脚本在build目录
cd build

#启动
sh start.sh

```
*注意：* 如果要将本地打包产物上传到服务器运行，需要将build目录下的所有文件一起上传到服务器。

### 示例

假设服务目录为: `/opt/demo/sealtalk-server`

```bash

cd /opt/demo/sealtalk-server

mvn -DskipTests clean package

cd build

sh start.sh

```



## 相关项目说明

[融云 RongCloud 官网](http://www.rongcloud.cn)

[SealTalk 全平台 App 下载](http://www.rongcloud.cn/downloads)

[SealTalk iOS 开源项目](https://github.com/rongcloud/sealtalk-ios)

[SealTalk Android 开源项目](https://github.com/rongcloud/sealtalk-android)

[SealTalk Web 开源项目](https://github.com/rongcloud/sealtalk-web)
