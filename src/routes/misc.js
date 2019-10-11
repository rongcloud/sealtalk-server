var APIResult, Blacklist, Cache, Config, FRIENDSHIP_AGREED, Friendship, Group, GroupMember, Session, User, ScreenStatus, Utility, _, express, jsonfile, path, ref, rongCloud, router, semver, sequelize ;

express = require('express');

_ = require('underscore');

jsonfile = require('jsonfile');

path = require('path');

semver = require('semver');

rongCloud = require('rongcloud-sdk');

Config = require('../conf');

Cache = require('../util/cache');

Session = require('../util/session');

Utility = require('../util/util').Utility;

APIResult = require('../util/util').APIResult;

ref = require('../db'), sequelize = ref[0], User = ref[1], Blacklist = ref[2], Friendship = ref[3], Group = ref[4], GroupMember = ref[5], ScreenStatus = ref[14];

FRIENDSHIP_AGREED = 20;

rongCloud.init(Config.RONGCLOUD_APP_KEY, Config.RONGCLOUD_APP_SECRET, {
  api: Config.RONGCLOUD_API_URL
});

router = express.Router();

router.get('/latest_update', function(req, res, next) {
  var clientVersion, err;
  clientVersion = req.query.version;
  try {
    return Cache.get('latest_update').then(function(squirrelConfig) {
      if (!squirrelConfig) {
        squirrelConfig = jsonfile.readFileSync(path.join(__dirname, '../squirrel.json'));
        Cache.set('latest_update', squirrelConfig);
      }
      if ((semver.valid(clientVersion) === null) || (semver.valid(squirrelConfig.version) === null)) {
        return res.status(400).send('Invalid version.');
      }
      if (semver.gte(clientVersion, squirrelConfig.version)) {
        return res.status(204).end();
      } else {
        return res.send(squirrelConfig);
      }
    });
  } catch (_error) {
    err = _error;
    return next(err);
  }
});

router.get('/client_version', function(req, res, next) {
  var err;
  try {
    return Cache.get('client_version').then(function(clientVersionInfo) {
      if (!clientVersionInfo) {
        clientVersionInfo = jsonfile.readFileSync(path.join(__dirname, '../client_version.json'));
        Cache.set('client_version', clientVersionInfo);
      }
      return res.send(clientVersionInfo);
    });
  } catch (_error) {
    err = _error;
    return next(err);
  }
});

router.get('/mobile_version', function (req, res, next) {
  var err;
  try {
    return Cache.get('client_version').then(function (clientVersionInfo) {
      if (!clientVersionInfo) {
        clientVersionInfo = jsonfile.readFileSync(path.join(__dirname, '../client_version.json'));
        Cache.set('client_version', clientVersionInfo);
      }
      return res.send(new APIResult(200, clientVersionInfo));
    });
  } catch (_error) {
    err = _error;
    return next(err);
  }
});

router.get('/demo_square', function(req, res, next) {
  var demoSquareData, err, groupIds;
  try {
    demoSquareData = jsonfile.readFileSync(path.join(__dirname, '../demo_square.json'));
    groupIds = _.chain(demoSquareData).where({
      type: 'group'
    }).pluck('id').value();
    return Group.findAll({
      where: {
        id: {
          $in: groupIds
        }
      },
      attributes: ['id', 'name', 'portraitUri', 'memberCount']
    }).then(function(groups) {
      demoSquareData.forEach(function(item) {
        var group;
        if (item.type === 'group') {
          group = _.findWhere(groups, {
            id: item.id
          });
          if (!group) {
            group = {
              name: 'Unknown',
              portraitUri: '',
              memberCount: 0
            };
          }
          item.name = group.name;
          item.portraitUri = group.portraitUri;
          item.memberCount = group.memberCount;
          return item.maxMemberCount = group.maxMemberCount;
        }
      });
      return res.send(new APIResult(200, Utility.encodeResults(demoSquareData)));
    });
  } catch (_error) {
    err = _error;
    return next(err);
  }
});

router.post('/send_message', function(req, res, next) {
  var content, conversationType, currentUserId, encodedCurrentUserId, encodedTargetId, objectName, pushContent, targetId;
  conversationType = req.body.conversationType;
  targetId = req.body.targetId;
  objectName = req.body.objectName;
  content = req.body.content;
  pushContent = req.body.pushContent;
  encodedTargetId = req.body.encodedTargetId;
  currentUserId = Session.getCurrentUserId(req);
  encodedCurrentUserId = Utility.encodeId(currentUserId);
  switch (conversationType) {
    case 'PRIVATE':
      return Friendship.count({
        where: {
          userId: currentUserId,
          friendId: targetId,
          status: FRIENDSHIP_AGREED
        }
      }).then(function(count) {
        if (count > 0) {
          return rongCloud.message["private"].publish(encodedCurrentUserId, encodedTargetId, objectName, content, pushContent, function(err, resultText) {
            if (err) {
              Utility.logError('Error: send message failed: %j', err);
              throw err;
            }
            return res.send(new APIResult(200));
          });
        } else {
          return res.status(403).send("User " + encodedTargetId + " is not your friend.");
        }
      });
    case 'GROUP':
      return GroupMember.count({
        where: {
          groupId: targetId,
          memberId: currentUserId
        }
      }).then(function(count) {
        if (count > 0) {
          return rongCloud.message.group.publish(encodedCurrentUserId, encodedTargetId, objectName, content, pushContent, function(err, resultText) {
            if (err) {
              Utility.logError('Error: send message failed: %j', err);
              throw err;
            }
            return res.send(new APIResult(200));
          });
        } else {
          return res.status(403).send("Your are not member of Group " + encodedTargetId + ".");
        }
      });
    default:
      return res.status(403).send('Unsupported conversation type.');
  }
});
/**
 * 发送开关截屏通知消息
 * @param {*} userId 
 * @param {*} targetId 
 * @param {*} conversationType 
 * @param {*} operation 操作： openScreenNtf: 打开、  closeScreenNtf: 关闭 ，sendScreenNtf： 屏幕通知消息
 */
var sendScreenMsg = function(userId, targetId, conversationType, operation) {
  var encodedUserId, message, objectName;
  objectName = 'ST:ConNtf';
  encodedUserId = Utility.encodeId(userId);
  encodedTargetId = Utility.encodeId(targetId);
  message = {
    operatorUserId: encodedUserId,
    operation: operation,
  };
  console.log('send msg--',encodedUserId,encodedTargetId,message)
  switch(conversationType) {
    case 1:
      return new Promise(function (resolve, reject) {
        rongCloud.message.private.publishCus({
          fromUserId: encodedUserId,
          objectName: objectName,
          content: JSON.stringify(message),
          isIncludeSender: 1
        }, [{ field: 'toUserId', values: [encodedTargetId] }], function (err, resultText) {
          if (err) {
            reject(err);
            return Utility.logError('Error: send contact notification failed: %j', err);
          }
          return resolve(resultText);
        });
      })
    case 3:
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
    default:
      console.log('Unsupported conversation type')
  }
  
}

// 设置截屏通知状态
router.post('/set_screen_capture', function(req, res, next) {
  var conversationType = Number(req.body.conversationType),
    currentUserId = Session.getCurrentUserId(req);
    targetId = req.body.targetId,
    noticeStatus = req.body.noticeStatus;
  // console.log(req.body)
  var operateId = targetId;
  var statusContent = noticeStatus ==  0 ? 'closeScreenNtf' : 'openScreenNtf';
  if (conversationType == 1) {
    operateId = currentUserId < targetId ? currentUserId + '_' + targetId : targetId + '_' + currentUserId;
  }
  //获取用户名
  console.log(typeof conversationType)
  User.findOne({
    where: {
      id: currentUserId
    },
    attributes: ['nickname']
  }).then(function (user) {
    return ScreenStatus.findOne({
      where: {
        operateId: operateId,
        conversationType: conversationType
      }
    }).then(function(result){
      if(result) {
        console.log('update---')
        // update
        return ScreenStatus.update({
          status: noticeStatus
        },{
          where: {
            operateId: operateId,
            conversationType: conversationType
          }
        }).then(function() {
          //send msg
          return sendScreenMsg(currentUserId,targetId,conversationType,statusContent).then(function(result) {
            // console.log(result)
            return res.send(new APIResult(200));
          })['catch'](next)
        })
      }else {
        // create
        console.log('create---')
        return ScreenStatus.create({
          operateId: operateId,
          conversationType: conversationType,
          status: noticeStatus
        }).then(function () {
          //send msg
          return sendScreenMsg(currentUserId,targetId,conversationType,statusContent).then(function(result) {
            // console.log(result)
            return res.send(new APIResult(200));
          })
        })
      }
    })
  })['catch'](next)
})

// 获取截屏通知状态
router.post('/get_screen_capture', function(req, res, next){
  var conversationType = req.body.conversationType,
    currentUserId = Session.getCurrentUserId(req);
    targetId = req.body.targetId;
  var operateId = targetId;
  if (conversationType == 1) {
    operateId = currentUserId < targetId ? currentUserId + '_' + targetId : targetId + '_' + currentUserId;
  }
  return ScreenStatus.findOne({
    where: {
      operateId: operateId,
      conversationType: conversationType
    },
    attributes: ["status"]
  }).then(function (result) {
    if(!result) {
      return res.send(new APIResult(200,{status:0}));
    }
    return res.send(new APIResult(200,Utility.encodeResults(result)));
  })["catch"](next)
})

// 发送屏幕通知消息
router.post('/send_sc_msg', function(req, res, next) {
  console.log('req',req.body)
  var currentUserId = Session.getCurrentUserId(req),
    conversationType = req.body.conversationType,
    targetId = req.body.targetId;
  // console.log(currentUserId)
  sendScreenMsg(currentUserId,targetId,conversationType,'sendScreenNtf').then(function(result) {
    console.log(result)
    return res.send(new APIResult(200));
  })['catch'](next);
})
module.exports = router;
