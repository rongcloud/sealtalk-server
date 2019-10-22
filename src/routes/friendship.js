var APIResult, Blacklist, CONTACT_OPERATION_ACCEPT_RESPONSE, CONTACT_OPERATION_REQUEST, Cache, Config, DataVersion, FRIENDSHIP_AGREED, FRIENDSHIP_DELETED, FRIENDSHIP_IGNORED, FRIENDSHIP_REQUESTED, FRIENDSHIP_REQUESTING, FRIEND_DISPLAY_NAME_MAX_LENGTH, FRIEND_DISPLAY_NAME_MIN_LENGTH, FRIEND_REQUEST_MESSAGE_MAX_LENGTH, FRIEND_REQUEST_MESSAGE_MIN_LENGTH, Friendship, Group, GroupMember, GroupSync, LoginLog, Session, User, Utility, VerificationCode, express, moment, ref, rongCloud, router, sendContactNotification, sequelize, validator, regionMap;

express = require('express');

moment = require('moment');

rongCloud = require('rongcloud-sdk');

Config = require('../conf');

Cache = require('../util/cache');

Session = require('../util/session');

Utility = require('../util/util').Utility;

APIResult = require('../util/util').APIResult;

ref = require('../db'), sequelize = ref[0], User = ref[1], Blacklist = ref[2], Friendship = ref[3], Group = ref[4], GroupMember = ref[5], GroupSync = ref[6], DataVersion = ref[7], VerificationCode = ref[8], LoginLog = ref[9];

var addUpdateTimeToList = require('../util/util').addUpdateTimeToList;

FRIENDSHIP_REQUESTING = 10;

FRIENDSHIP_REQUESTED = 11;

FRIENDSHIP_AGREED = 20;

FRIENDSHIP_IGNORED = 21;

FRIENDSHIP_DELETED = 30;

FRIENDSHIP_PULLEDBLACK = 31;

FRIEND_REQUEST_MESSAGE_MIN_LENGTH = 0;

FRIEND_REQUEST_MESSAGE_MAX_LENGTH = 64;

FRIEND_DISPLAY_NAME_MIN_LENGTH = 1;

FRIEND_DISPLAY_NAME_MAX_LENGTH = 32;

CONTACT_OPERATION_ACCEPT_RESPONSE = 'AcceptResponse';

CONTACT_OPERATION_REQUEST = 'Request';

var ENUM = require('../util/enum');

regionMap = {
  '86': 'zh-CN'
};

var RegistrationStatus = ENUM.RegistrationStatus;
var RelationshipStatus = ENUM.RelationshipStatus;

rongCloud.init(Config.RONGCLOUD_APP_KEY, Config.RONGCLOUD_APP_SECRET, {
  api: Config.RONGCLOUD_API_URL
});

sendContactNotification = function(userId, nickname, friendId, operation, message, timestamp) {
  var contactNotificationMessage, encodedFriendId, encodedUserId;
  encodedUserId = Utility.encodeId(userId);
  encodedFriendId = Utility.encodeId(friendId);
  contactNotificationMessage = {
    operation: operation,
    sourceUserId: encodedUserId,
    targetUserId: encodedFriendId,
    message: message,
    extra: {
      sourceUserNickname: nickname,
      version: timestamp
    }
  };
  contactNotificationMessage = JSON.stringify(contactNotificationMessage);
  Utility.log('Sending ContactNotificationMessage:', JSON.stringify(contactNotificationMessage));
  return rongCloud.message.system.publish(encodedUserId, [encodedFriendId], 'ST:ContactNtf', contactNotificationMessage, function(err, resultText) {
    if (err) {
      return Utility.logError('Error: send contact notification failed: %j', err);
    }
  });
};

router = express.Router();

validator = sequelize.Validator;

var removeBlackListPerson = function(currentUserId, friendId) {
  return new Promise(function(resolve, reject) {
    return rongCloud.user.blacklist.remove(Utility.encodeId(currentUserId), Utility.encodeId(friendId), function (err, resultText) {
      return rongCloud.user.blacklist.remove(Utility.encodeId(friendId), Utility.encodeId(currentUserId), function (err, resultText) {
        return Blacklist.update({
          status: false
        },{
          where: {
            userId: {$in: [currentUserId, friendId]},
            friendId: {$in: [friendId, currentUserId]}
          } 
        }).then(function(result){
          console.log('removeBlackListPerson',result);
          Cache.del("user_blacklist_" + currentUserId);
          resolve(result);
        }).catch(function(err) {
          reject(err)
        })
      })
    }) 
  })
};

router.post('/invite', function(req, res, next) {
  var currentUserId, friendId, message, timestamp;
  friendId = req.body.friendId;
  message = Utility.xss(req.body.message, FRIEND_REQUEST_MESSAGE_MAX_LENGTH);
  if (!validator.isLength(message, FRIEND_REQUEST_MESSAGE_MIN_LENGTH, FRIEND_REQUEST_MESSAGE_MAX_LENGTH)) {
    return res.status(400).send('Length of friend request message is out of limit.');
  }
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  Utility.log('%s invite user -> %s', currentUserId, friendId);
  return User.findOne({
      where: {
        id: friendId
      },
      attributes: ['friVerify']
    }).then(function(result) {
      console.log('friVerify:',result.friVerify);
      if(result.friVerify == 1){//需对方验证
        console.log('需对方验证')
        return Promise.all([
          Friendship.getInfo(currentUserId, friendId), Friendship.getInfo(friendId, currentUserId), Blacklist.findOne({
            where: {
              userId: friendId,
              friendId: currentUserId
            },
            attributes: ['status']
          })
        ]).then(function(arg) {
          // console.log('arg:--',JSON.stringify(arg))
          var action, blacklist, fd, fdStatus, fg, fgStatus, resultMessage, unit;
          fg = arg[0], fd = arg[1], blacklist = arg[2];
          Utility.log('Friendship requesting: %j', fg);
          Utility.log('Friendship requested:  %j', fd);
          if (blacklist && blacklist.status && fg.status == FRIENDSHIP_PULLEDBLACK) { //不给只加入黑名单的人发送邀请消息
            Utility.log('Invite result: %s %s', 'None: blacklisted by friend', 'Do nothing.');
            return res.send(new APIResult(200, {
              action: 'None'
            }, 'Do nothing.'));
          }
          action = 'Added';
          resultMessage = 'Friend added.';
          console.log('fg && fd',fg && fd)
          if (fg && fd) {
            if (fg.status === FRIENDSHIP_AGREED && fd.status === FRIENDSHIP_AGREED) {
              return res.status(400).send("User " + friendId + " is already your friend.");
            }
            if (req.app.get('env') === 'development') {
              unit = 's';
            } else {
              unit = 'd';
            }
            if (fd.status === FRIENDSHIP_REQUESTING) {
              fgStatus = FRIENDSHIP_AGREED;
              fdStatus = FRIENDSHIP_AGREED;
              message = fd.message;
            } else if (fd.status === FRIENDSHIP_AGREED) {
              fgStatus = FRIENDSHIP_AGREED;
              fdStatus = FRIENDSHIP_AGREED;
              message = fd.message;
              timestamp = fd.timestamp;
            } else if ((fd.status === FRIENDSHIP_DELETED && fg.status === FRIENDSHIP_DELETED) || (fg.status === FRIENDSHIP_AGREED && fd.status === FRIENDSHIP_DELETED) || (fg.status === FRIENDSHIP_REQUESTING && fd.status === FRIENDSHIP_IGNORED && moment().subtract(1, unit).isAfter(fg.updatedAt)) || (fg.status === FRIENDSHIP_REQUESTING && fd.status === FRIENDSHIP_REQUESTED && moment().subtract(3, unit).isAfter(fg.updatedAt))) {
              fgStatus = FRIENDSHIP_REQUESTING;
              fdStatus = FRIENDSHIP_REQUESTED;
              action = 'Sent';
              resultMessage = 'Request sent.';
            } else {
              Utility.log('Invite result: %s %s', 'None', 'Do nothing.');
              // console.log('none ---')
              return res.send(new APIResult(200, {
                action: 'None'
              }, 'Do nothing.'));
            }
            return sequelize.transaction(function(t) {
              return Promise.all([
                fg.update({
                  status: fgStatus,
                  timestamp: timestamp
                }, {
                  transaction: t
                }), fd.update({
                  status: fdStatus,
                  timestamp: timestamp,
                  message: message
                }, {
                  transaction: t
                })
              ]).then(function() {
                return DataVersion.updateFriendshipVersion(currentUserId, timestamp).then(function() {
                  if (fd.status === FRIENDSHIP_REQUESTED) {
                    // console.log('---sendssss msg，直接添加');
                    return DataVersion.updateFriendshipVersion(friendId, timestamp).then(function() {
                      Session.getCurrentUserNickname(currentUserId, User).then(function(nickname) {
                        return sendContactNotification(currentUserId, nickname, friendId, CONTACT_OPERATION_REQUEST, message, timestamp);
                      });
                      Cache.del("friendship_all_" + currentUserId);
                      Cache.del("friendship_all_" + friendId);
                      Utility.log('Invite result: %s %s', action, resultMessage);
                      // console.log('fd.status === FRIENDSHIP_REQUESTED')
                      return res.send(new APIResult(200, {
                        action: action
                      }, resultMessage));
                    });
                  } else {
                    removeBlackListPerson(currentUserId,friendId).then(function(result) {
                      Cache.del("friendship_all_" + currentUserId);
                      Cache.del("friendship_all_" + friendId);
                      Utility.log('Invite result: %s %s', action, resultMessage);
                      return res.send(new APIResult(200, {
                        action: action
                      }, resultMessage));
                    })
                  }
                });
              });
            });
          } else {
            if (friendId === currentUserId) {
              console.log('fd.status !== FRIENDSHIP_REQUESTED')
              return Promise.all([
                Friendship.create({
                  userId: currentUserId,
                  friendId: friendId,
                  message: '',
                  status: FRIENDSHIP_AGREED,
                  timestamp: timestamp
                }), DataVersion.updateFriendshipVersion(currentUserId, timestamp)
              ]).then(function() {
                Cache.del("friendship_all_" + currentUserId);
                Cache.del("friendship_all_" + friendId);
                Utility.log('Invite result: %s %s', action, resultMessage);
                return res.send(new APIResult(200, {
                  action: action
                }, resultMessage));
              });
            } else {
              return sequelize.transaction(function(t) {
                return Promise.all([
                  Friendship.create({
                    userId: currentUserId,
                    friendId: friendId,
                    message: '',
                    status: FRIENDSHIP_REQUESTING,
                    timestamp: timestamp
                  }, {
                    transaction: t
                  }), Friendship.create({
                    userId: friendId,
                    friendId: currentUserId,
                    message: message,
                    status: FRIENDSHIP_REQUESTED,
                    timestamp: timestamp
                  }, {
                    transaction: t
                  })
                ]).then(function() {
                  return Promise.all([DataVersion.updateFriendshipVersion(currentUserId, timestamp), DataVersion.updateFriendshipVersion(friendId, timestamp)]).then(function() {
                    Session.getCurrentUserNickname(currentUserId, User).then(function(nickname) {
                      return sendContactNotification(currentUserId, nickname, friendId, CONTACT_OPERATION_REQUEST, message, timestamp);
                    });
                    Cache.del("friendship_all_" + currentUserId);
                    Cache.del("friendship_all_" + friendId);
                    Utility.log('Invite result: %s %s', 'Sent', 'Request sent.');
                    console.log('fd.status === FRIENDSHIP_REQUESTED232222')
                    return res.send(new APIResult(200, {
                      action: 'Sent'
                    }, 'Request sent.'));
                  });
                });
              });
            }
          }
        })["catch"](next);
      }else {//不需对方验证直接添加
        removeBlackListPerson(currentUserId,friendId).then(function(result) {
          return Friendship.upsert({
            userId: currentUserId,
            friendId: friendId,
            message: message,
            status: FRIENDSHIP_AGREED,
            timestamp: timestamp
          },{
            where: {
              userId: currentUserId,
              friendId: friendId
            }
          }).then(function (result){
            console.log('upsert----',result);
            return Friendship.upsert({
              userId: friendId,
              friendId: currentUserId,
              message: message,
              status: FRIENDSHIP_AGREED,
              timestamp: timestamp
            },{
              where: {
                userId: currentUserId,
                friendId: friendId
              }
            }).then(function (result){
              Session.getCurrentUserNickname(currentUserId, User).then(function(nickname) {
                return sendContactNotification(currentUserId, nickname, friendId, CONTACT_OPERATION_REQUEST, message, timestamp);
              }).catch(function(err){
                console.log(err,'111getCurrentUserName')
              });
              Cache.del("friendship_all_" + currentUserId);
              Cache.del("friendship_all_" + friendId);
              Utility.log('Invite result: %s %s', 'None');
              return res.send(new APIResult(200, {
                action: 'AddDirectly'
              }, 'Request sent.'));
            });
          })
        })
      }
    })
});

router.post('/agree', function(req, res, next) {
  var currentUserId, friendId, timestamp;
  friendId = req.body.friendId;
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  Utility.log('%s agreed to user -> %s', currentUserId, friendId);
  removeBlackListPerson(currentUserId,friendId).then(function(result) {
    return sequelize.transaction(function(t) {
      return Friendship.update({
        status: FRIENDSHIP_AGREED,
        timestamp: timestamp
      }, {
        where: {
          userId: currentUserId,
          friendId: friendId,
          status: {
            $in: [FRIENDSHIP_REQUESTED, FRIENDSHIP_AGREED]
          }
        },
        transaction: t
      }).then(function(arg) {
        var affectedCount;
        affectedCount = arg[0];
        if (affectedCount === 0) {
          return res.status(404).send('Unknown friend user or invalid status.');
        }
        return Friendship.update({
          status: FRIENDSHIP_AGREED,
          timestamp: timestamp
        }, {
          where: {
            userId: friendId,
            friendId: currentUserId
          },
          transaction: t
        }).then(function() {
          return Promise.all([DataVersion.updateFriendshipVersion(currentUserId, timestamp), DataVersion.updateFriendshipVersion(friendId, timestamp)]).then(function() {
            Session.getCurrentUserNickname(currentUserId, User).then(function(nickname) {
              return sendContactNotification(currentUserId, nickname, friendId, CONTACT_OPERATION_ACCEPT_RESPONSE, '', timestamp);
            });
            Cache.del("friendship_all_" + currentUserId);
            Cache.del("friendship_all_" + friendId);
            return res.send(new APIResult(200));
          });
        });
      });
    })["catch"](next);
  })
});

router.post('/ignore', function(req, res, next) {
  var currentUserId, friendId, timestamp;
  friendId = req.body.friendId;
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  return Friendship.update({
    status: FRIENDSHIP_IGNORED,
    timestamp: timestamp
  }, {
    where: {
      userId: currentUserId,
      friendId: friendId,
      status: FRIENDSHIP_REQUESTED
    }
  }).then(function(arg) {
    var affectedCount;
    affectedCount = arg[0];
    if (affectedCount === 0) {
      return res.status(404).send('Unknown friend user or invalid status.');
    }
    return DataVersion.updateFriendshipVersion(currentUserId, timestamp).then(function() {
      Cache.del("friendship_all_" + currentUserId);
      Cache.del("friendship_all_" + friendId);
      return res.send(new APIResult(200));
    });
  })["catch"](next);
});

router.post('/delete', function(req, res, next) {
  var currentUserId, friendId, timestamp;
  friendId = req.body.friendId;
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  return Friendship.update({
    status: FRIENDSHIP_DELETED,
    displayName: '',
    message: '',
    timestamp: timestamp
  }, {
    where: {
      userId: currentUserId,
      friendId: friendId,
      status: {
        $in: [FRIENDSHIP_AGREED, FRIENDSHIP_PULLEDBLACK]
      }
    }
  }).then(function(arg) {
    var affectedCount;
    affectedCount = arg[0];
    if (affectedCount === 0) {
      return res.status(404).send('Unknown friend user or invalid status.');
    }
    return DataVersion.updateFriendshipVersion(currentUserId, timestamp).then(function() {
      // Cache.del("friendship_profile_displayName_" + currentUserId + "_" + friendId);
      // Cache.del("friendship_profile_user_" + currentUserId + "_" + friendId);
      // Cache.del("friendship_all_" + currentUserId);
      // Cache.del("friendship_all_" + friendId);
      // return res.send(new APIResult(200));
      return User.checkUserExists(friendId).then(function (result) {
        console.log(result)
        if (result) {
          return rongCloud.user.blacklist.add(Utility.encodeId(currentUserId), Utility.encodeId(friendId), function (err, resultText) {
            if (err) {
              return next(err);
            } else {
              return Blacklist.upsert({
                userId: currentUserId,
                friendId: friendId,
                status: true,
                timestamp: timestamp
              }).then(function () {
                return DataVersion.updateBlacklistVersion(currentUserId, timestamp).then(function () {
                  Cache.del("user_blacklist_" + currentUserId);
                  return Friendship.update({
                    status: FRIENDSHIP_DELETED,
                    displayName: '',
                    message: '',
                    timestamp: timestamp
                  }, {
                    where: {
                      userId: currentUserId,
                      friendId: friendId,
                      status: FRIENDSHIP_AGREED
                    }
                  });
                }).then(function () {
                  Cache.del("friendship_profile_displayName_" + currentUserId + "_" + friendId);
                  Cache.del("friendship_profile_user_" + currentUserId + "_" + friendId);
                  Cache.del("friendship_all_" + currentUserId);
                  Cache.del("friendship_all_" + friendId);
                  return res.send(new APIResult(200));
                });
              });
            }
          });
        } else {
          return res.status(404).send('friendId is not an available userId.');
        }
      })
    });
  })["catch"](next);
});

router.post('/set_display_name', function(req, res, next) {
  var currentUserId, displayName, friendId, timestamp;
  friendId = req.body.friendId;
  displayName = Utility.xss(req.body.displayName, FRIEND_REQUEST_MESSAGE_MAX_LENGTH);
  if ((displayName !== '') && !validator.isLength(displayName, FRIEND_DISPLAY_NAME_MIN_LENGTH, FRIEND_DISPLAY_NAME_MAX_LENGTH)) {
    return res.status(400).send('Length of displayName is out of limit.');
  }
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  return Friendship.update({
    displayName: displayName,
    timestamp: timestamp
  }, {
    where: {
      userId: currentUserId,
      friendId: friendId,
      status: FRIENDSHIP_AGREED
    }
  }).then(function(arg) {
    var affectedCount;
    affectedCount = arg[0];
    if (affectedCount === 0) {
      return res.status(404).send('Unknown friend user or invalid status.');
    }
    return DataVersion.updateFriendshipVersion(currentUserId, timestamp).then(function() {
      Cache.del("friendship_profile_displayName_" + currentUserId + "_" + friendId);
      Cache.del("friendship_all_" + currentUserId);
      return res.send(new APIResult(200));
    });
  })["catch"](next);
});

router.get('/all', function(req, res, next) {
  var currentUserId;
  currentUserId = Session.getCurrentUserId(req);
  return Cache.get("friendship_all_" + currentUserId).then(function(friends) {
    if (friends) {
      return res.send(new APIResult(200, friends));
    } else {
      return Friendship.findAll({
        where: {
          userId: currentUserId
        },
        attributes: ['displayName', 'message', 'status', 'updatedAt'],
        include: {
          model: User,
          attributes: ['id', 'nickname', 'region', 'phone', 'portraitUri','gender','stAccount','phone']
        }
      }).then(function(friends) {
        var results;
        results = Utility.encodeResults(friends, [['user', 'id']]);
        results = addUpdateTimeToList(results);
        Cache.set("friendship_all_" + currentUserId, results);
        return res.send(new APIResult(200, results));
      });
    }
  })["catch"](next);
});

router.get('/:friendId/profile', function(req, res, next) {
  var currentUserId, friendId;
  friendId = req.params.friendId;
  friendId = Utility.decodeIds(friendId);
  currentUserId = Session.getCurrentUserId(req);
  return Cache.get("friendship_profile_displayName_" + currentUserId + "_" + friendId).then(function(displayName) {
    return Friendship.findOne({
      where: {
        userId: currentUserId,
        friendId: friendId,
        status: FRIENDSHIP_AGREED
      },
      attributes: ['displayName'],
      include: {
        model: User,
        attributes: ['id', 'nickname', 'region', 'phone', 'portraitUri', 'createdAt', 'updatedAt', 'gender', 'stAccount']
      }
    }).then(function (friend) {
      if (!friend) {
        return res.status(403).send("Current user is not friend of user " + currentUserId + ".");
      }
      results = Utility.encodeResults(friend, [['user', 'id']]);
      results.user = addUpdateTimeToList([results.user])[0];
      Cache.set("friendship_profile_displayName_" + currentUserId + "_" + friendId, results.displayName);
      Cache.set("friendship_profile_user_" + currentUserId + "_" + friendId, results.user);
      return res.send(new APIResult(200, results));
    });
  })["catch"](next);
});

//获取通讯录手机信息
router.post('/get_contacts_info', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req),
    contactList = req.body.contactList;

  var registerUsers = {}, friendIdList = [];

  var defaultValue = {
    registered: 0,
    relationship: 0,
    stAccount: '',
    phone: 0,
    id: '',
    nickname: '',
    portraitUri: ''
  };
  
  return User.findAll({
    where: {
      phone: { $in: contactList}
    },
    attributes: ['id', 'phone', 'nickname', 'portraitUri', 'stAccount']
  }).then(function (userList) {
    var registerUserIdList = [];
    userList.forEach(function (user) {
      registerUserIdList.push(user.id);
      user = user.dataValues;
      registerUsers[user.phone] = user;
    });
    return Friendship.findAll({
      where: {
        userId: currentUserId,
        friendId: { $in: registerUserIdList },
        status: FRIENDSHIP_AGREED
      },
      attributes: ['friendId']
    });
  }).then(function (friendList) {
    friendIdList = friendList.map(function name(friend) {
      return friend.friendId;
    });
    contactList = contactList.map(function (phone) {
      var user = registerUsers[phone];
      var registered = user ? RegistrationStatus.REGISTERED : RegistrationStatus.UN_REGISTERED;
      var relationship = registered && friendIdList.indexOf(user.id) !== -1;
      relationship = relationship ? RelationshipStatus.IS_FRIEND : RelationshipStatus.NON_FRIEND;
      user = user || defaultValue;
      return Object.assign({}, user, {
        registered: registered,
        relationship: relationship,
        phone: phone,
        id: Utility.encodeId(user.id)
      });
    });
    return res.send(new APIResult(200, contactList));
  });
});

//设置备注和描述
router.post('/set_friend_description', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req),
    friendId = req.body.friendId,
    displayName = req.body.displayName,
    region = req.body.region,
    phone = req.body.phone,
    description = req.body.description,
    imageUri = req.body.imageUri;
  if(friendId == undefined) {
    return res.status(400).send('friendId is empty.');
  }
  console.log(typeof region,Number(region))
  var emptyRegion = phone && region == undefined;
  var emptyPhone = region && phone == undefined;
  if(emptyRegion || emptyPhone){
    return res.status(400).send('region or phone is empty.');
  }
  
  if(phone && region) {
    var region = Number(region);
    var regionName = regionMap[region];
    // if (regionName && !validator.isMobilePhone(phone, regionName)) {
    //   return res.status(400).send('Invalid region and phone number.');
    // }
  }
  return Friendship.findOne({
    where: {
      userId: currentUserId,
      friendId: friendId
    },
    attributes: ['displayName', 'region', 'phone', 'description', 'imageUri']
  }).then(function (result) {
    return Friendship.update({
      displayName: displayName == undefined ? result.displayName : displayName,
      region: region == undefined ? result.region : region,
      phone: phone == undefined ? result.phone : phone,
      description: description == undefined ? result.description : description,
      imageUri: imageUri == undefined ? result.imageUri : imageUri
    }, {
      where: {
        userId: currentUserId,
        friendId: friendId
      }
    }).then(function () {
      Cache.del("friendship_all_" + currentUserId);
      return res.send(new APIResult(200));
    })['catch'](next); 
  })
})

//获取备注和描述
router.post('/get_friend_description', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req),
    friendId = req.body.friendId;
  return Friendship.findOne({
    where: {
      userId: currentUserId,
      friendId: friendId
    },
    attributes: ['displayName', 'region', 'phone', 'description', 'imageUri']
  }).then(function (result) {
    // Utility.encodeResults(user)
    console.log(JSON.stringify(result))
    return res.send(new APIResult(200, result));
  })
})

//批量删除
router.post('/batch_delete', function (req, res, next) {
  var currentUserId = Session.getCurrentUserId(req),
    friendIds = req.body.friendIds,
    timestamp = Date.now();
  console.log(req.body, friendIds)
  return Friendship.update({
    status: FRIENDSHIP_DELETED,
    displayName: '',
    message: '',
    timestamp: timestamp
  },{
    where: {
      userId: currentUserId,
      friendId: {
        $in: friendIds
      },
      status: FRIENDSHIP_AGREED //FRIENDSHIP_DELETED
    }
  }).then(function (result) {
    //更新成功后添加到 IM 黑名单
    rongCloud.user.blacklist.addCustom({
      userId: Utility.encodeId(currentUserId)
    },[ { field: 'blackUserId', values: Utility.encodeIds(friendIds)}],function(){
      Cache.del("friendship_all_" + currentUserId);
      return res.send(new APIResult(200, result));
    })
  });
})
module.exports = router;
