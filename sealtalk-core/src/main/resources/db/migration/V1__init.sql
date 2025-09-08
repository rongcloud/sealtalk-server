CREATE TABLE `ai_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `template_id` varchar(64) NOT NULL DEFAULT '' COMMENT '模板ID',
  `template_name` varchar(64) NOT NULL DEFAULT '' COMMENT '模板名称',
  `system_prompt` varchar(4096) NOT NULL DEFAULT '' COMMENT '系统提示词',
  `language` varchar(16) NOT NULL DEFAULT '' COMMENT '语言种类',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_template_lang` (`template_id`,`language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模板表';


CREATE TABLE `ai_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `ai_user_id` varchar(64) NOT NULL DEFAULT '' COMMENT 'AI 角色ID',
  `template_id` varchar(64) NOT NULL DEFAULT '' COMMENT '模板ID',
  `avatar` varchar(512) NOT NULL DEFAULT '' COMMENT '头像地址',
  `gender` int NOT NULL DEFAULT '0' COMMENT '性别',
  `age` int NOT NULL DEFAULT '0' COMMENT '年龄',
  `open` tinyint NOT NULL DEFAULT '0' COMMENT '是否公开',
  `system_prompt` varchar(10000) NOT NULL DEFAULT '' COMMENT '系统提示词',
  `create_source` int NOT NULL DEFAULT '0' COMMENT '创建来源',
  `create_type` int NOT NULL DEFAULT '0' COMMENT '创建方式',
  `creator_id` bigint NOT NULL DEFAULT '0' COMMENT '创建者用户ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_ai_user_id` (`ai_user_id`),
  KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='AI 用户（角色）表';


CREATE TABLE `ai_user_i18n` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `ai_user_id` varchar(64) NOT NULL DEFAULT '' COMMENT 'AI 角色ID',
  `language` varchar(16) NOT NULL DEFAULT '' COMMENT '语言种类',
  `nickname` varchar(64) NOT NULL DEFAULT '' COMMENT '昵称',
  `area` varchar(64) NOT NULL DEFAULT '' COMMENT '地区',
  `profession` varchar(64) NOT NULL DEFAULT '' COMMENT '职业',
  `introduction` varchar(1024) NOT NULL DEFAULT '' COMMENT '介绍',
  `tags` varchar(512) NOT NULL DEFAULT '' COMMENT '标签',
  `opening_remark` varchar(512) NOT NULL DEFAULT '' COMMENT '开场白',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_ai_user_lang` (`ai_user_id`,`language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='AI 用户多语言信息表';

CREATE TABLE `blacklists` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `userId` int unsigned NOT NULL,
  `friendId` int unsigned NOT NULL,
  `status` tinyint NOT NULL,
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `blacklists_user_id_friend_id` (`userId`,`friendId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='黑名单表';


CREATE TABLE `bot_info` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `bot_id` varchar(64) NOT NULL COMMENT '机器人ID',
  `name` varchar(100) NOT NULL COMMENT '机器人名称',
  `portraitUri` varchar(500) NOT NULL DEFAULT '' COMMENT '机器人头像URL',
  `opening_message` varchar(1000) NOT NULL DEFAULT '' COMMENT '开场消息',
  `bot_type` int NOT NULL DEFAULT '0' COMMENT '机器人类型',
  `bot_status` tinyint NOT NULL DEFAULT '1' COMMENT '是否生效 1:生效 0:不生效',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `bot_id_idx` (`bot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='机器人信息表';

CREATE TABLE `bot_user_bind` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `bot_id` varchar(64) NOT NULL COMMENT '机器人ID',
  `bind_type` int NOT NULL DEFAULT '0',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_bot_unique_idx` (`user_id`,`bot_id`),
  KEY `bot_id_idx` (`bot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户机器人绑定表';



CREATE TABLE `config_list` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `att_key` varchar(80) NOT NULL,
  `att_value` varchar(2048) NOT NULL DEFAULT '',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `IX_ATTKEY` (`att_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='配置信息表';


CREATE TABLE `customer_report` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `report_id` varchar(64) NOT NULL COMMENT '举报人',
  `target_id` varchar(64) NOT NULL COMMENT '被举报人',
  `channel_type` int NOT NULL,
  `report_level_first` varchar(64) NOT NULL DEFAULT '',
  `report_level_second` varchar(64) NOT NULL DEFAULT '',
  `pic` varchar(1000) NOT NULL DEFAULT '',
  `content` varchar(500) NOT NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='客户举报信息记录';

CREATE TABLE `freeze_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `phone` varchar(20) NOT NULL,
  `region` varchar(20) NOT NULL DEFAULT '',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户id',
  `freeze_time` bigint NOT NULL COMMENT '归档时间',
  `data` mediumtext NOT NULL COMMENT '归档数据',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='数据归档';


CREATE TABLE `friendships` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `userId` int unsigned NOT NULL,
  `friendId` int unsigned NOT NULL COMMENT '好友id',
  `displayName` varchar(32) NOT NULL DEFAULT '' COMMENT '备注名',
  `message` varchar(64) NOT NULL,
  `status` int unsigned NOT NULL,
  `region` varchar(32) DEFAULT '',
  `phone` varchar(32) DEFAULT '',
  `description` varchar(500) DEFAULT '',
  `imageUri` varchar(256) DEFAULT '',
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `friendships_user_id_friend_id` (`userId`,`friendId`),
  KEY `user_id_timestamp` (`userId`,`createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='好友关系表';


CREATE TABLE `group_bulletins` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `groupId` int unsigned NOT NULL,
  `content` text,
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `groupId_idx` (`groupId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='群公告';



CREATE TABLE `group_exited_lists` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `groupId` int unsigned NOT NULL,
  `quitUserId` int unsigned NOT NULL,
  `quitNickname` varchar(32) NOT NULL,
  `quitPortraitUri` varchar(256) NOT NULL DEFAULT '',
  `quitReason` int unsigned NOT NULL,
  `quitTime` bigint NOT NULL,
  `operatorId` int unsigned DEFAULT NULL,
  `operatorName` varchar(32) DEFAULT NULL,
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `groupId_userid_idx` (`groupId`,`quitUserId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='退群记录';


CREATE TABLE `group_favs` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `userId` int unsigned NOT NULL,
  `groupId` int unsigned NOT NULL,
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `groupfavindex` (`userId`,`groupId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='通讯录';



CREATE TABLE `group_members` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `groupId` int unsigned NOT NULL,
  `memberId` int unsigned NOT NULL,
  `displayName` varchar(32) NOT NULL DEFAULT '' COMMENT '群内备注名',
  `role` int unsigned NOT NULL COMMENT '群内角色',
  `groupNickname` varchar(32) NOT NULL DEFAULT '' COMMENT '群内备注名',
  `region` varchar(32) DEFAULT '',
  `phone` varchar(32) DEFAULT '',
  `WeChat` varchar(32) DEFAULT '',
  `Alipay` varchar(32) DEFAULT '',
  `memberDesc` varchar(800) DEFAULT '',
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `group_members_group_id_member_id_is_deleted` (`groupId`,`memberId`),
  KEY `member_id_ct_idx` (`memberId`,`createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='群成员列表';


CREATE TABLE `group_receivers` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `userId` int unsigned NOT NULL COMMENT '操作接收人',
  `groupId` int unsigned DEFAULT NULL,
  `groupName` varchar(32) NOT NULL DEFAULT '',
  `groupPortraitUri` varchar(256) NOT NULL DEFAULT '',
  `requesterId` int unsigned DEFAULT NULL COMMENT '操作发起人',
  `receiverId` int unsigned DEFAULT NULL COMMENT '操作目标人',
  `type` int unsigned NOT NULL,
  `status` int unsigned NOT NULL,
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `groupId` (`groupId`),
  KEY `requesterId` (`requesterId`),
  KEY `receiverId` (`receiverId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='群操作记录表';


CREATE TABLE `groups` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL,
  `portraitUri` varchar(256) NOT NULL DEFAULT '',
  `memberCount` int unsigned NOT NULL DEFAULT '0',
  `maxMemberCount` int unsigned NOT NULL DEFAULT '500',
  `creatorId` int unsigned NOT NULL,
  `bulletin` text,
  `certiStatus` int unsigned NOT NULL DEFAULT '1' COMMENT '进群认证状态',
  `isMute` int unsigned NOT NULL DEFAULT '0' COMMENT '是否禁言',
  `clearStatus` int unsigned NOT NULL DEFAULT '0' COMMENT '群消息清理配置',
  `clearTimeAt` bigint unsigned NOT NULL DEFAULT '0',
  `memberProtection` int unsigned NOT NULL DEFAULT '0' COMMENT '群保护配置',
  `copiedTime` bigint unsigned NOT NULL DEFAULT '0',
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `create_idx` (`createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='群信息表';


CREATE TABLE `login_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户id',
  `login_time` bigint NOT NULL COMMENT '登陆时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户登陆token记录表';


CREATE TABLE `msg_callback` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `target_id` varchar(64) NOT NULL DEFAULT '',
  `channel_type` varchar(64) NOT NULL DEFAULT '',
  `msg_type` varchar(64) NOT NULL DEFAULT '',
  `msg_content` mediumblob,
  `msg_id` varchar(64) NOT NULL DEFAULT '',
  `msg_time` datetime NOT NULL,
  `strategy` varchar(64) NOT NULL DEFAULT '',
  `risk_status` varchar(32) NOT NULL DEFAULT '',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `IX_USERID` (`user_id`),
  KEY `IX_CHANNELTYPE` (`channel_type`),
  KEY `IX_MSGTYPE` (`msg_type`),
  KEY `IX_RISKSTATUS` (`risk_status`),
  KEY `IX_CREATEDAT` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='消息审核回调记录';



CREATE TABLE `risk_list` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `risk_status` varchar(32) NOT NULL DEFAULT '',
  `device_id` varchar(512) NOT NULL DEFAULT '',
  `ip` varchar(64) NOT NULL DEFAULT '',
  `detail` varchar(64) NOT NULL DEFAULT '',
  `other_detail` mediumblob,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `IX_USERID_DEVICEID` (`user_id`,`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户风险表';

CREATE TABLE `screen_statuses` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `operateId` char(40) NOT NULL,
  `conversationType` int unsigned NOT NULL,
  `status` int unsigned NOT NULL,
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `screen_statuses_operate_id` (`operateId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='截屏状态';


CREATE TABLE `ug_channel_usergroup` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel_id` varchar(20) COLLATE utf8mb4_bin NOT NULL COMMENT '频道Id',
  `usergroup_id` bigint NOT NULL COMMENT '用户组Id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_channel_usergroup` (`channel_id`,`usergroup_id`),
  KEY `idx_usergroup_id` (`usergroup_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='超级群频道用户组绑定表';


CREATE TABLE `ug_usergroup` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '用户组名称',
  `group_id` bigint NOT NULL COMMENT '群组Id',
  `creator_id` int NOT NULL COMMENT '创建人id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户组信息表';

CREATE TABLE `ug_usergroup_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `usergroup_id` bigint NOT NULL COMMENT '用户组Id',
  `member_id` int NOT NULL COMMENT '用户Id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_usergroup_member` (`usergroup_id`,`member_id`),
  KEY `idx_member_id` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户组成员表';


CREATE TABLE `ultra_group` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL,
  `portraitUri` varchar(256) NOT NULL DEFAULT '',
  `creatorId` int unsigned NOT NULL,
  `summary` text,
  `memberCount` int unsigned NOT NULL,
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='超级群表';

CREATE TABLE `ultra_group_channel` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `ultraGroupId` int unsigned NOT NULL,
  `channelId` varchar(20) NOT NULL,
  `channelName` varchar(32) NOT NULL,
  `type` int NOT NULL DEFAULT '0',
  `creatorId` int unsigned NOT NULL,
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `un_ultragroup_channle_id` (`ultraGroupId`,`channelId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='超级群频道列表';

CREATE TABLE `ultra_group_members` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `ultraGroupId` int unsigned NOT NULL,
  `memberId` int unsigned NOT NULL,
  `role` int unsigned NOT NULL,
  `groupNickname` varchar(32) NOT NULL DEFAULT '',
  `createdAt` datetime NOT NULL,
  `updatedAt` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ultragroup_members_group_member_id` (`ultraGroupId`,`memberId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='超级群成员表';


CREATE TABLE `url_store` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `url` varchar(1000) NOT NULL COMMENT '资源URL',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='URL 存储表';



CREATE TABLE `users` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `region` varchar(5) NOT NULL,
  `phone` varchar(11) NOT NULL,
  `nickname` varchar(32) NOT NULL COMMENT '名称',
  `portraitUri` varchar(256) NOT NULL DEFAULT '' COMMENT '头像',
  `rongCloudToken` varchar(256) NOT NULL DEFAULT '' COMMENT '融云token',
  `gender` varchar(32) NOT NULL DEFAULT 'male',
  `stAccount` varchar(32) NOT NULL DEFAULT '' COMMENT 'st账号',
  `phoneVerify` int unsigned NOT NULL DEFAULT '1' COMMENT '手机号查询权限',
  `stSearchVerify` int unsigned NOT NULL DEFAULT '1' COMMENT 'st账号查询权限',
  `friVerify` int unsigned NOT NULL DEFAULT '1' COMMENT '加好友权限',
  `groupVerify` int unsigned NOT NULL DEFAULT '1' COMMENT '加群权限',
  `pokeStatus` int unsigned NOT NULL DEFAULT '1',
  `groupCount` int unsigned NOT NULL DEFAULT '0',
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `blockStatus` int unsigned NOT NULL DEFAULT '0',
  `blockStartTime` datetime DEFAULT NULL,
  `blockEndTime` datetime DEFAULT NULL,
  `lastIp` varchar(64) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `users_region_phone` (`phone`,`region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户信息表';


CREATE TABLE `verification_codes` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `region` varchar(5) NOT NULL,
  `phone` varchar(11) NOT NULL,
  `sessionId` varchar(32) NOT NULL,
  `token` char(36) NOT NULL,
  `createdAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `verification_codes_region_phone` (`region`,`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='短信验证表';


CREATE TABLE `verification_violations` (
  `ip` varchar(64) NOT NULL,
  `time` datetime NOT NULL,
  `count` int unsigned DEFAULT NULL,
  PRIMARY KEY (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='短信发送记录表';

CREATE TABLE `white_list` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `region` varchar(5) NOT NULL,
  `phone` varchar(30) NOT NULL,
  `type` int unsigned NOT NULL COMMENT '白名单类型',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `phone_type_idx` (`phone`,`region`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='白名单表';