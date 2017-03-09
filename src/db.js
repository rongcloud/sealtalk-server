var Blacklist, Config, DataVersion, Friendship, GROUP_CREATOR, GROUP_MEMBER, Group, GroupMember, GroupSync, HTTPError, LoginLog, Sequelize, User, Utility, VerificationCode, _, co, dataVersionClassMethods, friendshipClassMethods, groupClassMethods, groupMemberClassMethods, sequelize, userClassMethods, verificationCodeClassMethods;

Sequelize = require('sequelize');

co = require('co');

_ = require('underscore');

Config = require('./conf');

Utility = require('./util/util').Utility;

HTTPError = require('./util/util').HTTPError;

GROUP_CREATOR = 0;

GROUP_MEMBER = 1;

sequelize = new Sequelize(Config.DB_NAME, Config.DB_USER, Config.DB_PASSWORD, {
  host: Config.DB_HOST,
  port: Config.DB_PORT,
  dialect: 'mysql',
  timezone: '+08:00',
  logging: null
});

userClassMethods = {
  getNicknames: function(userIds) {
    return User.findAll({
      where: {
        id: {
          $in: userIds
        }
      },
      attributes: ['id', 'nickname']
    }).then(function(users) {
      return userIds.map(function(userId) {
        return _.find(users, function(user) {
          return user.id === userId;
        }).nickname;
      });
    });
  },
  getNickname: function(userId) {
    return User.findById(userId, {
      attributes: ['nickname']
    }).then(function(user) {
      if (user) {
        return user.nickname;
      } else {
        return null;
      }
    });
  },
  checkUserExists: function(userId) {
    return User.count({
      where: {
        id: userId
      }
    }).then(function(count) {
      return count === 1;
    });
  },
  checkPhoneAvailable: function(region, phone) {
    return User.count({
      where: {
        region: region,
        phone: phone
      }
    }).then(function(count) {
      return count === 0;
    });
  }
};

friendshipClassMethods = {
  getInfo: function(userId, friendId) {
    return Friendship.findOne({
      where: {
        userId: userId,
        friendId: friendId
      },
      attributes: ['id', 'status', 'message', 'timestamp', 'updatedAt']
    });
  }
};

groupClassMethods = {
  getInfo: function(groupId) {
    return Group.findById(groupId, {
      attributes: ['id', 'name', 'creatorId', 'memberCount']
    });
  }
};

groupMemberClassMethods = {
  bulkUpsert: function(groupId, memberIds, timestamp, transaction, creatorId) {
    return co(function*() {
      var createGroupMembers, groupMembers, roleFlag, updateGroupMemberIds;
      groupMembers = (yield GroupMember.unscoped().findAll({
        where: {
          groupId: groupId
        },
        attributes: ['memberId', 'isDeleted']
      }));
      createGroupMembers = [];
      updateGroupMemberIds = [];
      roleFlag = GROUP_MEMBER;
      memberIds.forEach(function(memberId) {
        var isUpdateMember;
        if (Utility.isEmpty(memberId)) {
          throw new HTTPError('Empty memberId in memberIds.', 400);
        }
        if (memberId === creatorId) {
          roleFlag = GROUP_CREATOR;
        }
        isUpdateMember = false;
        groupMembers.some(function(groupMember) {
          if (memberId === groupMember.memberId) {
            if (!groupMember.isDeleted) {
              throw new HTTPError('Should not add exist member to the group.', 400);
            }
            return isUpdateMember = true;
          } else {
            return false;
          }
        });
        if (isUpdateMember) {
          return updateGroupMemberIds.push(memberId);
        } else {
          return createGroupMembers.push({
            groupId: groupId,
            memberId: memberId,
            role: memberId === creatorId ? GROUP_CREATOR : GROUP_MEMBER,
            timestamp: timestamp
          });
        }
      });
      if (creatorId !== void 0 && roleFlag === GROUP_MEMBER) {
        throw new HTTPError('Creator is not in memeber list.', 400);
      }
      if (updateGroupMemberIds.length > 0) {
        (yield GroupMember.unscoped().update({
          role: GROUP_MEMBER,
          isDeleted: false,
          timestamp: timestamp
        }, {
          where: {
            groupId: groupId,
            memberId: {
              $in: updateGroupMemberIds
            }
          },
          transaction: transaction
        }));
      }
      return (yield GroupMember.bulkCreate(createGroupMembers, {
        transaction: transaction
      }));
    });
  },
  getGroupCount: function(userId) {
    return GroupMember.count({
      where: {
        memberId: userId
      }
    });
  }
};

dataVersionClassMethods = {
  updateUserVersion: function(userId, timestamp) {
    return DataVersion.update({
      userVersion: timestamp
    }, {
      where: {
        userId: userId
      }
    });
  },
  updateBlacklistVersion: function(userId, timestamp) {
    return DataVersion.update({
      blacklistVersion: timestamp
    }, {
      where: {
        userId: userId
      }
    });
  },
  updateFriendshipVersion: function(userId, timestamp) {
    return DataVersion.update({
      friendshipVersion: timestamp
    }, {
      where: {
        userId: userId
      }
    });
  },
  updateAllFriendshipVersion: function(userId, timestamp) {
    return sequelize.query('UPDATE data_versions d JOIN friendships f ON d.userId = f.userId AND f.friendId = ? AND f.status = 20 SET d.friendshipVersion = ?', {
      replacements: [userId, timestamp],
      type: Sequelize.QueryTypes.UPDATE
    });
  },
  updateGroupVersion: function(groupId, timestamp) {
    return sequelize.query('UPDATE data_versions d JOIN group_members g ON d.userId = g.memberId AND g.groupId = ? AND g.isDeleted = 0 SET d.groupVersion = ?', {
      replacements: [groupId, timestamp],
      type: Sequelize.QueryTypes.UPDATE
    });
  },
  updateGroupMemberVersion: function(groupId, timestamp) {
    return sequelize.query('UPDATE data_versions d JOIN group_members g ON d.userId = g.memberId AND g.groupId = ? AND g.isDeleted = 0 SET d.groupVersion = ?, d.groupMemberVersion = ?', {
      replacements: [groupId, timestamp, timestamp],
      type: Sequelize.QueryTypes.UPDATE
    });
  }
};

verificationCodeClassMethods = {
  getByToken: function(token) {
    return VerificationCode.findOne({
      where: {
        token: token
      },
      attributes: ['region', 'phone']
    });
  },
  getByPhone: function(region, phone) {
    return VerificationCode.findOne({
      where: {
        region: region,
        phone: phone
      },
      attributes: ['sessionId', 'token', 'updatedAt']
    });
  }
};

User = sequelize.define('users', {
  id: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true,
    autoIncrement: true
  },
  region: {
    type: Sequelize.STRING(5),
    allowNull: false,
    validate: {
      isInt: true
    }
  },
  phone: {
    type: Sequelize.STRING(11),
    allowNull: false,
    validate: {
      isInt: true
    }
  },
  nickname: {
    type: Sequelize.STRING(32),
    allowNull: false
  },
  portraitUri: {
    type: Sequelize.STRING(256),
    allowNull: false,
    defaultValue: ''
  },
  passwordHash: {
    type: Sequelize.CHAR(40),
    allowNull: false
  },
  passwordSalt: {
    type: Sequelize.CHAR(4),
    allowNull: false
  },
  rongCloudToken: {
    type: Sequelize.STRING(256),
    allowNull: false,
    defaultValue: ''
  },
  groupCount: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    defaultValue: 0
  },
  timestamp: {
    type: Sequelize.BIGINT,
    allowNull: false,
    defaultValue: 0,
    comment: '时间戳（版本号）'
  }
}, {
  classMethods: userClassMethods,
  paranoid: true,
  indexes: [
    {
      unique: true,
      fields: ['region', 'phone']
    }
  ]
});

Blacklist = sequelize.define('blacklists', {
  id: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true,
    autoIncrement: true
  },
  userId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  friendId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  status: {
    type: Sequelize.BOOLEAN,
    allowNull: false,
    comment: 'true: 拉黑'
  },
  timestamp: {
    type: Sequelize.BIGINT.UNSIGNED,
    allowNull: false,
    defaultValue: 0,
    comment: '时间戳（版本号）'
  }
}, {
  indexes: [
    {
      unique: true,
      fields: ['userId', 'friendId']
    }, {
      method: 'BTREE',
      fields: ['userId', 'timestamp']
    }
  ]
});

Blacklist.belongsTo(User, {
  foreignKey: 'friendId',
  constraints: false
});

Friendship = sequelize.define('friendships', {
  id: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true,
    autoIncrement: true
  },
  userId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  friendId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  displayName: {
    type: Sequelize.STRING(32),
    allowNull: false,
    defaultValue: ''
  },
  message: {
    type: Sequelize.STRING(64),
    allowNull: false
  },
  status: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    comment: '10: 请求, 11: 被请求, 20: 同意, 21: 忽略, 30: 被删除'
  },
  timestamp: {
    type: Sequelize.BIGINT.UNSIGNED,
    allowNull: false,
    defaultValue: 0,
    comment: '时间戳（版本号）'
  }
}, {
  classMethods: friendshipClassMethods,
  indexes: [
    {
      unique: true,
      fields: ['userId', 'friendId']
    }, {
      method: 'BTREE',
      fields: ['userId', 'timestamp']
    }
  ]
});

Friendship.belongsTo(User, {
  foreignKey: 'friendId',
  constraints: false
});

Group = sequelize.define('groups', {
  id: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true,
    autoIncrement: true
  },
  name: {
    type: Sequelize.STRING(32),
    allowNull: false,
    comment: '最小 2 个字'
  },
  portraitUri: {
    type: Sequelize.STRING(256),
    allowNull: false,
    defaultValue: ''
  },
  memberCount: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    defaultValue: 0
  },
  maxMemberCount: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    defaultValue: 500
  },
  creatorId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  bulletin: {
    type: Sequelize.TEXT,
    allowNull: true
  },
  timestamp: {
    type: Sequelize.BIGINT.UNSIGNED,
    allowNull: false,
    defaultValue: 0,
    comment: '时间戳（版本号）'
  }
}, {
  classMethods: groupClassMethods,
  paranoid: true,
  indexes: [
    {
      unique: true,
      fields: ['id', 'timestamp']
    }
  ]
});

GroupMember = sequelize.define('group_members', {
  id: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true,
    autoIncrement: true
  },
  groupId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  memberId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  displayName: {
    type: Sequelize.STRING(32),
    allowNull: false,
    defaultValue: ''
  },
  role: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    comment: '0: 创建者, 1: 普通成员'
  },
  isDeleted: {
    type: Sequelize.BOOLEAN,
    allowNull: false,
    defaultValue: false
  },
  timestamp: {
    type: Sequelize.BIGINT.UNSIGNED,
    allowNull: false,
    defaultValue: 0,
    comment: '时间戳（版本号）'
  }
}, {
  classMethods: groupMemberClassMethods,
  defaultScope: {
    where: {
      isDeleted: false
    }
  },
  indexes: [
    {
      unique: true,
      fields: ['groupId', 'memberId', 'isDeleted']
    }, {
      method: 'BTREE',
      fields: ['memberId', 'timestamp']
    }
  ]
});

GroupMember.belongsTo(User, {
  foreignKey: 'memberId',
  constraints: false
});

GroupMember.belongsTo(Group, {
  foreignKey: 'groupId',
  constraints: false
});

GroupSync = sequelize.define('group_syncs', {
  groupId: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true
  },
  syncInfo: {
    type: Sequelize.BOOLEAN,
    allowNull: false,
    defaultValue: false,
    comment: '是否需要同步群组信息到 IM 服务器'
  },
  syncMember: {
    type: Sequelize.BOOLEAN,
    allowNull: false,
    defaultValue: false,
    comment: '是否需要同步群组成员到 IM 服务器'
  },
  dismiss: {
    type: Sequelize.BOOLEAN,
    allowNull: false,
    defaultValue: false,
    comment: '是否需要在 IM 服务端成功解散群组'
  }
}, {
  timestamps: false
});

DataVersion = sequelize.define('data_versions', {
  userId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    primaryKey: true
  },
  userVersion: {
    type: Sequelize.BIGINT.UNSIGNED,
    allowNull: false,
    defaultValue: 0,
    comment: '用户信息时间戳（版本号）'
  },
  blacklistVersion: {
    type: Sequelize.BIGINT.UNSIGNED,
    allowNull: false,
    defaultValue: 0,
    comment: '黑名单时间戳（版本号）'
  },
  friendshipVersion: {
    type: Sequelize.BIGINT.UNSIGNED,
    allowNull: false,
    defaultValue: 0,
    comment: '好友关系时间戳（版本号）'
  },
  groupVersion: {
    type: Sequelize.BIGINT.UNSIGNED,
    allowNull: false,
    defaultValue: 0,
    comment: '群组信息时间戳（版本号）'
  },
  groupMemberVersion: {
    type: Sequelize.BIGINT.UNSIGNED,
    allowNull: false,
    defaultValue: 0,
    comment: '群组关系时间戳（版本号）'
  }
}, {
  classMethods: dataVersionClassMethods,
  timestamps: false
});

VerificationCode = sequelize.define('verification_codes', {
  id: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true,
    autoIncrement: true
  },
  region: {
    type: Sequelize.STRING(5),
    allowNull: false,
    primaryKey: true
  },
  phone: {
    type: Sequelize.STRING(11),
    allowNull: false,
    primaryKey: true
  },
  sessionId: {
    type: Sequelize.STRING(32),
    allowNull: false
  },
  token: {
    type: Sequelize.UUID,
    allowNull: false,
    defaultValue: Sequelize.UUIDV1,
    unique: true
  }
}, {
  classMethods: verificationCodeClassMethods,
  indexes: [
    {
      unique: true,
      fields: ['region', 'phone']
    }
  ]
});

LoginLog = sequelize.define('login_logs', {
  id: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true,
    autoIncrement: true
  },
  userId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  ipAddress: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  os: {
    type: Sequelize.STRING(64),
    allowNull: false
  },
  osVersion: {
    type: Sequelize.STRING(64),
    allowNull: false
  },
  carrier: {
    type: Sequelize.STRING(64),
    allowNull: false
  },
  device: {
    type: Sequelize.STRING(64)
  },
  manufacturer: {
    type: Sequelize.STRING(64)
  },
  userAgent: {
    type: Sequelize.STRING(256)
  }
}, {
  updatedAt: false
});

module.exports = [sequelize, User, Blacklist, Friendship, Group, GroupMember, GroupSync, DataVersion, VerificationCode, LoginLog];
