var ref = require('./db'), sequelize = ref[0];

var sqls = {
  //users 需求 2.1.0
  'gender': "ALTER TABLE users ADD gender VARCHAR( 32 ) NOT NULL DEFAULT 'male'",
  'stAccount': "ALTER TABLE `users` ADD `stAccount` VARCHAR( 32 ) DEFAULT ''",
  'phoneVerify': 'ALTER TABLE `users` ADD `phoneVerify`INTEGER NOT NULL DEFAULT 1',
  'stSearchVerify': 'ALTER TABLE `users` ADD `stSearchVerify`INTEGER NOT NULL DEFAULT 1',
  'friVerify': 'ALTER TABLE `users` ADD `friVerify`INTEGER NOT NULL DEFAULT 1',
  'groupVerify': 'ALTER TABLE `users` ADD `groupVerify`INTEGER NOT NULL DEFAULT 1',
  //users 需求 2.2.0
  'pokeStatus':'ALTER TABLE `users` ADD `pokeStatus`INTEGER NOT NULL DEFAULT 1',
  //groups 需求 2.1.0
  'certiStatus': 'ALTER TABLE `groups` ADD `certiStatus`INTEGER NOT NULL DEFAULT 1',
  'isMute': 'ALTER TABLE `groups` ADD `isMute`INTEGER NOT NULL DEFAULT 0',
  'clearStatus': 'ALTER TABLE `groups` ADD `clearStatus`INTEGER NOT NULL DEFAULT 0',
  'clearTimeAt':'ALTER TABLE `groups` ADD `clearTimeAt`BIGINT NOT NULL DEFAULT 0',
  //groups 需求 2.2.0
  'memberProtection':'ALTER TABLE `groups` ADD `memberProtection`INTEGER NOT NULL DEFAULT 0',
  'copiedTime':'ALTER TABLE `groups` ADD `copiedTime` BIGINT NOT NULL DEFAULT 0 AFTER `timestamp`',
  //group_members 需求 2.2.0
  'groupNickname': "ALTER TABLE `group_members` ADD `groupNickname` VARCHAR( 32 ) DEFAULT ''AFTER `isDeleted`" ,
  'region_group_members': "ALTER TABLE `group_members` ADD `region` VARCHAR( 32 ) DEFAULT ''AFTER `isDeleted`",
  'phone_group_members': "ALTER TABLE `group_members` ADD `phone` VARCHAR( 32 ) DEFAULT ''AFTER `isDeleted`",
  'WeChat': "ALTER TABLE `group_members` ADD `WeChat` VARCHAR( 32 ) DEFAULT ''AFTER `isDeleted`",
  'Alipay': "ALTER TABLE `group_members` ADD `Alipay` VARCHAR( 32 ) DEFAULT ''AFTER `isDeleted`",
  'memberDesc': "ALTER TABLE `group_members` ADD `memberDesc` VARCHAR( 800 ) DEFAULT ''AFTER `isDeleted`",
  //friendships 需求 2.2.0
  'region': "ALTER TABLE `friendships` ADD `region` VARCHAR( 32 ) DEFAULT '' AFTER `status`",
  'phone': "ALTER TABLE `friendships` ADD `phone` VARCHAR( 32 ) DEFAULT '' AFTER `status`",
  'description': "ALTER TABLE `friendships` ADD `description` VARCHAR( 500 ) DEFAULT '' AFTER `status`",
  'imageUri': "ALTER TABLE `friendships` ADD `imageUri` VARCHAR( 256 ) DEFAULT '' AFTER `status`",
}


for(var key in sqls){
  sequelize.query(sqls[key]).spread((results, metadata) => {
    console.log(sqls[key]);
  })
}

