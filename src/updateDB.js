var ref = require('./db'), sequelize = ref[0];

var sqls = {
  //users
  'gender': "ALTER TABLE users ADD gender VARCHAR( 32 ) NOT NULL DEFAULT 'male'",
  'stAccount': "ALTER TABLE `users` ADD `stAccount` VARCHAR( 32 ) DEFAULT ''",
  'phoneVerify': 'ALTER TABLE `users` ADD `phoneVerify`INTEGER NOT NULL DEFAULT 1',
  'stSearchVerify': 'ALTER TABLE `users` ADD `stSearchVerify`INTEGER NOT NULL DEFAULT 1',
  'friVerify': 'ALTER TABLE `users` ADD `friVerify`INTEGER NOT NULL DEFAULT 1',
  'groupVerify': 'ALTER TABLE `users` ADD `groupVerify`INTEGER NOT NULL DEFAULT 1',
  //groups
  'certiStatus': 'ALTER TABLE `groups` ADD `certiStatus`INTEGER NOT NULL DEFAULT 1',
  'isMute': 'ALTER TABLE `groups` ADD `isMute`INTEGER NOT NULL DEFAULT 0',
  'clearStatus': 'ALTER TABLE `groups` ADD `clearStatus`INTEGER NOT NULL DEFAULT 0',
  'clearTimeAt':'ALTER TABLE `groups` ADD `clearTimeAt`BIGINT NOT NULL DEFAULT 0'
}


for(var key in sqls){
  sequelize.query(sqls[key]).spread((results, metadata) => {
    console.log(sqls[key]);
  })
}

