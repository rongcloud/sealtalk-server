var Blacklist, Config, DataVersion, Department, DepartmentMember, Duty, Friendship, GROUP_CREATOR, GROUP_MEMBER, Group, GroupMember, GroupSync, HTTPError, LoginLog, Sequelize, User, UserRelation, Utility, VerificationCode, _, co, dataVersionClassMethods, departClassMethods, friendshipClassMethods, groupClassMethods, groupMemberClassMethods, sequelize, userClassMethods, userRelationCalssMethods, verificationCodeClassMethods;

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

departClassMethods = {
  getInfo: function(departId) {
    return Department.findById(departId, {
      attributes: ['id', 'deptName', 'sort', 'parentId']
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

userRelationCalssMethods = {
  starList: function(userId) {
    var sql;
    sql = ['select t.nickname as displayName, t1.id, t1.tel, t1.region,t1.phone, t1.email, t1.nickname, t1.portraitUri, ', 't2.deptId, t3.deptName,t4.dutyName, t2.managerId, t5.nickname as managerName,', 't5.portraitUri as managerPortrait, t5.phone as managerPhone, 1 as star from user_relations t ', 'left join users t1 on t.targetId = t1.id ', 'left join dept_members t2 on t1.id = t2.userId ', 'left join departments t3 on t2.deptId = t3.id ', 'left join duties t4 on t2.dutyId = t4.id ', 'left join users t5 on t2.managerId = t5.id ', 'where t.star = 1 and t.userId = ' + userId].join('');
    return sequelize.query(sql, {
      type: sequelize.QueryTypes.SELECT
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
  email: {
    type: Sequelize.STRING(250),
    allowNull: true,
    defaultValue: ''
  },
  tel: {
    type: Sequelize.STRING(250),
    allowNull: true,
    defaultValue: ''
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

Duty = sequelize.define('duties', {
  id: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true,
    autoIncrement: true
  },
  dutyName: {
    type: Sequelize.STRING(20),
    allowNull: true,
    defaultValue: ''
  }
});

Department = sequelize.define('departments', {
  id: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true,
    autoIncrement: true
  },
  deptNum: {
    type: Sequelize.STRING(20),
    allowNull: true,
    defaultValue: ''
  },
  deptName: {
    type: Sequelize.STRING(100),
    allowNull: true,
    defaultValue: ''
  },
  sort: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    defaultValue: 0
  },
  parentId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: true
  },
  parentPath: {
    type: Sequelize.STRING(100),
    allowNull: true
  }
}, {
  classMethods: departClassMethods,
  indexes: [
    {
      unique: true,
      fields: ['id', 'deptName']
    }
  ]
});

DepartmentMember = sequelize.define('dept_members', {
  id: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true,
    autoIncrement: true
  },
  deptId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: true
  },
  userId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: true
  },
  displayName: {
    type: Sequelize.STRING(45),
    allowNull: true,
    defaultValue: ''
  },
  managerId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: true
  }
}, {
  indexes: [
    {
      unique: true,
      fields: ['deptId', 'userId']
    }
  ]
});

DepartmentMember.belongsTo(User, {
  foreignKey: 'userId',
  constraints: false
});

DepartmentMember.belongsTo(Department, {
  foreignKey: 'deptId',
  constraints: false
});

DepartmentMember.belongsTo(Duty, {
  foreignKey: 'dutyId',
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
    allowNull: true
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
  type: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    defaultValue: 1
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
  show: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    defaultValue: 1
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

UserRelation = sequelize.define('user_relation', {
  userId: {
    type: Sequelize.STRING(64),
    allowNull: false,
    comment: '当前用户的 Id'
  },
  targetId: {
    type: Sequelize.STRING(64),
    allowNull: false,
    comment: '联系人的用户 Id'
  },
  nickname: {
    type: Sequelize.STRING(64),
    allowNull: true,
    comment: '星标联系人备注'
  },
  star: {
    type: Sequelize.BOOLEAN,
    allowNull: true,
    comment: '星标联系人标识'
  }
}, {
  classMethods: userRelationCalssMethods
});

UserRelation.belongsTo(User, {
  foreignKey: 'targetId',
  constraints: false
});

module.exports = [sequelize, User, Blacklist, Friendship, Duty, Department, DepartmentMember, Group, GroupMember, GroupSync, DataVersion, VerificationCode, LoginLog, UserRelation];
