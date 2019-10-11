var Blacklist, Config, DataVersion, Friendship, Group, GroupMember, GroupReceiver, ScreenStatus, GroupSync, HTTPError, LoginLog, Sequelize, User, Utility, VerificationCode, VerificationViolation, _, co, dataVersionClassMethods, friendshipClassMethods, groupClassMethods, groupMemberClassMethods, sequelize, userClassMethods, verificationCodeClassMethods;

Sequelize = require('sequelize');

co = require('co');

_ = require('underscore');

Config = require('./conf');

var utils = require('./util/util');

var addUpdateTimeToList = utils.addUpdateTimeToList;

Utility = utils.Utility;

HTTPError = require('./util/util').HTTPError;

var ENUM = require('./util/enum');
var GroupRole = require('./util/enum').GroupRole;
var GROUP_CREATOR = GroupRole.CREATOR,
  GROUP_MEMBER = GroupRole.MEMBER,
  GROUP_MANAGER = GroupRole.MANAGER;

var GroupFav, GroupBulletin;

sequelize = new Sequelize(Config.DB_NAME, Config.DB_USER, Config.DB_PASSWORD, {
  host: Config.DB_HOST,
  port: Config.DB_PORT,
  dialect: 'mysql',
  charset: 'utf8',
  dialectOptions: {
    charset: 'utf8',
    collate: 'utf8_general_ci'
  },
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
  },
  hasGroupVerify: function (userId) {
    return User.findOne({
      where: {
        id: userId
      },
      attributes: ['groupVerify']
    }).then(function (result) {
      return result && result.groupVerify == ENUM.CertiStatus.OPENED;
    });
  },
  batchGroupVerify: function (userIdList) {
    return User.findAll({
      where: {
        id: {
          $in: userIdList
        }
      },
      attributes: ['id', 'groupVerify']
    }).then(function (list) {
      var veirfyOpenedUserList = [],
        verifyClosedUserList = [];
      list.forEach(function (user) {
        if (user.groupVerify == ENUM.CertiStatus.OPENED) {
          veirfyOpenedUserList.push(user.id);
        } else {
          verifyClosedUserList.push(user.id);
        }
      });
      return {
        opened: veirfyOpenedUserList,
        closed: verifyClosedUserList
      };
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
  },
  hasManagerRole: function (groupId, userId) {
    var hasManagerRole = [GROUP_CREATOR, GROUP_MANAGER];
    return GroupMember.findOne({
      where: {
        groupId: groupId,
        memberId: userId
      },
      attributes: ['role']
    }).then(function (result) {
      if (!result) {
        return false;
      }
      var role = result.role;
      return hasManagerRole.indexOf(role) !== -1;
    });
  },
  getGroupVerify: function (groupId, requesterId) {
    if (requesterId) {
      return GroupMember.findOne({
        where: {
          groupId: groupId,
          memberId: requesterId
        },
        include: {
          model: Group,
          as: 'group',
          attributes: ['id', 'name', 'portraitUri', 'certiStatus']
        }
      }).then(function (result) {
        var isVerify = result && result.group &&
          result.group.certiStatus == ENUM.CertiStatus.OPENED &&
          ENUM.OptPermissionRole.indexOf(result.role) === -1;
        return {
          isVerify: isVerify,
          group: result ? result.group : {}
        };
      });
    } else {
      return Group.findOne({
        where: {
          id: groupId
        },
        attributes: ['id', 'name', 'portraitUri', 'certiStatus']
      }).then(function (result) {
        var isVerify = result && result.certiStatus == ENUM.CertiStatus.OPENED;
        return {
          isVerify: isVerify,
          group: result ? result : {}
        }
      });
    }
  },
  hasGroupVerify: function (groupId, requesterId) {
    if (requesterId) {
      return GroupMember.findOne({
        where: {
          groupId: groupId,
          memberId: requesterId
        },
        include: {
          model: Group,
          as: 'group',
          attributes: ['id', 'name', 'certiStatus']
        }
      }).then(function (result) {
        return result && result.group &&
          result.group.certiStatus == ENUM.CertiStatus.OPENED &&
          ENUM.OptPermissionRole.indexOf(result.role) === -1;
      });
    } else {
      return Group.findOne({
        where: {
          id: groupId
        },
        attributes: ['certiStatus']
      }).then(function (result) {
        return result && result.certiStatus == ENUM.CertiStatus.OPENED;;
      });
    }
  }
};

var groupBulletinMethods = {
  createBulletin: function (groupId, content) {
    return GroupBulletin.create({
      groupId: groupId,
      content: content,
      timestamp: Date.now()
    });
  },
  getBulletin: function (groupId) {
    return GroupBulletin.findOne({
      order: [
        ['timestamp', 'DESC']
      ],
      where: {
        groupId: groupId
      },
      attributes: ['id', 'groupId', 'content', 'timestamp']
    });
  }
};

var groupReceiverMethods = {
  deleteByGroup: function (groupId, userId) {
    return GroupReceiver.destroy({
      where: {
        groupId: groupId,
        userId: userId
      }
    });
  },
  deleteByGroupManager: function (groupId, managerIds) {
    return GroupReceiver.destroy({
      where: {
        groupId: groupId,
        userId: {
          $or: managerIds
        }
      }
    });
  },
  addDeleteUser: function (deleteUsers, userId) {
    var deletedUserList = groupReceiverMethods.getDeletedUserList(deleteUsers);
    deletedUserList.push(userId);
    return JSON.stringify(deletedUserList);
  },
  getDeletedUserList: function (deleteUsers) {
    var deletedUserList = [];
    try {
      deletedUserList = JSON.parse(deleteUsers);
    } catch(e) {

    }
    return deletedUserList;
  },
  deleteByUser: function (receiverList, userId) {
    return sequelize.transaction(function (t) {
      return sequelize.Promise.each(receiverList, function (receiver) {
        var deletedUsers = receiver.deletedUsers;
        deleteUsers = groupReceiverMethods.addDeleteUser(deletedUsers, userId);
        return GroupReceiver.update({
          deletedUsers: deleteUsers
        }, {
            where: {
              id: receiver.id
            },
            transaction: t
          });
      });
    });
  },
  updateToExpired: function (groupId, memberIds) {
    return GroupReceiver.update({
      status: ENUM.GroupReceiverStatus.EXPIRED
    }, {
      where: {
        groupId: groupId,
        receiverId: {
          $in: memberIds
        },
        status: ENUM.GroupReceiverStatus.WAIT
      }
    });
  },
  bulkUpsert: function (group, requesterId, receiverIdList, operatorList, type, status) {
    var groupId = group.id;
    var updateReceiverIdList = [],
      createReceiverList = [];
    var optMapReceiver = [];
    operatorList.forEach(function (userId) {
      optMapReceiver.push({
        userId: userId,
        receiverId: {
          $in: receiverIdList
        }
      });
    });
    var where = {
      groupId: groupId,
      // requesterId: requesterId,
      type: type,
      $or: optMapReceiver
    };
    if (type === ENUM.GroupReceiverType.MANAGER) {
      where.requesterId = requesterId;
    }
    return GroupReceiver.findAll({
      where: where,
      attributes: ['receiverId']
    }).then(function (list) {
      list.forEach(function (receiver) {
        updateReceiverIdList.push(receiver.receiverId);
      });
      receiverIdList.forEach(function (receiverId) {
        var groupReceiver = {
          userId: receiverId,
          groupId: groupId,
          groupName: group.name,
          groupPortraitUri: group.portraitUri,
          requesterId: requesterId,
          receiverId: receiverId,
          status: status,
          type: type,
          isRead: 0,
          timestamp: Date.now()
        };
        if (updateReceiverIdList.indexOf(receiverId) === -1) {
          if (type === ENUM.GroupReceiverType.MEMBER) {
            createReceiverList.push(utils.parse(groupReceiver));
          } else {
            operatorList.forEach(function (userId) {
              groupReceiver.userId = userId;
              createReceiverList.push(utils.parse(groupReceiver));
            });
          }
        }
      });
      console.log('update params', {
        groupId: groupId,
        requesterId: requesterId,
        type: type,
        $or: optMapReceiver
      });
      var updateData = {
        status: status,
        timestamp: Date.now()
      }
      if (type === ENUM.GroupReceiverType.MEMBER) {
        updateData.requesterId = requesterId;
      }
      return GroupReceiver.update(updateData, {
        where: where
      });
    }).then(function () {
      return GroupReceiver.bulkCreate(createReceiverList);
    });
  },
  batchDetail: function (where) {
    var receiverQueryInclude = [{
      model: User,
      as: 'requester',
      attributes: ['id', 'nickname']
    }, {
      model: User,
      as: 'receiver',
      attributes: ['id', 'nickname']
    }, {
      model: Group,
      as: 'group',
      attributes: ['id', 'name']
    }];
    return GroupReceiver.findAll({
      where: where,
      attributes: ['id', 'groupId', 'groupName', 'type', 'status', 'deletedUsers', 'timestamp'],
      order: [
        ['timestamp', 'DESC']
      ],
      include: receiverQueryInclude
    }).then(function (list) {
      return list.map(function (item) {
        item = item.dataValues;
        item.group = {
          name: item.groupName,
          id: item.groupId
        };
        delete item.groupName;
        delete item.groupId;
        return item;
      });
    });
  }
};

var groupFavMethods = {
  createFav: function (userId, groupId) {
    return GroupFav.create({
      userId: userId,
      groupId: groupId
    });
  },
  deleteFac: function (groupId, userIdList) {
    if (!groupId && !userId) {
      return Promise.resolve();
    }
    var deleteCondition = {};
    if (groupId) {
      deleteCondition.groupId = groupId;
    }
    if (userIdList) {
      deleteCondition.userId = userIdList;
    }
    return GroupFav.destroy({
      where: deleteCondition
    });
  },
  hasGroup: function (userId, groupId, callback) {
    return GroupFav.findOne({
      where: {
        userId: userId,
        groupId: groupId
      }
    }).then(function (fav) {
      return fav ? Promise.resolve(fav) : Promise.reject();
    });
  },
  getGroups: function (userId, limit, offset) {
    var findParams = {
      where: { userId: userId },
      include: {
        model: Group,
        attributes: ['id', 'name', 'portraitUri', 'creatorId','isMute', 'certiStatus', 'memberCount', 'maxMemberCount', 'createdAt', 'updatedAt']
      }
    };
    if (utils.isInt(offset) && utils.isInt(limit)) {
      findParams = Object.assign(findParams, {
        limit: parseInt(limit),
        offset: parseInt(offset)
      });
    }
    return GroupFav.findAndCountAll(findParams).then(function (result) {
      var favList = result.rows.filter(function (fav) {
        return !!fav.group;
      });
      var groupList = favList.map(function (fav) {
        fav = Utility.encodeResults(fav, [['group', 'id'], ['group', 'creatorId']]);
        return fav.group;
      });
      return Promise.resolve({
        list: addUpdateTimeToList(groupList),
        total: result.count,
        limit: limit,
        offset: offset
      });
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
              // throw new HTTPError('Should not add exist member to the group.', 400);
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
  gender: {
    type: Sequelize.STRING(32),
    allowNull: false,
    defaultValue: 'male'
  },
  stAccount: {
    type: Sequelize.STRING(32),
    allowNull: false,
    defaultValue: '',
  },
  phoneVerify: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    defaultValue: 1
  },
  stSearchVerify: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    defaultValue: 1
  },
  friVerify: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    defaultValue: 1
  },
  groupVerify: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    defaultValue: 1
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
    comment: '10: 请求, 11: 被请求, 20: 同意, 21: 忽略, 30: 被删除, 31: 被拉黑'
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
  certiStatus: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    defaultValue: 1
  },
  isMute: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    defaultValue: 0
  },
  clearStatus: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    defaultValue: 0
  },
  clearTimeAt: {
    type: Sequelize.BIGINT.UNSIGNED,
    allowNull: false,
    defaultValue: 0,
    comment: '设置清除时间'
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

GroupFav = sequelize.define('group_fav', {
  id: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true,
    autoIncrement: true
  },
  userId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    unique: 'groupfavindex'
  },
  groupId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    unique: 'groupfavindex'
  }
}, {
  classMethods: groupFavMethods
});

GroupFav.belongsTo(Group, {
  foreignKey: 'groupId',
  constraints: false
});

GroupBulletin = sequelize.define('group_bulletin', {
  id: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true,
    autoIncrement: true
  },
  groupId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  content: {
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
  classMethods: groupBulletinMethods
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


VerificationViolation = sequelize.define('verification_violations', {
  ip: {
    type: Sequelize.STRING(64),
    allowNull: false,
    primaryKey: true
  },
  time: {
    type: Sequelize.DATE,
    allowNull: false
  },
  count: {
    type: Sequelize.INTEGER.UNSIGNED
  }
}, {
  timestamps: false
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

GroupReceiver = sequelize.define('group_receiver', {
  id: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true,
    autoIncrement: true
  },
  userId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  groupId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  groupName: {
    type: Sequelize.STRING(32),
    allowNull: false,
    defaultValue: ''
  },
  groupPortraitUri: {
    type: Sequelize.STRING(256),
    allowNull: false,
    defaultValue: ''
  },
  requesterId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  receiverId: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  type: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    comment: '0: 管理者审核、1: 被邀请者审核'
  },
  status: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    comment: '0: 已忽略、1: 已同意、2: 待审核'
  },
  deletedUsers: {
    type: Sequelize.STRING(256),
    allowNull: false,
    defaultValue: ''
  },
  isRead: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  joinInfo: {
    type: Sequelize.STRING(256),
    allowNull: false,
    defaultValue: ''
  },
  timestamp: {
    type: Sequelize.BIGINT,
    allowNull: false,
    defaultValue: 0,
    comment: '时间戳（版本号）'
  }
},{
  classMethods: groupReceiverMethods
});

GroupReceiver.belongsTo(User, {
  foreignKey: 'requesterId',
  targetKey: 'id',
  as: 'requester'
});

GroupReceiver.belongsTo(User, {
  foreignKey: 'receiverId',
  targetKey: 'id',
  as: 'receiver'
});

GroupReceiver.belongsTo(Group, {
  foreignKey: 'groupId',
  targetKey: 'id',
  as: 'group'
});

ScreenStatus = sequelize.define('screen_statuses',{
  id: {
    type: Sequelize.INTEGER.UNSIGNED,
    primaryKey: true,
    autoIncrement: true
  },
  operateId: {
    type: Sequelize.CHAR(40),
    allowNull: false
  },
  conversationType: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false
  },
  status: {
    type: Sequelize.INTEGER.UNSIGNED,
    allowNull: false,
    comment: '截屏通知： 0 关闭 1 开启'
  }
}, {
  indexes: [
    {
      unique: true,
      fields: ['operateId']
    }
  ]
});

// var salt = Utility.random(1000, 9999);
// var hash = Utility.hash('123456', salt);
// var test = {
//   region: '86',
//   phone: 16333100000,
//   nickname: '',
//   passwordHash: hash,
//   passwordSalt: salt.toString()
// };
// var arr = [];
// for (var i = 0; i < 10000; i++) {
//   var t = JSON.parse(JSON.stringify(test));
//   t.phone = t.phone + i;
//   t.nickname = i;
//   arr.push(t);
// }

// var phoneArr = arr.map(function (item) {
//   return item.phone;
// });

// User.bulkCreate(arr).then(function (result) {
//   console.log(result.length);
//   console.log('success');
// }).catch(function (e) {
//   // console.log('error', e);
// })

module.exports = [sequelize, User, Blacklist, Friendship, Group, GroupMember, GroupSync, DataVersion, VerificationCode, LoginLog, VerificationViolation, GroupFav, GroupBulletin, GroupReceiver, ScreenStatus];
