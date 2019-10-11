var APIResult, Blacklist, Cache, Config, DEFAULT_MAX_GROUP_MEMBER_COUNT, DataVersion, Friendship, GROUP_BULLETIN_MAX_LENGTH, GROUP_MEMBER_DISPLAY_NAME_MAX_LENGTH, GROUP_NAME_MAX_LENGTH, GROUP_NAME_MIN_LENGTH, GROUP_OPERATION_ADD, GROUP_OPERATION_BULLETIN, GROUP_OPERATION_CREATE, GROUP_OPERATION_DISMISS, GROUP_OPERATION_KICKED, GROUP_OPERATION_QUIT, GROUP_OPERATION_RENAME, GROUP_OPERATION_TRANSFER, Group, GroupMember, GroupSync, HTTPError, LoginLog, MAX_USER_GROUP_OWN_COUNT, PORTRAIT_URI_MAX_LENGTH, PORTRAIT_URI_MIN_LENGTH, Session, User, Utility, VerificationCode, _, co, express, ref, rongCloud, router, sendGroupNotification, sequelize, validator;

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

var addUpdateTimeToList = require('../util/util').addUpdateTimeToList,
  addUpdateTime = require('../util/util').addUpdateTime;

var GroupReceiver = ref[13];

var ENUM = require('../util/enum');
var GroupRole = ENUM.GroupRole;
var ErrorENUM = ENUM.Error;
var GroupReceiverStatus = ENUM.GroupReceiverStatus;
var GROUP_CREATOR = GroupRole.CREATOR,
  GROUP_MEMBER = GroupRole.MEMBER,
  GROUP_MANAGER = GroupRole.MANAGER;

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

GROUP_OPERATION_SETMANAGER = 'SetManager';

GROUP_OPERATION_REMOVEMANAGER = 'RemoveManager';

GROUP_OPERATION_INVITE = 'Invite';

var GroupFav = ref[11];

var GroupBulletin = ref[12];

rongCloud.init(Config.RONGCLOUD_APP_KEY, Config.RONGCLOUD_APP_SECRET, {
  api: Config.RONGCLOUD_API_URL
});

var sendGroupApplyMessage = function (requesterId, operatorIdList, type, inviteStatus, groupId) {
  return new Promise(function (resolve, reject) {
    var requesterName, inviteeName;
    return Session.getCurrentUserNickname(requesterId, User).then(function (name) {
      requesterName = name;
      return Group.findById(groupId);
    }).then(function (group) {
      var content = {
        operatorUserId: Utility.encodeId(requesterId),
        operation: GROUP_OPERATION_INVITE,
        data: {
          operatorNickname: requesterName,
          targetGroupId: Utility.encodeId(groupId),
          targetGroupName: group ? group.name : '',
          status: inviteStatus,
          type: type,
          timestamp: +new Date()
        }
      };
      content = JSON.stringify(content);
      operatorIdList = Utility.encodeIds(operatorIdList);
      rongCloud.message.private.publishCus({
        fromUserId: '__group_apply__',
        objectName: 'ST:GrpApply',
        content: content
      }, [{ field: 'toUserId', values: operatorIdList }], function (err, resultText) {
        if (err) {
          reject(err);
          return Utility.logError('Error: send contact notification failed: %j', err);
        }
        resolve();
      });
    });
  });
};

var sendBulletinNotification = function (userId, groupId, content) {
  var encodedUserId = Utility.encodeId(userId),
    encodedGroupId = Utility.encodeId(groupId),
    objectName = 'RC:TxtMsg';
    content = {
      content: '@所有人 ' + content,
      mentionedInfo: {
        type: 1,
        userIdList: ['All']
      }
    };
  return new Promise(function (resolve, reject) {
    return rongCloud.message.group.publishCus({
      fromUserId: encodedUserId,
      toGroupId: encodedGroupId,
      objectName: objectName,
      content: JSON.stringify(content),
      isMentioned: 1,
      isIncludeSender: 1
    }, function (err, resultText) {
      if (err) {
        Utility.logError('Error: send group notification failed: %s', err);
        reject(err);
      }
      return resolve(resultText);
    })
    // return rongCloud.message.group.publish(encodedUserId, encodedGroupId, objectName, JSON.stringify(content), function (err, resultText) {
    //   if (err) {
    //     Utility.logError('Error: send group notification failed: %s', err);
    //     reject(err);
    //   }
    //   return resolve(resultText);
    // });
  });
};

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
    return rongCloud.message.group.publish('__system__', encodedGroupId, 'ST:GrpNtf', JSON.stringify(groupNotificationMessage), function(err, resultText) {
      if (err) {
        Utility.logError('Error: send group notification failed: %s', err);
        reject(err);
      }
      return resolve(resultText);
    });
  });
};

var upsertGroupReceiver = function (groupId, receiverId, operatorId, type, status) {
  return GroupReceiver.update({
    status: status
  }, {
      where: {
        groupId: groupId,
        receiverId: receiverId,
        type: type
      }
    }).then(function () {
      return sendGroupApplyMessage(operatorId, [operatorId], type, status, groupId);
    });
};

/* 已 operatorList 为 id 存储, 存储方式有两种: 接收者处理/管理者处理 */
var batchUpsertGroupReceiver = function (group, requesterId, receiverIdList, operatorList, type, status) {
  var groupId = group.id;
  return GroupReceiver.bulkUpsert(group, requesterId, receiverIdList, operatorList, type, status).then(function () {
    return sendGroupApplyMessage(requesterId, operatorList, type, status, groupId);
  });
};

var createGroup = function (memberIds, groupId, name) {
  return new Promise(function (resolve, reject) {
    rongCloud.group.create(memberIds, groupId, name, function (err, resultText) {
      var result, success;
      if (err) {
        return reject(err);
      }
      result = JSON.parse(resultText);
      resolve(result);
    });
  });
};

router = express.Router();

validator = sequelize.Validator;

router.post('/create', function(req, res, next) {
  var currentUserId, encodedMemberIds, memberIds, name, timestamp, portraitUri;
  name = Utility.xss(req.body.name, GROUP_NAME_MAX_LENGTH);
  memberIds = req.body.memberIds;
  encodedMemberIds = req.body.encodedMemberIds;
  portraitUri = req.body.portraitUri || '';
  Utility.log('memberIds', memberIds);
  Utility.log('encodedMemberIds', encodedMemberIds);
  currentUserId = Session.getCurrentUserId(req);

  var userStatus = [];

  var joinUserIds = memberIds.filter(function (memberId) {
    return memberId !== currentUserId
  });

  if (!validator.isLength(name, GROUP_NAME_MIN_LENGTH, GROUP_NAME_MAX_LENGTH)) {
    return res.status(400).send('Length of group name is out of limit.');
  }
  if (memberIds.length === 1) {
    return res.status(400).send("Group's member count should be greater than 1 at least.");
  }
  if (memberIds.length > DEFAULT_MAX_GROUP_MEMBER_COUNT) {
    return res.status(400).send("Group's member count is out of max group member count limit (" + DEFAULT_MAX_GROUP_MEMBER_COUNT + ").");
  }
  timestamp = Date.now();

  return GroupMember.getGroupCount(currentUserId).then(function (count) {
    if (count === MAX_USER_GROUP_OWN_COUNT) {
      return res.send(new APIResult(1000, null, "Current user's group count is out of max user group count limit (" + MAX_USER_GROUP_OWN_COUNT + ")."));
    }

    var verifyOpenedUserIdList = [];
    var verifyClosedUserIdList = [];
    var group;
    return User.batchGroupVerify(joinUserIds).then(function (result) {
      verifyOpenedUserIdList = result.opened;
      verifyClosedUserIdList = result.closed;
      joinUserIds = verifyClosedUserIdList.concat([currentUserId]);
      return Group.create({
        name: name,
        portraitUri: portraitUri,
        memberCount: joinUserIds.length,
        creatorId: currentUserId,
        timestamp: timestamp
      });
    }).then(function (groupDetail) {
      group = groupDetail;
      return sequelize.transaction(function (t) {
        return co(function* () {
          userStatus = userStatus.concat(joinUserIds.map(function (id) {
            return { id: Utility.encodeId(id), status: ENUM.GroupAddStatus.ADDED };
          }));
          (yield GroupMember.bulkUpsert(group.id, joinUserIds, timestamp, t, currentUserId));
          return group;
        });
      });
    }).then(function () {
      return DataVersion.updateGroupMemberVersion(group.id, timestamp);
    }).then(function () {
      var groupMemberIds = Utility.encodeIds(joinUserIds);
      return createGroup(groupMemberIds, Utility.encodeId(group.id), name);
    }).then(function (result) {
      var success = result.code === 200;
      if (success) {
        return Session.getCurrentUserNickname(currentUserId, User).then(function (nickname) {
          return sendGroupNotification(currentUserId, group.id, GROUP_OPERATION_CREATE, {
            operatorNickname: nickname,
            targetGroupName: name,
            timestamp: timestamp
          });
        });
      } else {
        return GroupSync.upsert({
          syncInfo: success,
          syncMember: success
        }, {
          where: {
            groupId: group.id
          }
        });
      }
    }).then(function () {
      if (verifyOpenedUserIdList.length) {
        userStatus = userStatus.concat(verifyOpenedUserIdList.map(function (id) {
          return { id: Utility.encodeId(id), status: ENUM.GroupAddStatus.WAIT_MEMBER };
        }));
        return batchUpsertGroupReceiver(group, currentUserId, verifyOpenedUserIdList, verifyOpenedUserIdList, ENUM.GroupReceiverType.MEMBER, GroupReceiverStatus.WAIT);
      }
      return Promise.resolve();
    }).then(function () {
      memberIds.forEach(function (memberId) {
        return Cache.del("user_groups_" + memberId);
      });
      return res.send(new APIResult(200, Utility.encodeResults({
        id: group.id,
        userStatus: userStatus
      })));
    });
  })["catch"](next);



  // /* ---------------- */
  // return GroupMember.getGroupCount(currentUserId).then(function(count) {
  //   if (count === MAX_USER_GROUP_OWN_COUNT) {
  //     return res.send(new APIResult(1000, null, "Current user's group count is out of max user group count limit (" + MAX_USER_GROUP_OWN_COUNT + ")."));
  //   }
  //   return sequelize.transaction(function(t) {
  //     return co(function*() {
  //       var group;
  //       group = (yield Group.create({
  //         name: name,
  //         portraitUri: portraitUri,
  //         memberCount: memberIds.length,
  //         creatorId: currentUserId,
  //         timestamp: timestamp
  //       }, {
  //         transaction: t
  //       }));
  //       Utility.log('Group %s created by %s', group.id, currentUserId);
  //       (yield GroupMember.bulkUpsert(group.id, memberIds, timestamp, t, currentUserId));
  //       return group;
  //     });
  //   }).then(function(group) {
  //     return DataVersion.updateGroupMemberVersion(group.id, timestamp).then(function() {
  //       rongCloud.group.create(encodedMemberIds, Utility.encodeId(group.id), name, function(err, resultText) {
  //         var result, success;
  //         if (err) {
  //           Utility.logError('Error: create group failed on IM server, error: %s', err);
  //         }
  //         result = JSON.parse(resultText);
  //         success = result.code === 200;
  //         if (success) {
  //           Session.getCurrentUserNickname(currentUserId, User).then(function(nickname) {
  //             return sendGroupNotification(currentUserId, group.id, GROUP_OPERATION_CREATE, {
  //               operatorNickname: nickname,
  //               targetGroupName: name,
  //               timestamp: timestamp
  //             });
  //           });
  //         } else {
  //           Utility.logError('Error: create group failed on IM server, code: %s', result.code);
  //           GroupSync.upsert({
  //             syncInfo: success,
  //             syncMember: success
  //           }, {
  //             where: {
  //               groupId: group.id
  //             }
  //           });
  //         }
  //         memberIds.forEach(function (memberId) {
  //           return Cache.del("user_groups_" + memberId);
  //         });
  //         return res.send(new APIResult(200, Utility.encodeResults({
  //           id: group.id
  //         })));
  //       });
  //     });
  //   });
  // })["catch"](next);
});

var joinGroup = function (encodedGroupId, groupName, encodedMemberIds) {
  return new Promise(function (resolve, reject) {
    rongCloud.group.join(encodedMemberIds, encodedGroupId, groupName, function (err, resultText) {
      if (err) {
        console.log('join group error');
        return reject(err);
      }
      resolve(resultText);
    });
  });
};

var addMembers = function (groupId, memberIds, optUserId) {
  var groupName, targetUserDisplayNames, targetUserIds;
  var timestamp = +new Date();
  return Group.getInfo(groupId).then(function (group) {
    if (!group) {
      return Promise.reject('Unknown group.');
    }
    groupName = group.name;
    var memberCount = group.memberCount + memberIds.length;
    var t = +new Date();
    return sequelize.transaction(function (t) {
      var updateCountParams = { memberCount: memberCount, timestamp: timestamp };
      var updateCountWhere = { where: { id: groupId }, transaction: t };
      return Promise.all([
        Group.update(updateCountParams, updateCountWhere),
        GroupMember.bulkUpsert(groupId, memberIds, timestamp, t),
        GroupReceiver.updateToExpired(groupId, memberIds)
      ]);
    });
  }).then(function (t) {
    var encodedGroupId = Utility.encodeId(groupId),
      encodedMemberIds = Utility.encodeIds(memberIds);
    targetUserIds = encodedMemberIds;
    return joinGroup(encodedGroupId, groupName, encodedMemberIds);
  }).then(function () {
    return User.getNicknames(memberIds);
  }).then(function (nicknames) {
    targetUserDisplayNames = nicknames;
    return Session.getCurrentUserNickname(optUserId, User);
  }).then(function (nickname) {
    memberIds.forEach(function (memberId) {
      return Cache.del("user_groups_" + memberId);
    });
    Cache.del("group_" + groupId);
    Cache.del("group_members_" + groupId);
    return sendGroupNotification(optUserId, groupId, GROUP_OPERATION_ADD, {
      operatorNickname: nickname,
      targetUserIds: targetUserIds,
      targetUserDisplayNames: targetUserDisplayNames,
      timestamp: timestamp
    });
  });
};

// router.post('/add', function(req, res, next) {
//   var currentUserId, encodedGroupId, encodedMemberIds, groupId, memberIds, timestamp;
//   groupId = req.body.groupId;
//   memberIds = req.body.memberIds;
//   encodedGroupId = req.body.encodedGroupId;
//   encodedMemberIds = req.body.encodedMemberIds;
//   Utility.log('Group %s add members %j by user %s', groupId, memberIds, Session.getCurrentUserId(req));
//   currentUserId = Session.getCurrentUserId(req);
//   timestamp = Date.now();
//   return Group.getInfo(groupId).then(function(group) {
//     var memberCount;
//     if (!group) {
//       return res.status(404).send('Unknown group.');
//     }
//     memberCount = group.memberCount + memberIds.length;
//     if (memberCount > group.maxMemberCount) {
//       return res.status(400).send("Group's member count is out of max group member count limit (" + group.maxMemberCount + ").");
//     }
//     return sequelize.transaction(function(t) {
//       return Promise.all([
//         Group.update({
//           memberCount: memberCount,
//           timestamp: timestamp
//         }, {
//           where: {
//             id: groupId
//           },
//           transaction: t
//         }), GroupMember.bulkUpsert(groupId, memberIds, timestamp, t)
//       ]);
//     }).then(function() {
//       return DataVersion.updateGroupMemberVersion(groupId, timestamp).then(function() {
//         rongCloud.group.join(encodedMemberIds, encodedGroupId, group.name, function(err, resultText) {
//           var result, success;
//           if (err) {
//             Utility.logError('Error: join group failed on IM server, error: %s', err);
//           }
//           result = JSON.parse(resultText);
//           success = result.code === 200;
//           if (success) {
//             return User.getNicknames(memberIds).then(function(nicknames) {
//               return Session.getCurrentUserNickname(currentUserId, User).then(function(nickname) {
//                 return sendGroupNotification(currentUserId, groupId, GROUP_OPERATION_ADD, {
//                   operatorNickname: nickname,
//                   targetUserIds: encodedMemberIds,
//                   targetUserDisplayNames: nicknames,
//                   timestamp: timestamp
//                 });
//               });
//             });
//           } else {
//             Utility.logError('Error: join group failed on IM server, code: %s', result.code);
//             return GroupSync.upsert({
//               syncMember: true
//             }, {
//               where: {
//                 groupId: group.id
//               }
//             });
//           }
//         });
//         memberIds.forEach(function(memberId) {
//           return Cache.del("user_groups_" + memberId);
//         });
//         Cache.del("group_" + groupId);
//         Cache.del("group_members_" + groupId);
//         return res.send(new APIResult(200));
//       });
//     });
//   })["catch"](next);
// });

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
  var HasKickRoles = [ GROUP_CREATOR, GROUP_MANAGER ];

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

    if (memberIds.indexOf(group.creatorId) !== -1) {
      return res.status(405).send('Can not kick the host.');
    }

    // if (group.creatorId !== currentUserId) {
    //   return res.status(403).send('Current user is not group creator.');
    // }
    return GroupMember.findAll({
      where: {
        groupId: groupId
      },
      attributes: ['memberId', 'role']
    }).then(function(groupMembers) {

      /* 判断是否有权限踢人 */
      var currentMember = Utility.getFromArrayById(groupMembers, currentUserId, 'memberId');
      var hasPermission = HasKickRoles.indexOf(currentMember.role) !== -1;
      if (!hasPermission) {
        return res.status(403).send('Current user is not group manager.');
      }

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
        return Session.getCurrentUserNickname(currentUserId, User).then(function (nickname) {
          return sequelize.transaction(function (t) {
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
            ]).then(function () {
              DataVersion.updateGroupMemberVersion(groupId, timestamp).then(function () {
              }, function (err) {
                console.log('err', err);
              });
              return sendGroupNotification(currentUserId, groupId, GROUP_OPERATION_KICKED, {
                operatorNickname: nickname,
                targetUserIds: encodedMemberIds,
                targetUserDisplayNames: nicknames,
                timestamp: timestamp
              }).then(function () {
                return rongCloud.group.quit(encodedMemberIds, encodedGroupId, function (err, resultText) {
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
                  memberIds.forEach(function (memberId) {
                    return Cache.del("user_groups_" + memberId);
                  });
                  Cache.del("group_" + groupId);
                  Cache.del("group_members_" + groupId);
                  return GroupFav.deleteFac(groupId, memberIds).then(function () {
                    return res.send(new APIResult(200));
                  });
                })
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
                return GroupFav.deleteFac(groupId, [ currentUserId ]).then(function () {
                  return res.send(new APIResult(200, null, resultMessage));
                });
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
            return GroupFav.deleteFac(groupId).then(function () {
              var receiverList = [];
              GroupReceiver.findAll({
                where: {
                  groupId: groupId
                }
              }).then(function (list) {
                receiverList = list;
                return GroupReceiver.update({
                  status: GroupReceiverStatus.EXPIRED
                }, {
                  where: {
                    groupId: groupId
                  }
                });
              }).then(function () {
                var requesterId = currentUserId;
                var optUserIdList = receiverList.map(function (item) {
                  return item.userId;
                });
                console.log(optUserIdList);
                sendGroupApplyMessage(requesterId, optUserIdList, 0, GroupReceiverStatus.EXPIRED, groupId);
              });
              return res.send(new APIResult(200));
            });
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

var addIMWhiteBlackList = function(groupId, userId, currentUserId){
  return new Promise(function(resolve, reject) {
    return Group.findOne({
      where: {
        id: groupId
      },
      attributes: ['isMute']
    }).then(function(gro) {
      if(gro.isMute != 1) {
        return GroupReceiver.deleteByGroup(groupId, currentUserId).then(function () {
          resolve();
        });
      }
      rongCloud.group.ban.addWhitelist({
        groupId: Utility.encodeId(groupId)
      }, [ { field: 'userId', values: [Utility.encodeId(userId)]} ], function(err, resultText) {
        if (err) {
          Utility.logError('Error: add group whitelist failed on IM server, error: %s', err);
        }
        if(JSON.parse(resultText).code === 200) {
          rongCloud.group.ban.removeWhiteList({
            groupId: Utility.encodeId(groupId)
          }, [ { field: 'userId', values: [Utility.encodeId(currentUserId)]} ], function(err, resultText) {
            if (err) {
              Utility.logError('Error: remove group whitelist failed on IM server, error: %s', err);
            }
            if(JSON.parse(resultText).code === 200) {
              return GroupReceiver.deleteByGroup(groupId, currentUserId).then(function () {
                resolve();
              });
            }
          })
        }
      })
    });
  })
}

router.post('/transfer', function(req, res, next) {
  var currentUserId, encodedUserId, groupId, timestamp, userId;
  groupId = req.body.groupId;
  userId = req.body.userId;
  encodedUserId = req.body.encodedUserId;
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  console.log('userId',userId,'currentUserId',currentUserId)
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
      console.log(groupMembers[0].memberId,currentUserId,groupMembers[0].role)
      return res.status(400).send('Current user is not group creator.');
    }
    return sequelize.transaction(function(t) {
      console.log('userId---',userId,'currentUserId',currentUserId)
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
        // Session.getCurrentUserNickname(currentUserId, User).then(function(currentUserNickname) {
        //   return User.getNickname(userId).then(function(nickname) {
        //     return sendGroupNotification(currentUserId, groupId, GROUP_OPERATION_TRANSFER, {
        //       operatorId: Utility.encodeId(currentUserId),
        //       operatorNickname: currentUserNickname,
        //       targetUserIds: [ encodedUserId ],
        //       targetUserDisplayNames: [ nickname ],
        //       timestamp: timestamp
        //     });
        //     /* 以下为旧代码 */
        //     // return sendGroupNotification(currentUserId, groupId, GROUP_OPERATION_TRANSFER, {
        //     //   oldCreatorId: Utility.encodeId(currentUserId),
        //     //   oldCreatorName: currentUserNickname,
        //     //   newCreatorId: encodedUserId,
        //     //   newCreatorName: nickname,
        //     //   timestamp: timestamp
        //     // });
        //   });
        // });
        console.log('userId---::',userId,'currentUserId',currentUserId)
        return addIMWhiteBlackList(groupId, userId, currentUserId).then(function(){
          Session.getCurrentUserNickname(currentUserId, User).then(function(currentUserNickname) {
            return User.getNickname(userId).then(function(nickname) {
              return sendGroupNotification(currentUserId, groupId, GROUP_OPERATION_TRANSFER, {
                operatorId: Utility.encodeId(currentUserId),
                operatorNickname: currentUserNickname,
                targetUserIds: [ encodedUserId ],
                targetUserDisplayNames: [ nickname ],
                timestamp: timestamp
              }).then(function(){
                Cache.del("group_" + groupId);
                Cache.del("group_members_" + groupId);
                return res.send(new APIResult(200));
              });
              /* 以下为旧代码 */
              // return sendGroupNotification(currentUserId, groupId, GROUP_OPERATION_TRANSFER, {
              //   oldCreatorId: Utility.encodeId(currentUserId),
              //   oldCreatorName: currentUserNickname,
              //   newCreatorId: encodedUserId,
              //   newCreatorName: nickname,
              //   timestamp: timestamp
              // });
            });
          });
        })
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
      return GroupReceiver.update({
        groupName: name
      }, {
        where: {
          groupId: groupId
        }
      }).then(function () {
        return res.send(new APIResult(200));
      });
    });
  })["catch"](next);
});

router.post('/set_bulletin', function (req, res, next) {
  var groupId = req.body.groupId,
    content = req.body.content || req.body.bulletin,
    currentUserId = Session.getCurrentUserId(req);
  
  /* 长度限制 */
  content = Utility.xss(content, GROUP_BULLETIN_MAX_LENGTH);
  if (!validator.isLength(content, 0, GROUP_BULLETIN_MAX_LENGTH)) {
    return res.status(400).send('Length of bulletin invalid.');
  }

  return GroupBulletin.createBulletin(groupId, content).then(function () {
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
    content && Session.getCurrentUserNickname(currentUserId, User).then(function (nickName) {
      sendBulletinNotification(currentUserId, groupId, content);
    });
    return res.send(new APIResult(200));
  })["catch"](next);
});

router.get('/get_bulletin', function (req, res, next) {
  var groupId = req.query.groupId;
  groupId = Utility.decodeIds(groupId);
  console.log('groupId', groupId);
  return GroupBulletin.getBulletin(groupId).then(function (buttlin) {
    if (buttlin) {
      buttlin = Utility.encodeResults(buttlin, ['id, groupId']);
      return res.send(new APIResult(200, buttlin));
    }
    return res.status(402).send('No group bulletin.');
  }).catch(next);
});

// router.post('/set_bulletin', function(req, res, next) {
//   var bulletin, currentUserId, groupId, timestamp;
//   groupId = req.body.groupId;
//   bulletin = Utility.xss(req.body.bulletin, GROUP_BULLETIN_MAX_LENGTH);
//   if (!validator.isLength(bulletin, 0, GROUP_BULLETIN_MAX_LENGTH)) {
//     return res.status(400).send('Length of bulletin invalid.');
//   }
//   currentUserId = Session.getCurrentUserId(req);
//   timestamp = Date.now();
//   return Group.update({
//     bulletin: bulletin,
//     timestamp: timestamp
//   }, {
//     where: {
//       id: groupId,
//       creatorId: currentUserId
//     }
//   }).then(function(arg) {
//     var affectedCount;
//     affectedCount = arg[0];
//     if (affectedCount === 0) {
//       return res.status(400).send('Unknown group or not creator.');
//     }
//     return DataVersion.updateGroupVersion(groupId, timestamp).then(function() {
//       GroupMember.findAll({
//         where: {
//           groupId: groupId
//         },
//         attributes: ['memberId']
//       }).then(function(members) {
//         return members.forEach(function(member) {
//           return Cache.del("user_groups_" + member.memberId);
//         });
//       });
//       Cache.del("group_" + groupId);
//       Session.getCurrentUserNickname(currentUserId, User).then(function (nickName) {
//         sendGroupNotification(currentUserId, groupId, GROUP_OPERATION_BULLETIN, {
//           operatorId: Utility.encodeId(currentUserId),
//           operatorName: nickName,
//           content: bulletin
//         });
//       });
//       return res.send(new APIResult(200));
//     });
//   })["catch"](next);
// });

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

router.get('/notice_info', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req);
  console.log(currentUserId)
  return GroupReceiver.batchDetail({
    userId: currentUserId
  }).then(function (receiverList) {
    var newReceiverList = [];
    receiverList.forEach(function (receiver) {
      receiver.group = Utility.encodeResults(receiver.group);
      receiver.requester = Utility.encodeResults(receiver.requester);
      receiver.receiver = Utility.encodeResults(receiver.receiver);
      receiver = addUpdateTime(receiver);
      delete receiver['deletedUsers'];
      newReceiverList.push(receiver);
    });
    return res.send(new APIResult(200, Utility.encodeResults(newReceiverList)));
  });

  // return GroupMember.findAll({
  //   where: {
  //     memberId: currentUserId,
  //     role: {
  //       $in: [GROUP_CREATOR, GROUP_MANAGER]
  //     }
  //   },
  //   attributes: ['groupId']
  // }).then(function (list) {
  //   var groupIdList = list.map(function (member) {
  //     return member.groupId;
  //   });
  //   return GroupReceiver.batchDetail({
  //     $or: [
  //       {
  //         groupId: { $in: groupIdList },
  //         type: ENUM.GroupReceiverType.MANAGER
  //       },
  //       {
  //         receiverId: currentUserId,
  //         type: ENUM.GroupReceiverType.MEMBER
  //       }
  //     ]
  //   });
  // }).then(function (receiverList) {
  //   var newReceiverList = [];
  //   receiverList.forEach(function (receiver) {
  //     var deletedUserList = GroupReceiver.getDeletedUserList(receiver.deletedUsers);
  //     if (deletedUserList.indexOf(currentUserId) === -1) {
  //       receiver = receiver.dataValues;
  //       receiver.group = Utility.encodeResults(receiver.group);
  //       receiver.requester = Utility.encodeResults(receiver.requester);
  //       receiver.receiver = Utility.encodeResults(receiver.receiver);
  //       receiver = addUpdateTime(receiver);
  //       delete receiver['deletedUsers'];
  //       newReceiverList.push(receiver);
  //     }
  //   });
  //   newReceiverList = addUpdateTimeToList(newReceiverList);
  //   return res.send(new APIResult(200, Utility.encodeResults(newReceiverList)));
  // });
});

router.get('/:id', function(req, res, next) {
  var currentUserId, groupId;
  groupId = req.params.id;
  groupId = Utility.decodeIds(groupId);
  currentUserId = Session.getCurrentUserId(req);

  /* group 中带入 buttlin, 已废弃 */
  // return Cache.get("group_" + groupId).then(function (group) {
  //   if (group) {
  //     console.log('cache');
  //     return res.send(new APIResult(200, group));
  //   } else {
  //     return Promise.all([Group.findById(groupId, {
  //       attributes: ['id', 'name', 'portraitUri', 'memberCount', 'maxMemberCount', 'creatorId', 'bulletin', 'deletedAt'],
  //       paranoid: false
  //     }), GroupBulletin.getBulletin(groupId)]).then(function (resultList) {
  //       var group = resultList[0];
  //       var bulletin = resultList[1];
        
  //       var results;
  //       if (!group) {
  //         return res.status(404).send('Unknown group.');
  //       }
  //       results = Utility.encodeResults(group, ['id', 'creatorId']);
  //       if (bulletin) {
  //         results.bulletin = bulletin.content;
  //         results.bulletinTime = bulletin.timestamp;
  //       }
  //       Cache.set("group_" + groupId, results);
  //       return res.send(new APIResult(200, results));
  //     });
  //   }
  // })["catch"](next);

  return Cache.get("group_" + groupId).then(function(group) {
    // console.log('cache:',group);
    if (group) {
      return res.send(new APIResult(200, group));
    } else {
      return Group.findById(groupId, {
        attributes: ['id', 'name', 'portraitUri', 'memberCount', 'maxMemberCount', 'creatorId', 'bulletin', 'deletedAt','isMute','certiStatus'],
        paranoid: false
      }).then(function(group) {
        var results;
        if (!group) {
          return res.status(404).send('Unknown group.');
        }
        results = Utility.encodeResults(group, ['id', 'creatorId']);
        console.log('results:',results);
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
      console.log(groupId);
      return GroupMember.findAll({
        where: {
          groupId: groupId
        },
        attributes: ['displayName', 'role', 'timestamp', 'createdAt', 'updatedAt'],
        include: {
          model: User,
          attributes: ['id', 'nickname', 'portraitUri','gender','stAccount','phone']
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
        results = results = addUpdateTimeToList(results, {
          updateKeys: ['createdAt', 'updatedAt']
        });
        Cache.set("group_members_" + groupId, results);
        return res.send(new APIResult(200, results));
      });
    }
  })["catch"](next);
});

router.post('/fav', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req),
    groupId = req.body.groupId;
  
  var ExistsGroup = 405;

  return GroupFav.createFav(currentUserId, groupId).then(function () {
    return res.send(new APIResult(200));
  }).catch(function (err) {
    if (err.name === 'SequelizeUniqueConstraintError') {
      return res.status(ExistsGroup).send('Group already exists.');
    }
    return next(err);
  });
});

router.delete('/fav', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req),
    groupId = req.body.groupId;

  return GroupFav.deleteFac(groupId, [ currentUserId ]).then(function () {
    return res.send(new APIResult(200));
  }).catch(next);
});

var setGroupMemberRole = function (groupId, memberIds, role, currentUserId, optName) {
  var CanSetManagerRole = [GROUP_CREATOR];

  var ParamsError = { code: 400, msg: 'Request parameter error.' };
  var RoleError = { code: 401, msg: 'No permission to set up an manager.' }; // 设置权限不足
  var NotInGroupError = { code: 402, msg: 'Not in the group.' }; // 不在群组中
  var SetCreatorError = { code: 403, msg: 'Cannot set the group creator.' }; // 不能设置群主

  if (!groupId || !memberIds) {
    return Promise.reject(ParamsError);
  }

  var includeSelfMemberIds = memberIds.concat(currentUserId);

  return GroupMember.findAll({
    where: {
      groupId: groupId,
      memberId: {
        $in: includeSelfMemberIds
      }
    },
    attributes: ['memberId', 'role']
  }).then(function (groupMembers) {
    /* 判断自己是否在群组中 */
    var selfMember = Utility.getFromArrayById(groupMembers, currentUserId, 'memberId');
    if (!selfMember) {
      return Promise.reject(NotInGroupError);
    }

    /* 判断是否有权限设置管理员 */
    var hasPermission = CanSetManagerRole.indexOf(selfMember.role) !== -1;
    if (!hasPermission) {
      return Promise.reject(RoleError);
    }

    /* 判断设置的成员中是否有群主 */;
    for (var i = 0; i < groupMembers.length; i++) {
      var member = groupMembers[i];
      var isSelf = member.memberId === currentUserId;
      var isCreator = member.role === GROUP_CREATOR;

      if (!isSelf && isCreator) {
        return Promise.reject(SetCreatorError);
      }
    }

    var nickNames;
    return GroupMember.update({ role: role }, {
      where: {
        groupId: groupId,
        memberId: {
          $in: memberIds
        }
      }
    }).then(function () {
      return User.getNicknames(memberIds);
    }).then(function (nicknameList) {
      nickNames = nicknameList;
      return Session.getCurrentUserNickname(currentUserId, User);
    }).then(function (nickName) {
      return sendGroupNotification(currentUserId, groupId, optName, {
        operatorId: currentUserId,
        operatorNickname: nickName,
        targetUserIds: Utility.encodeIds(memberIds),
        targetUserDisplayNames: nickNames,
        timestamp: Date.now()
      });
    });
  });
};

router.post('/set_manager', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req),
    groupId = req.body.groupId,
    memberIds = req.body.memberIds;
  var encodedMemberIds = Utility.encodeIds(memberIds);
  console.log(memberIds,encodedMemberIds)
  return setGroupMemberRole(groupId, memberIds, GROUP_MANAGER, currentUserId, GROUP_OPERATION_SETMANAGER).then(function () {
    return DataVersion.updateGroupMemberVersion(groupId, Date.now());
  }).then(function () {
    Cache.del("group_" + groupId);
    Cache.del("group_members_" + groupId);
    return Group.findOne({
      where: {
        id: groupId
      },
      attributes: ['isMute']
    }).then(function(gro) {
      console.log('gro.isMute',gro.isMute);
      if(gro.isMute != 1) {
        return res.send(new APIResult(200));
      }
      console.log('groupId---',groupId,'memberIds---:',memberIds)
      rongCloud.group.ban.addWhitelist({
          groupId: Utility.encodeId(groupId)
        }, [ { field: 'userId', values: encodedMemberIds} ], function(err, resultText) {
          if (err) {
            Utility.logError('Error: add group whitelist failed on IM server, error: %s', err);
          }
          if(JSON.parse(resultText).code === 200) {
            return res.send(new APIResult(200));
          }
        })
    })
  }).catch(function (error) {
    if (error.msg && error.code) {
      return res.status(error.code).send(error.msg);
    }
    return next(error);
  });
});

router.post('/remove_manager', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req),
    groupId = req.body.groupId,
    memberIds = req.body.memberIds;
    var encodedMemberIds = Utility.encodeIds(memberIds);
  return setGroupMemberRole(groupId, memberIds, GROUP_MEMBER, currentUserId, GROUP_OPERATION_REMOVEMANAGER).then(function () {
    return DataVersion.updateGroupMemberVersion(groupId, Date.now());
  }).then(function () {
    Cache.del("group_" + groupId);
    Cache.del("group_members_" + groupId);

    return Group.findOne({
      where: {
        id: groupId
      },
      attributes: ['isMute']
    }).then(function(gro) {
      if(gro.isMute != 1) {
        return GroupReceiver.deleteByGroupManager(groupId, memberIds).then(function () {
          return res.send(new APIResult(200));
        });
      }
      rongCloud.group.ban.removeWhiteList({
          groupId: Utility.encodeId(groupId)
        }, [ { field: 'userId', values: encodedMemberIds} ], function(err, resultText) {
          if (err) {
            Utility.logError('Error: remove group whitelist failed on IM server, error: %s', err);
          }
          if(JSON.parse(resultText).code === 200) {
            return GroupReceiver.deleteByGroupManager(groupId, memberIds).then(function () {
              return res.send(new APIResult(200));
            });
          }
        })
    })
    
  }).catch(function (error) {
    if (error.msg && error.code) {
      return res.status(error.code).send(error.msg);
    }
    return next(error);
  });
});

router.post('/set_certification', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req),
    groupId = req.body.groupId,
    certiStatus = req.body.certiStatus;
  return Group.hasManagerRole(groupId, currentUserId).then(function (hasRole) {
    if (!hasRole) {
      var error = ErrorENUM.PERMISSIONS;
      return res.send(new APIResult(error.code, null, error.msg));
    }
    return Group.update({
      certiStatus: certiStatus
    }, {
      where: {
        id: groupId
      }
    });
  }).then(function () {
    Cache.del("group_" + groupId);
    return res.send(new APIResult(200));
  }).catch(next);
});

router.post('/add', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req),
    groupId = req.body.groupId,
    memberIds = req.body.memberIds;

  if (!memberIds.length) {
    var error = ErrorENUM.LIMIT;
    return res.send(new APIResult(error.code, null, error.msg));
  }

  var userStatus = [], groupDetail = {};

  var hasManagerRole, isGroupVerifyOpened, verifyOpendUserIds, verifyClosedUserIds;
  return Group.hasManagerRole(groupId, currentUserId).then(function (hasRole) {
    hasManagerRole = hasRole; // 自己在目标群组中的角色, 管理者/普通成员
    return Group.getGroupVerify(groupId);
  }).then(function (groupVerify) {
    var hasVerify = groupVerify.isVerify;
    groupDetail = groupVerify.group;
    isGroupVerifyOpened = hasVerify; // 目标群组是否开启了 入群认证
    return User.batchGroupVerify(memberIds);
  }).then(function (result) {
    verifyOpendUserIds = result.opened; // 开启个人入群认证的 userId
    verifyClosedUserIds = result.closed; // 未开启个人入群认证的 userId
    /*
    处理 verifyOpendUserIds 开启认证的用户
    更新为待用户处理状态, 批量发消息
    */
    var type = ENUM.GroupReceiverType.MEMBER;
    if (verifyOpendUserIds.length) {
      userStatus = userStatus.concat(verifyOpendUserIds.map(function (id) {
        return { id: Utility.encodeId(id), status: ENUM.GroupAddStatus.WAIT_MEMBER };
      }));
      return batchUpsertGroupReceiver(groupDetail, currentUserId, verifyOpendUserIds, verifyOpendUserIds, type, GroupReceiverStatus.WAIT);
    }
    return Promise.resolve();
  }).then(function () {
    if (!verifyClosedUserIds.length) {
      return Promise.resolve();
    }
    var isApplyToManager = !hasManagerRole && isGroupVerifyOpened; // 当自己不是管理者 && 群组开启了入群认证时, 需要管理员同意
    if (isApplyToManager) {
      /*
      处理 verifyClosedUserIds 关闭认证的用户
      !hasManagerRole && isGroupVerifyOpened --> 更新为待管理员审批状态, 更新多个, 发消息
      */
      return GroupMember.findAll({
        where: {
          groupId: groupId,
          role: {
            $in: [GROUP_CREATOR, GROUP_MANAGER]
          }
        },
        attributes: ['memberId']
      }).then(function (list) {
        var managerIds = list.map(function (member) {
          return member.memberId;
        });
        var type = ENUM.GroupReceiverType.MANAGER;
        userStatus = userStatus.concat(verifyClosedUserIds.map(function (id) {
          return { id: Utility.encodeId(id), status: ENUM.GroupAddStatus.WAIT_MANAGER };
        }));
        return batchUpsertGroupReceiver(groupDetail, currentUserId, verifyClosedUserIds, managerIds, type, GroupReceiverStatus.WAIT);
      });
    } else {
      /*
      处理 verifyClosedUserIds
      !isGroupVerifyOpened || hasManagerRole --> 直接加群
      */
      userStatus = userStatus.concat(verifyClosedUserIds.map(function (id) {
        return { id: Utility.encodeId(id), status: ENUM.GroupAddStatus.ADDED };
      }));
      return addMembers(groupId, verifyClosedUserIds, currentUserId);
    }
  }).then(function () {
    return res.send(new APIResult(200, userStatus));
  })['catch'](next);;
});

router.post('/agree', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req),
    groupId = req.body.groupId,
    receiverId = req.body.receiverId,
    receiverStatus = req.body.status;

  var isReceiverOpt = currentUserId === receiverId; // 是否为 被邀请者同意或忽略
  var GroupReceiverType = ENUM.GroupReceiverType;
  var isAgree = receiverStatus === ENUM.GroupReceiverStatus.AGREED;
  var type = isReceiverOpt ? GroupReceiverType.MEMBER : GroupReceiverType.MANAGER;
  var groupReceiver;

  console.log(groupId, receiverId, type, currentUserId);
  return GroupReceiver.findOne({
    where: {
      groupId: groupId,
      receiverId: receiverId,
      type: type
    },
    attributes: ['requesterId']
  }).then(function (result) {
    if (!result) {
      var error = ErrorENUM.NOT_FOUND;
      return res.send(new APIResult(error.code, null, error.msg));
    }
    groupReceiver = result;
    return upsertGroupReceiver(groupId, receiverId, currentUserId, type, receiverStatus);
  }).then(function () {
    var error = ErrorENUM.NOT_FOUND;
    return isAgree ? Group.getGroupVerify(groupId, groupReceiver.requesterId) : Promise.resolve;
  }).then(function (groupVerify) {
    var hasVerify = groupVerify.isVerify;
    var groupDetail = groupVerify.group;
    if (!isAgree) { // 不同意, 更新数据后直接返回
      return;
    }
    if (isReceiverOpt && hasVerify) { // 如果为邀请者同意且群组开启了认证, 新增为管理员认证
      return GroupMember.findAll({
        where: {
          groupId: groupId,
          role: {
            $in: [GROUP_CREATOR, GROUP_MANAGER]
          }
        },
        attributes: ['memberId']
      }).then(function (list) {
        var managerIds = list.map(function (member) {
          return member.memberId;
        });
        var managerType = ENUM.GroupReceiverType.MANAGER;
        return batchUpsertGroupReceiver(groupDetail, groupReceiver.requesterId, [receiverId], managerIds, managerType, GroupReceiverStatus.WAIT);
      });
    } else { // 如果为群组未开启认证 或 为管理员同意, 直接加群
      return addMembers(groupId, [receiverId], groupReceiver.requesterId);
    }
  }).then(function () {
    return res.send(new APIResult(200));
  })['catch'](next);
});

router.post('/clear_notice', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req);

  return GroupReceiver.destroy({
    where: {
      userId: currentUserId
    }
  }).then(function () {
    return res.send(new APIResult(200));
  })['catch'](next);
  
  // return GroupMember.findAll({
  //   where: {
  //     memberId: currentUserId,
  //     role: {
  //       $in: [GROUP_CREATOR, GROUP_MANAGER]
  //     }
  //   },
  //   attributes: ['groupId']
  // }).then(function (list) {
  //   var groupIdList = list.map(function (member) {
  //     return member.groupId;
  //   });
  //   return GroupReceiver.batchDetail({
  //     $or: [
  //       {
  //         groupId: { $in: groupIdList },
  //         type: ENUM.GroupReceiverType.MANAGER
  //       },
  //       {
  //         receiverId: currentUserId,
  //         type: ENUM.GroupReceiverType.MEMBER
  //       }
  //     ]
  //   });
  // }).then(function (receiverList) {
  //   return GroupReceiver.deleteByUser(receiverList, currentUserId);
  // }).then(function () {
  //   return res.send(new APIResult(200));
  // });
});

//群组禁言
router.post('/mute_all', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req),
    muteStatus = Number(req.body.muteStatus),
    groupId = req.body.groupId,
    memberIds = [];
    if(req.body.userId){
      memberIds = req.body.userId
    }

  if([0,1].indexOf(muteStatus) == -1){
    return res.status(400).send('Illegal parameter .');
  }
  
  //查出群主与管理员
  if(muteStatus == 0) { // 取消禁言
    rongCloud.group.ban.remove({
      groupId: Utility.encodeId(groupId)
    }, [], function(err, resultText) {
      if (err) {
        Utility.logError('Error: rollback group failed on IM server, error: %s', err);
      }
      if(JSON.parse(resultText).code === 200) {
        //更新库
        Group.update({
          isMute: muteStatus
        },{
          where:{
            id: groupId
          }
        }).then(function() {
          Cache.del("group_" + groupId);
          return res.send(new APIResult(200));
        })
      }
    })
  }else { //开启禁言
    return GroupMember.findAll({
      where: {
        groupId: groupId,
        role: {
          $in:[0, 2]
        }
      },
      attributes: ['memberId']
    }).then(function(groupMembers) {
      groupMembers.forEach(function (item) {
        memberIds.push(Utility.encodeId(item.memberId))
      })
      //设置禁言
      rongCloud.group.ban.add({
        groupId: Utility.encodeId(groupId)
      }, [], function(err, resultText) {
        if (err) {
          Utility.logError('Error: ban group failed on IM server, error: %s', err);
        }
        if(JSON.parse(resultText).code === 200) {
          // console.log('memberIds',memberIds)
          rongCloud.group.ban.addWhitelist({
            groupId: Utility.encodeId(groupId)
          }, [ { field: 'userId', values: memberIds} ], function (err, resultText) {
            if (err) {
              Utility.logError('Error: add group whitelist failed on IM server, error: %s', err);
            }
            if(JSON.parse(resultText).code === 200){
              //更新库
              console.log('mutestatus',muteStatus)
              Group.update({
                isMute: muteStatus
              },{
                where:{
                  id: groupId
                }
              }).then(function() {
                Cache.del("group_" + groupId);
                return res.send(new APIResult(200));
              })
            }
          })
        }
      })
    })
  }
});

var sendGroupNtfMsg = function(userId,targetId,operation) {
  var encodedUserId, message, objectName;
  objectName = 'ST:ConNtf';
  encodedUserId = Utility.encodeId(userId);
  encodedTargetId = Utility.encodeId(targetId);
  message = {
    operatorUserId: encodedUserId,
    operation: operation,
  };
  return new Promise(function(resolve, reject) {
    rongCloud.message.group.publishCus({
      fromUserId: encodedUserId,
      toGroupId: encodedTargetId,
      objectName: objectName,
      content: JSON.stringify(message),
      isMentioned: 0,
      isIncludeSender: 1
    }, function (err, resultText) {
      if (err) {
        Utility.logError('Error: send group notification failed: %s', err);
        reject(err);
      }
      return resolve(resultText);
    })
  })
}
var findClearGroup = function(clearStatus) {
  return new Promise(function(resolve, reject) {
    return Group.findAll({
      where: {
        clearStatus: clearStatus
      },
      attributes: [id]
    }).then(function(gro) {
      // console.log(gro)
      resolve(gro);
    })
  })
}
/**
 * params.currentUserId: 当前用户 Id
 * params.targetId: 目标用户 Id
 * params.conversationType: 会话类型
 * params.clearType: 清除类型 3 3天前、7 7天前、36 36小时前
 */
var regularClearMsg = function(params) {
  Group.findAll({
    where: {
        clearStatus: {
          $in:[3, 7, 36]
        }
    },
    attributes: ['id', 'clearStatus', 'clearTimeAt']
  }).then(function (groups) {
      console.log(typeof groups);
      for(var key in groups){
        console.log(groups[key].id,groups[key].clearStatus,groups[key].clearTimeAt)
      }
  })
  
}
//定时清理群消息
router.post('/set_regular_clear', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req),
    groupId = req.body.groupId,
    clearStatus = req.body.clearStatus;
  var operation = 'openRegularClear';
  var currentTime = new Date().getTime();
  if([0,3,7,36].indexOf(clearStatus) == -1){
    return res.status(400).send('Illegal parameter .');
  }
  if(clearStatus == 0){
    operation = 'closeRegularClear'
  }
  GroupMember.findOne({
    where: {
      groupId: groupId,
      memberId: currentUserId
    },
    attributes: ['role']
  }).then(function(result) {
    // console.log(result.role)
    if(result.role != 0) {
      return res.status(400).send('Not a group owner .');
    }
    return Group.update({
      clearStatus: clearStatus,
      clearTimeAt: currentTime
    },{
      where: {
        id: groupId
      }
    }).then(function() {
      return sendGroupNtfMsg(currentUserId, groupId, operation).then(function (result) {
        // regularClearMsg();
        return res.send(new APIResult(200));
      })
    })
  })['catch'](next)
})

//获取清理群消息设置
router.post('/get_regular_clear', function (req, res, next) {
  var groupId = req.body.groupId;
  return Group.findOne({
    where: {
      id: groupId
    },
    attributes: ['clearStatus']
  }).then(function (result){
    return res.send(new APIResult(200,result.clearStatus));
  })['catch'](next)
})
module.exports = router;