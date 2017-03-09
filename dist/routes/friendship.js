var APIResult, Blacklist, CONTACT_OPERATION_ACCEPT_RESPONSE, CONTACT_OPERATION_REQUEST, Cache, Config, DataVersion, Department, DepartmentMember, Duty, FRIENDSHIP_AGREED, FRIENDSHIP_DELETED, FRIENDSHIP_IGNORED, FRIENDSHIP_REQUESTED, FRIENDSHIP_REQUESTING, FRIEND_DISPLAY_NAME_MAX_LENGTH, FRIEND_DISPLAY_NAME_MIN_LENGTH, FRIEND_REQUEST_MESSAGE_MAX_LENGTH, FRIEND_REQUEST_MESSAGE_MIN_LENGTH, Friendship, Group, GroupMember, GroupSync, LoginLog, Session, User, Utility, VerificationCode, express, moment, ref, rongCloud, router, sendContactNotification, sequelize, validator;

express = require('express');

moment = require('moment');

rongCloud = require('rongcloud-sdk');

Config = require('../conf');

Cache = require('../util/cache');

Session = require('../util/session');

Utility = require('../util/util').Utility;

APIResult = require('../util/util').APIResult;

ref = require('../db'), sequelize = ref[0], User = ref[1], Blacklist = ref[2], Friendship = ref[3], Duty = ref[4], Department = ref[5], DepartmentMember = ref[6], Group = ref[7], GroupMember = ref[8], GroupSync = ref[9], DataVersion = ref[10], VerificationCode = ref[11], LoginLog = ref[12];

FRIENDSHIP_REQUESTING = 10;

FRIENDSHIP_REQUESTED = 11;

FRIENDSHIP_AGREED = 20;

FRIENDSHIP_IGNORED = 21;

FRIENDSHIP_DELETED = 30;

FRIEND_REQUEST_MESSAGE_MIN_LENGTH = 0;

FRIEND_REQUEST_MESSAGE_MAX_LENGTH = 64;

FRIEND_DISPLAY_NAME_MIN_LENGTH = 1;

FRIEND_DISPLAY_NAME_MAX_LENGTH = 32;

CONTACT_OPERATION_ACCEPT_RESPONSE = 'AcceptResponse';

CONTACT_OPERATION_REQUEST = 'Request';

rongCloud.init(Config.RONGCLOUD_APP_KEY, Config.RONGCLOUD_APP_SECRET);

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
  Utility.log('Sending ContactNotificationMessage:', JSON.stringify(contactNotificationMessage));
  return rongCloud.message.system.publish(encodedUserId, [encodedFriendId], 'RC:ContactNtf', contactNotificationMessage, function(err, resultText) {
    if (err) {
      return Utility.logError('Error: send contact notification failed: %j', err);
    }
  });
};

router = express.Router();

validator = sequelize.Validator;

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
  return Promise.all([
    Friendship.getInfo(currentUserId, friendId), Friendship.getInfo(friendId, currentUserId), Blacklist.findOne({
      where: {
        userId: friendId,
        friendId: currentUserId
      },
      attributes: ['status']
    })
  ]).then(function(arg) {
    var action, blacklist, fd, fdStatus, fg, fgStatus, resultMessage, unit;
    fg = arg[0], fd = arg[1], blacklist = arg[2];
    Utility.log('Friendship requesting: %j', fg);
    Utility.log('Friendship requested:  %j', fd);
    if (blacklist && blacklist.status) {
      Utility.log('Invite result: %s %s', 'None: blacklisted by friend', 'Do nothing.');
      return res.send(new APIResult(200, {
        action: 'None'
      }, 'Do nothing.'));
    }
    action = 'Added';
    resultMessage = 'Friend added.';
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
              return DataVersion.updateFriendshipVersion(friendId, timestamp).then(function() {
                Session.getCurrentUserNickname(currentUserId, User).then(function(nickname) {
                  return sendContactNotification(currentUserId, nickname, friendId, CONTACT_OPERATION_REQUEST, message, timestamp);
                });
                Cache.del("friendship_all_" + currentUserId);
                Cache.del("friendship_all_" + friendId);
                Utility.log('Invite result: %s %s', action, resultMessage);
                return res.send(new APIResult(200, {
                  action: action
                }, resultMessage));
              });
            } else {
              Cache.del("friendship_all_" + currentUserId);
              Cache.del("friendship_all_" + friendId);
              Utility.log('Invite result: %s %s', action, resultMessage);
              return res.send(new APIResult(200, {
                action: action
              }, resultMessage));
            }
          });
        });
      });
    } else {
      if (friendId === currentUserId) {
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
              return res.send(new APIResult(200, {
                action: 'Sent'
              }, 'Request sent.'));
            });
          });
        });
      }
    }
  })["catch"](next);
});

router.post('/agree', function(req, res, next) {
  var currentUserId, friendId, timestamp;
  friendId = req.body.friendId;
  currentUserId = Session.getCurrentUserId(req);
  timestamp = Date.now();
  Utility.log('%s agreed to user -> %s', currentUserId, friendId);
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
      Cache.del("friendship_profile_user_" + currentUserId + "_" + friendId);
      Cache.del("friendship_all_" + currentUserId);
      Cache.del("friendship_all_" + friendId);
      return res.send(new APIResult(200));
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
          attributes: ['id', 'nickname', 'region', 'phone', 'portraitUri']
        }
      }).then(function(friends) {
        var results;
        results = Utility.encodeResults(friends, [['user', 'id']]);
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
    return Cache.get("friendship_profile_user_" + currentUserId + "_" + friendId).then(function(profile) {
      var results;
      if (displayName && profile) {
        results = {
          displayName: displayName,
          user: profile
        };
        return res.send(new APIResult(200, results));
      } else {
        return Friendship.findOne({
          where: {
            userId: currentUserId,
            friendId: friendId,
            status: FRIENDSHIP_AGREED
          },
          attributes: ['displayName'],
          include: {
            model: User,
            attributes: ['id', 'nickname', 'region', 'phone', 'portraitUri']
          }
        }).then(function(friend) {
          if (!friend) {
            return res.status(403).send("Current user is not friend of user " + currentUserId + ".");
          }
          results = Utility.encodeResults(friend, [['user', 'id']]);
          Cache.set("friendship_profile_displayName_" + currentUserId + "_" + friendId, results.displayName);
          Cache.set("friendship_profile_user_" + currentUserId + "_" + friendId, results.user);
          return res.send(new APIResult(200, results));
        });
      }
    });
  })["catch"](next);
});

module.exports = router;
