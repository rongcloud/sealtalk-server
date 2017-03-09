var APIResult, Blacklist, Cache, Config, DEFAULT_MAX_GROUP_MEMBER_COUNT, DataVersion, Friendship, GROUP_BULLETIN_MAX_LENGTH, GROUP_CREATOR, GROUP_MEMBER, GROUP_MEMBER_DISPLAY_NAME_MAX_LENGTH, GROUP_NAME_MAX_LENGTH, GROUP_NAME_MIN_LENGTH, GROUP_OPERATION_ADD, GROUP_OPERATION_BULLETIN, GROUP_OPERATION_CREATE, GROUP_OPERATION_DISMISS, GROUP_OPERATION_KICKED, GROUP_OPERATION_QUIT, GROUP_OPERATION_RENAME, GROUP_OPERATION_TRANSFER, Group, GroupMember, GroupSync, HTTPError, LoginLog, MAX_USER_GROUP_OWN_COUNT, PORTRAIT_URI_MAX_LENGTH, PORTRAIT_URI_MIN_LENGTH, Session, User, Utility, VerificationCode, _, co, express, ref, rongCloud, router, sendGroupNotification, sequelize, validator;

express = require('express');

co = require('co');

_ = require('underscore');

rongCloud = require('rongcloud-sdk');

Config = require('../conf');

Cache = require('../util/cache');

Session = require('../util/session');

Utility = require('../util/util').Utility;

APIResult = require('../util/util').APIResult;

HTTPError = require('../util/util').HTTPError;

ref = require('../db'), sequelize = ref[0], User = ref[1], Blacklist = ref[2], Friendship = ref[3], Group = ref[4], GroupMember = ref[5], GroupSync = ref[6], DataVersion = ref[7], VerificationCode = ref[8], LoginLog = ref[9];

GROUP_CREATOR = 0;

GROUP_MEMBER = 1;

GROUP_NAME_MIN_LENGTH = 2;

GROUP_NAME_MAX_LENGTH = 32;

GROUP_BULLETIN_MAX_LENGTH = 1024;

PORTRAIT_URI_MIN_LENGTH = 12;

PORTRAIT_URI_MAX_LENGTH = 256;

GROUP_MEMBER_DISPLAY_NAME_MAX_LENGTH = 32;

DEFAULT_MAX_GROUP_MEMBER_COUNT = 500;

MAX_USER_GROUP_OWN_COUNT = 500;

GROUP_OPERATION_CREATE = 'Create';

GROUP_OPERATION_ADD = 'Add';

GROUP_OPERATION_QUIT = 'Quit';

GROUP_OPERATION_DISMISS = 'Dismiss';

GROUP_OPERATION_KICKED = 'Kicked';

GROUP_OPERATION_RENAME = 'Rename';

GROUP_OPERATION_BULLETIN = 'Bulletin';

GROUP_OPERATION_TRANSFER = 'Transfer';

rongCloud.init(Config.RONGCLOUD_APP_KEY, Config.RONGCLOUD_APP_SECRET);

sendGroupNotification = function(userId, groupId, operation, data) {
  var encodedGroupId, encodedUserId, groupNotificationMessage;
  encodedUserId = Utility.encodeId(userId);
  encodedGroupId = Utility.encodeId(groupId);
  data.data = JSON.parse(JSON.stringify(data));
  groupNotificationMessage = {
    operatorUserId: encodedUserId,
    operation: operation,
    data: data,
    message: ''
  };
  Utility.log('Sending GroupNotificationMessage:', JSON.stringify(groupNotificationMessage));
  return new Promise(function(resolve, reject) {
    return rongCloud.message.group.publish('__system__', encodedGroupId, 'RC:GrpNtf', groupNotificationMessage, function(err, resultText) {
      if (err) {
        Utility.logError('Error: send group notification failed: %s', err);
        reject(err);
      }
      return resolve(resultText);
    });
  });
};

router = express.Router();

validator = sequelize.Validator;

router.post('/create', function(req, res, next) {
  var currentUserId, encodedMemberIds, memberIds, name, timestamp;
  name = Utility.xss(req.body.name, GROUP_NAME_MAX_LENGTH);
  memberIds = req.body.memberIds;
  encodedMemberIds = req.body.encodedMemberIds;
  Utility.log('memberIds', memberIds);
  Utility.log('encodedMemberIds', encodedMemberIds);
  if (!validator.isLength(name, GROUP_NAME_MIN_LENGTH, GROUP_NAME_MAX_LENGTH)) {
    return res.status(400).send('Length of group name is out of limit.');
  }
  if (memberIds.length === 1) {
    return res.status(400).send("Group's member count should be greater than 1 at least.");
  }
  if (memberIds.length > DEFAULT_MAX_GROUP_MEMBER_COUNT) {
    return res.status(400).send("Group's member count is out of max group member count limit (" + DEFAULT_MAX_GROUP_MEMBER_COUNT + ").");
  }
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  return GroupMember.getGroupCount(currentUserId).then(function(count) {
    if (count === MAX_USER_GROUP_OWN_COUNT) {
      return res.send(new APIResult(1000, null, "Current user's group count is out of max user group count limit (" + MAX_USER_GROUP_OWN_COUNT + ")."));
    }
    return sequelize.transaction(function(t) {
      return co(function*() {
        var group;
        group = (yield Group.create({
          name: name,
          portraitUri: '',
          memberCount: memberIds.length,
          creatorId: currentUserId,
          timestamp: timestamp
        }, {
          transaction: t
        }));
        Utility.log('Group %s created by %s', group.id, currentUserId);
        (yield GroupMember.bulkUpsert(group.id, memberIds, timestamp, t, currentUserId));
        return group;
      });
    }).then(function(group) {
      return DataVersion.updateGroupMemberVersion(group.id, timestamp).then(function() {
        rongCloud.group.create(encodedMemberIds, Utility.encodeId(group.id), name, function(err, resultText) {
          var result, success;
          if (err) {
            Utility.logError('Error: create group failed on IM server, error: %s', err);
          }
          result = JSON.parse(resultText);
          success = result.code === 200;
          if (success) {
            return Session.getCurrentUserNickname(currentUserId, User).then(function(nickname) {
              return sendGroupNotification(currentUserId, group.id, GROUP_OPERATION_CREATE, {
                operatorNickname: nickname,
                targetGroupName: name,
                timestamp: timestamp
              });
            });
          } else {
            Utility.logError('Error: create group failed on IM server, code: %s', result.code);
            return GroupSync.upsert({
              syncInfo: success,
              syncMember: success
            }, {
              where: {
                groupId: group.id
              }
            });
          }
        });
        memberIds.forEach(function(memberId) {
          return Cache.del("user_groups_" + memberId);
        });
        return res.send(new APIResult(200, Utility.encodeResults({
          id: group.id
        })));
      });
    });
  })["catch"](next);
});

router.post('/add', function(req, res, next) {
  var currentUserId, encodedGroupId, encodedMemberIds, groupId, memberIds, timestamp;
  groupId = req.body.groupId;
  memberIds = req.body.memberIds;
  encodedGroupId = req.body.encodedGroupId;
  encodedMemberIds = req.body.encodedMemberIds;
  Utility.log('Group %s add members %j by user %s', groupId, memberIds, Session.getCurrentUserId(req));
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  return Group.getInfo(groupId).then(function(group) {
    var memberCount;
    if (!group) {
      return res.status(404).send('Unknown group.');
    }
    memberCount = group.memberCount + memberIds.length;
    if (memberCount > group.maxMemberCount) {
      return res.status(400).send("Group's member count is out of max group member count limit (" + group.maxMemberCount + ").");
    }
    return sequelize.transaction(function(t) {
      return Promise.all([
        Group.update({
          memberCount: memberCount,
          timestamp: timestamp
        }, {
          where: {
            id: groupId
          },
          transaction: t
        }), GroupMember.bulkUpsert(groupId, memberIds, timestamp, t)
      ]);
    }).then(function() {
      return DataVersion.updateGroupMemberVersion(groupId, timestamp).then(function() {
        rongCloud.group.join(encodedMemberIds, encodedGroupId, group.name, function(err, resultText) {
          var result, success;
          if (err) {
            Utility.logError('Error: join group failed on IM server, error: %s', err);
          }
          result = JSON.parse(resultText);
          success = result.code === 200;
          if (success) {
            return User.getNicknames(memberIds).then(function(nicknames) {
              return Session.getCurrentUserNickname(currentUserId, User).then(function(nickname) {
                return sendGroupNotification(currentUserId, groupId, GROUP_OPERATION_ADD, {
                  operatorNickname: nickname,
                  targetUserIds: encodedMemberIds,
                  targetUserDisplayNames: nicknames,
                  timestamp: timestamp
                });
              });
            });
          } else {
            Utility.logError('Error: join group failed on IM server, code: %s', result.code);
            return GroupSync.upsert({
              syncMember: true
            }, {
              where: {
                groupId: group.id
              }
            });
          }
        });
        memberIds.forEach(function(memberId) {
          return Cache.del("user_groups_" + memberId);
        });
        Cache.del("group_" + groupId);
        Cache.del("group_members_" + groupId);
        return res.send(new APIResult(200));
      });
    });
  })["catch"](next);
});

router.post('/join', function(req, res, next) {
  var currentUserId, encodedGroupId, groupId, timestamp;
  groupId = req.body.groupId;
  encodedGroupId = req.body.encodedGroupId;
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  return Group.getInfo(groupId).then(function(group) {
    var memberCount;
    if (!group) {
      return res.status(404).send('Unknown group.');
    }
    memberCount = group.memberCount + 1;
    if (memberCount > group.maxMemberCount) {
      return res.status(400).send("Group's member count is out of max group member count limit (" + group.maxMemberCount + ").");
    }
    return sequelize.transaction(function(t) {
      return Promise.all([
        Group.update({
          memberCount: memberCount,
          timestamp: timestamp
        }, {
          where: {
            id: groupId
          },
          transaction: t
        }), GroupMember.bulkUpsert(groupId, [currentUserId], timestamp, t)
      ]);
    }).then(function() {
      return DataVersion.updateGroupMemberVersion(groupId, timestamp).then(function() {
        var encodedIds;
        encodedIds = [Utility.encodeId(currentUserId)];
        rongCloud.group.join(encodedIds, encodedGroupId, group.name, function(err, resultText) {
          var result, success;
          if (err) {
            Utility.logError('Error: join group failed on IM server, error: %s', err);
          }
          result = JSON.parse(resultText);
          success = result.code === 200;
          if (success) {
            return Session.getCurrentUserNickname(currentUserId, User).then(function(nickname) {
              return sendGroupNotification(currentUserId, groupId, GROUP_OPERATION_ADD, {
                operatorNickname: nickname,
                targetUserIds: encodedIds,
                targetUserDisplayNames: [nickname],
                timestamp: timestamp
              });
            });
          } else {
            Utility.logError('Error: join group failed on IM server, code: %s', result.code);
            return GroupSync.upsert({
              syncMember: true
            }, {
              where: {
                groupId: group.id
              }
            });
          }
        });
        Cache.del("user_groups_" + currentUserId);
        Cache.del("group_" + groupId);
        Cache.del("group_members_" + groupId);
        return res.send(new APIResult(200));
      });
    });
  })["catch"](next);
});

router.post('/kick', function(req, res, next) {
  var currentUserId, encodedGroupId, encodedMemberIds, groupId, memberIds, timestamp;
  groupId = req.body.groupId;
  memberIds = req.body.memberIds;
  encodedGroupId = req.body.encodedGroupId;
  encodedMemberIds = req.body.encodedMemberIds;
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  if (_.contains(memberIds, currentUserId)) {
    return res.status(400).send('Can not kick yourself.');
  }
  return Group.getInfo(groupId).then(function(group) {
    if (!group) {
      return res.status(404).send('Unknown group.');
    }
    if (group.creatorId !== currentUserId) {
      return res.status(403).send('Current user is not group creator.');
    }
    return GroupMember.findAll({
      where: {
        groupId: groupId
      },
      attributes: ['memberId']
    }).then(function(groupMembers) {
      var emptyMemberIdFlag, isKickNonMember;
      if (groupMembers.length === 0) {
        throw new Error('Group member should not be empty, please check your database.');
      }
      isKickNonMember = false;
      emptyMemberIdFlag = false;
      memberIds.forEach(function(memberId) {
        if (Utility.isEmpty(memberId)) {
          emptyMemberIdFlag = true;
        }
        return isKickNonMember = groupMembers.every(function(member) {
          return memberId !== member.memberId;
        });
      });
      if (emptyMemberIdFlag) {
        return res.status(400).send('Empty memberId.');
      }
      if (isKickNonMember) {
        return res.status(400).send('Can not kick none-member from the group.');
      }
      return User.getNicknames(memberIds).then(function(nicknames) {
        return Session.getCurrentUserNickname(currentUserId, User).then(function(nickname) {
          return sendGroupNotification(currentUserId, groupId, GROUP_OPERATION_KICKED, {
            operatorNickname: nickname,
            targetUserIds: encodedMemberIds,
            targetUserDisplayNames: nicknames,
            timestamp: timestamp
          }).then(function() {
            return rongCloud.group.quit(encodedMemberIds, encodedGroupId, function(err, resultText) {
              var result, success;
              if (err) {
                Utility.logError('Error: quit group failed on IM server, error: %s', err);
              }
              result = JSON.parse(resultText);
              success = result.code === 200;
              if (!success) {
                Utility.logError('Error: quit group failed on IM server, code: %s', result.code);
                return res.status(500).send('Quit failed on IM server.');
              }
              return sequelize.transaction(function(t) {
                return Promise.all([
                  Group.update({
                    memberCount: group.memberCount - memberIds.length,
                    timestamp: timestamp
                  }, {
                    where: {
                      id: groupId
                    },
                    transaction: t
                  }), GroupMember.update({
                    isDeleted: true,
                    timestamp: timestamp
                  }, {
                    where: {
                      groupId: groupId,
                      memberId: {
                        $in: memberIds
                      }
                    },
                    transaction: t
                  })
                ]);
              }).then(function() {
                return DataVersion.updateGroupMemberVersion(groupId, timestamp).then(function() {
                  memberIds.forEach(function(memberId) {
                    return Cache.del("user_groups_" + memberId);
                  });
                  Cache.del("group_" + groupId);
                  Cache.del("group_members_" + groupId);
                  return res.send(new APIResult(200));
                });
              });
            });
          });
        });
      });
    });
  })["catch"](next);
});

router.post('/quit', function(req, res, next) {
  var currentUserId, encodedGroupId, groupId, timestamp;
  groupId = req.body.groupId;
  encodedGroupId = req.body.encodedGroupId;
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  return Group.getInfo(groupId).then(function(group) {
    if (!group) {
      return res.status(404).send('Unknown group.');
    }
    return GroupMember.findAll({
      where: {
        groupId: groupId
      },
      attributes: ['memberId']
    }).then(function(groupMembers) {
      var encodedMemberIds, isInGroup, newCreatorId;
      isInGroup = groupMembers.some(function(groupMember) {
        return groupMember.memberId === currentUserId;
      });
      if (!isInGroup) {
        return res.status(403).send('Current user is not group member.');
      }
      newCreatorId = null;
      if (group.creatorId === currentUserId && groupMembers.length > 1) {
        groupMembers.some(function(groupMember) {
          if (groupMember.memberId !== currentUserId) {
            newCreatorId = groupMember.memberId;
            return true;
          } else {
            return false;
          }
        });
      }
      encodedMemberIds = [Utility.encodeId(currentUserId)];
      return Session.getCurrentUserNickname(currentUserId, User).then(function(nickname) {
        return sendGroupNotification(currentUserId, groupId, GROUP_OPERATION_QUIT, {
          operatorNickname: nickname,
          targetUserIds: encodedMemberIds,
          targetUserDisplayNames: [nickname],
          newCreatorId: newCreatorId,
          timestamp: timestamp
        }).then(function() {
          return rongCloud.group.quit(encodedMemberIds, encodedGroupId, function(err, resultText) {
            var result, resultMessage, success;
            if (err) {
              Utility.logError('Error: quit group failed on IM server, error: %s', err);
            }
            result = JSON.parse(resultText);
            success = result.code === 200;
            if (!success) {
              Utility.logError('Error: quit group failed on IM server, code: %s', result.code);
              return res.status(500).send('Quit failed on IM server.');
            }
            resultMessage = null;
            return sequelize.transaction(function(t) {
              if (group.creatorId !== currentUserId) {
                resultMessage = 'Quit.';
                return Promise.all([
                  Group.update({
                    memberCount: group.memberCount - 1,
                    timestamp: timestamp
                  }, {
                    where: {
                      id: groupId
                    },
                    transaction: t
                  }), GroupMember.update({
                    isDeleted: true,
                    timestamp: timestamp
                  }, {
                    where: {
                      groupId: groupId,
                      memberId: currentUserId
                    },
                    transaction: t
                  })
                ]);
              } else if (group.memberCount > 1) {
                resultMessage = 'Quit and group owner transfered.';
                return Promise.all([
                  Group.update({
                    memberCount: group.memberCount - 1,
                    creatorId: newCreatorId,
                    timestamp: timestamp
                  }, {
                    where: {
                      id: groupId
                    },
                    transaction: t
                  }), GroupMember.update({
                    role: GROUP_MEMBER,
                    isDeleted: true,
                    timestamp: timestamp
                  }, {
                    where: {
                      groupId: groupId,
                      memberId: currentUserId
                    },
                    transaction: t
                  }), GroupMember.update({
                    role: GROUP_CREATOR,
                    timestamp: timestamp
                  }, {
                    where: {
                      groupId: groupId,
                      memberId: newCreatorId
                    },
                    transaction: t
                  })
                ]);
              } else {
                resultMessage = 'Quit and group dismissed.';
                return Promise.all([
                  Group.update({
                    memberCount: 0,
                    timestamp: timestamp
                  }, {
                    where: {
                      id: groupId
                    },
                    transaction: t
                  }), Group.destroy({
                    where: {
                      id: groupId
                    },
                    transaction: t
                  }), GroupMember.update({
                    isDeleted: true,
                    timestamp: timestamp
                  }, {
                    where: {
                      groupId: groupId
                    },
                    transaction: t
                  })
                ]);
              }
            }).then(function() {
              return DataVersion.updateGroupMemberVersion(groupId, timestamp).then(function() {
                Cache.del("user_groups_" + currentUserId);
                Cache.del("group_" + groupId);
                Cache.del("group_members_" + groupId);
                return res.send(new APIResult(200, null, resultMessage));
              });
            });
          });
        });
      });
    });
  })["catch"](next);
});

router.post('/dismiss', function(req, res, next) {
  var currentUserId, encodedGroupId, groupId, timestamp;
  groupId = req.body.groupId;
  encodedGroupId = req.body.encodedGroupId;
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  return Session.getCurrentUserNickname(currentUserId, User).then(function(nickname) {
    return sendGroupNotification(currentUserId, groupId, GROUP_OPERATION_DISMISS, {
      operatorNickname: nickname,
      timestamp: timestamp
    }).then(function() {
      return rongCloud.group.dismiss(Utility.encodeId(currentUserId), encodedGroupId, function(err, resultText) {
        var result, success;
        if (err) {
          Utility.logError('Error: dismiss group failed on IM server, error: %s', err);
        }
        result = JSON.parse(resultText);
        success = result.code === 200;
        if (!success) {
          Utility.logError('Error: dismiss group failed on IM server, code: %s', result.code);
          return res.send(new APIResult(500, null, 'Quit failed on IM server.'));
          GroupSync.upsert({
            dismiss: true
          }, {
            where: {
              groupId: groupId
            }
          });
        }
        return sequelize.transaction(function(t) {
          return Group.update({
            memberCount: 0
          }, {
            where: {
              id: groupId,
              creatorId: currentUserId
            },
            transaction: t
          }).then(function(arg) {
            var affectedCount;
            affectedCount = arg[0];
            Utility.log('affectedCount', affectedCount);
            if (affectedCount === 0) {
              throw new HTTPError('Unknown group or not creator.', 400);
            }
            return Promise.all([
              Group.destroy({
                where: {
                  id: groupId
                },
                transaction: t
              }), GroupMember.update({
                isDeleted: true,
                timestamp: timestamp
              }, {
                where: {
                  groupId: groupId
                },
                transaction: t
              })
            ]);
          });
        }).then(function() {
          return DataVersion.updateGroupMemberVersion(groupId, timestamp).then(function() {
            GroupMember.unscoped().findAll({
              where: {
                groupId: groupId
              },
              attributes: ['memberId']
            }).then(function(members) {
              return members.forEach(function(member) {
                return Cache.del("user_groups_" + member.memberId);
              });
            });
            Cache.del("group_" + groupId);
            Cache.del("group_members_" + groupId);
            return res.send(new APIResult(200));
          });
        })["catch"](function(err) {
          if (err instanceof HTTPError) {
            return res.status(err.statusCode).send(err.message);
          }
        });
      });
    });
  })["catch"](next);
});

router.post('/transfer', function(req, res, next) {
  var currentUserId, encodedUserId, groupId, timestamp, userId;
  groupId = req.body.groupId;
  userId = req.body.userId;
  encodedUserId = req.body.encodedUserId;
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  if (userId === currentUserId) {
    return res.status(403).send('Can not transfer creator role to yourself.');
  }
  return GroupMember.findAll({
    where: {
      groupId: groupId,
      memberId: {
        $in: [currentUserId, userId]
      }
    },
    order: 'role ASC',
    attributes: ['memberId', 'role']
  }).then(function(groupMembers) {
    if (groupMembers.length !== 2) {
      return res.status(400).send('Invalid groupId or userId.');
    }
    if (groupMembers[0].memberId !== currentUserId || groupMembers[0].role !== GROUP_CREATOR) {
      return res.status(400).send('Current user is not group creator.');
    }
    return sequelize.transaction(function(t) {
      return Promise.all([
        Group.update({
          creatorId: userId,
          timestamp: timestamp
        }, {
          where: {
            id: groupId
          },
          transaction: t
        }), GroupMember.update({
          role: GROUP_MEMBER,
          timestamp: timestamp
        }, {
          where: {
            groupId: groupId,
            memberId: currentUserId
          },
          transaction: t
        }), GroupMember.update({
          role: GROUP_CREATOR,
          timestamp: timestamp
        }, {
          where: {
            groupId: groupId,
            memberId: userId
          },
          transaction: t
        })
      ]);
    }).then(function() {
      return DataVersion.updateGroupMemberVersion(groupId, timestamp).then(function() {
        Session.getCurrentUserNickname(currentUserId, User).then(function(currentUserNickname) {
          return User.getNickname(userId).then(function(nickname) {
            return sendGroupNotification(currentUserId, groupId, GROUP_OPERATION_TRANSFER, {
              oldCreatorId: Utility.encodeId(currentUserId),
              oldCreatorName: currentUserNickname,
              newCreatorId: encodedUserId,
              newCreatorName: nickname,
              timestamp: timestamp
            });
          });
        });
        Cache.del("group_" + groupId);
        Cache.del("group_members_" + groupId);
        return res.send(new APIResult(200));
      });
    });
  })["catch"](next);
});

router.post('/rename', function(req, res, next) {
  var currentUserId, encodedGroupId, groupId, name, timestamp;
  groupId = req.body.groupId;
  name = Utility.xss(req.body.name, GROUP_NAME_MAX_LENGTH);
  encodedGroupId = req.body.encodedGroupId;
  if (!validator.isLength(name, GROUP_NAME_MIN_LENGTH, GROUP_NAME_MAX_LENGTH)) {
    return res.status(400).send('Length of name invalid.');
  }
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  return Group.update({
    name: name,
    timestamp: timestamp
  }, {
    where: {
      id: groupId,
      creatorId: currentUserId
    }
  }).then(function(arg) {
    var affectedCount;
    affectedCount = arg[0];
    if (affectedCount === 0) {
      return res.status(400).send('Unknown group or not creator.');
    }
    return DataVersion.updateGroupVersion(groupId, timestamp).then(function() {
      rongCloud.group.refresh(encodedGroupId, name, function(err, resultText) {
        var result, success;
        if (err) {
          Utility.logError('Error: refresh group info failed on IM server, error: %s', err);
        }
        result = JSON.parse(resultText);
        success = result.code === 200;
        if (!success) {
          Utility.logError('Error: refresh group info failed on IM server, code: %s', result.code);
        }
        return GroupSync.upsert({
          syncInfo: true
        }, {
          where: {
            groupId: groupId
          }
        });
      });
      Session.getCurrentUserNickname(currentUserId, User).then(function(nickname) {
        return sendGroupNotification(currentUserId, groupId, GROUP_OPERATION_RENAME, {
          operatorNickname: nickname,
          targetGroupName: name,
          timestamp: timestamp
        });
      });
      GroupMember.findAll({
        where: {
          groupId: groupId
        },
        attributes: ['memberId']
      }).then(function(members) {
        return members.forEach(function(member) {
          return Cache.del("user_groups_" + member.memberId);
        });
      });
      Cache.del("group_" + groupId);
      return res.send(new APIResult(200));
    });
  })["catch"](next);
});

router.post('/set_bulletin', function(req, res, next) {
  var bulletin, currentUserId, groupId, timestamp;
  groupId = req.body.groupId;
  bulletin = Utility.xss(req.body.bulletin, GROUP_BULLETIN_MAX_LENGTH);
  if (!validator.isLength(bulletin, 0, GROUP_BULLETIN_MAX_LENGTH)) {
    return res.status(400).send('Length of bulletin invalid.');
  }
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  return Group.update({
    bulletin: bulletin,
    timestamp: timestamp
  }, {
    where: {
      id: groupId,
      creatorId: currentUserId
    }
  }).then(function(arg) {
    var affectedCount;
    affectedCount = arg[0];
    if (affectedCount === 0) {
      return res.status(400).send('Unknown group or not creator.');
    }
    return DataVersion.updateGroupVersion(groupId, timestamp).then(function() {
      GroupMember.findAll({
        where: {
          groupId: groupId
        },
        attributes: ['memberId']
      }).then(function(members) {
        return members.forEach(function(member) {
          return Cache.del("user_groups_" + member.memberId);
        });
      });
      Cache.del("group_" + groupId);
      return res.send(new APIResult(200));
    });
  })["catch"](next);
});

router.post('/set_portrait_uri', function(req, res, next) {
  var currentUserId, groupId, portraitUri, timestamp;
  groupId = req.body.groupId;
  portraitUri = Utility.xss(req.body.portraitUri, PORTRAIT_URI_MAX_LENGTH);
  if (!validator.isURL(portraitUri, {
    protocols: ['http', 'https'],
    require_protocol: true
  })) {
    return res.status(400).send('Invalid portraitUri format.');
  }
  if (!validator.isLength(portraitUri, PORTRAIT_URI_MIN_LENGTH, PORTRAIT_URI_MAX_LENGTH)) {
    return res.status(400).send('Length of portraitUri invalid.');
  }
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  return Group.update({
    portraitUri: portraitUri,
    timestamp: timestamp
  }, {
    where: {
      id: groupId,
      creatorId: currentUserId
    }
  }).then(function(arg) {
    var affectedCount;
    affectedCount = arg[0];
    if (affectedCount === 0) {
      return res.status(400).send('Unknown group or not creator.');
    }
    return DataVersion.updateGroupVersion(groupId, timestamp).then(function() {
      GroupMember.findAll({
        where: {
          groupId: groupId
        },
        attributes: ['memberId']
      }).then(function(members) {
        return members.forEach(function(member) {
          return Cache.del("user_groups_" + member.memberId);
        });
      });
      Cache.del("group_" + groupId);
      return res.send(new APIResult(200));
    });
  })["catch"](next);
});

router.post('/set_display_name', function(req, res, next) {
  var currentUserId, displayName, groupId, timestamp;
  groupId = req.body.groupId;
  displayName = Utility.xss(req.body.displayName, GROUP_MEMBER_DISPLAY_NAME_MAX_LENGTH);
  if (!validator.isLength(displayName, 0, GROUP_MEMBER_DISPLAY_NAME_MAX_LENGTH)) {
    return res.status(400).send('Length of display name invalid.');
  }
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  return GroupMember.update({
    displayName: displayName,
    timestamp: timestamp
  }, {
    where: {
      groupId: groupId,
      memberId: currentUserId
    }
  }).then(function(arg) {
    var affectedCount;
    affectedCount = arg[0];
    if (affectedCount === 0) {
      return res.status(404).send('Unknown group.');
    }
    return DataVersion.updateGroupMemberVersion(currentUserId, timestamp).then(function() {
      Cache.del("group_members_" + groupId);
      return res.send(new APIResult(200));
    });
  })["catch"](next);
});

router.get('/:id', function(req, res, next) {
  var currentUserId, groupId;
  groupId = req.params.id;
  groupId = Utility.decodeIds(groupId);
  currentUserId = Session.getCurrentUserId(req);
  return Cache.get("group_" + groupId).then(function(group) {
    if (group) {
      return res.send(new APIResult(200, group));
    } else {
      return Group.findById(groupId, {
        attributes: ['id', 'name', 'portraitUri', 'memberCount', 'maxMemberCount', 'creatorId', 'bulletin', 'deletedAt'],
        paranoid: false
      }).then(function(group) {
        var results;
        if (!group) {
          return res.status(404).send('Unknown group.');
        }
        results = Utility.encodeResults(group, ['id', 'creatorId']);
        Cache.set("group_" + groupId, results);
        return res.send(new APIResult(200, results));
      });
    }
  })["catch"](next);
});

router.get('/:id/members', function(req, res, next) {
  var currentUserId, groupId;
  groupId = req.params.id;
  groupId = Utility.decodeIds(groupId);
  currentUserId = Session.getCurrentUserId(req);
  return Cache.get("group_members_" + groupId).then(function(groupMembers) {
    var encodedCurrentUserId, isInGroup;
    if (groupMembers) {
      encodedCurrentUserId = Utility.encodeId(currentUserId);
      isInGroup = groupMembers.some(function(groupMember) {
        return groupMember.user.id === encodedCurrentUserId;
      });
      if (!isInGroup) {
        return res.status(403).send('Only group member can get group member info.');
      }
      return res.send(new APIResult(200, groupMembers));
    } else {
      return GroupMember.findAll({
        where: {
          groupId: groupId
        },
        attributes: ['displayName', 'role', 'createdAt', 'updatedAt'],
        include: {
          model: User,
          attributes: ['id', 'nickname', 'portraitUri']
        }
      }).then(function(groupMembers) {
        var results;
        if (groupMembers.length === 0) {
          return res.status(404).send('Unknown group.');
        }
        isInGroup = groupMembers.some(function(groupMember) {
          return groupMember.user.id === currentUserId;
        });
        if (!isInGroup) {
          return res.status(403).send('Only group member can get group member info.');
        }
        results = Utility.encodeResults(groupMembers, [['user', 'id']]);
        Cache.set("group_members_" + groupId, results);
        return res.send(new APIResult(200, results));
      });
    }
  })["catch"](next);
});

module.exports = router;
