var APIResult, Blacklist, Cache, Config, Department, DepartmentMember, Duty, FRIENDSHIP_AGREED, Friendship, Group, GroupMember, Session, User, Utility, _, express, jsonfile, path, ref, rongCloud, router, semver, sequelize;

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

ref = require('../db'), sequelize = ref[0], User = ref[1], Blacklist = ref[2], Friendship = ref[3], Duty = ref[4], Department = ref[5], DepartmentMember = ref[6], Group = ref[7], GroupMember = ref[8];

FRIENDSHIP_AGREED = 20;

rongCloud.init(Config.RONGCLOUD_APP_KEY, Config.RONGCLOUD_APP_SECRET);

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

module.exports = router;
