var APIResult, Blacklist, Cache, Config, DataVersion, Department, DepartmentMember, Duty, Friendship, Group, GroupMember, GroupSync, HTTPError, LoginLog, Session, User, Utility, VerificationCode, _, co, express, ref, rongCloud, router, sequelize;

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

ref = require('../db'), sequelize = ref[0], User = ref[1], Blacklist = ref[2], Friendship = ref[3], Duty = ref[4], Department = ref[5], DepartmentMember = ref[6], Group = ref[7], GroupMember = ref[8], GroupSync = ref[9], DataVersion = ref[10], VerificationCode = ref[11], LoginLog = ref[12];

router = express.Router();

router.get('/:id', function(req, res, next) {
  var departId;
  departId = req.params.id;
  departId = Utility.decodeIds(departId);
  return Cache.get("depart_" + departId).then(function(depart) {
    if (depart) {
      return res.send(new APIResult(200, depart));
    } else {
      return Department.findById(departId, {
        attributes: ['id', 'deptName', 'sort', 'parentId', 'timestamp']
      }).then(function(depart) {
        var results;
        if (!depart) {
          return res.status(404).send('Unknow department.');
        }
        results = Utility.encodeResults(depart, ['id', 'parentId']);
        Cache.set("depart_" + departId, results);
        return res.send(new APIResult(200, results));
      });
    }
  })["catch"](next);
});

router.get('/:id/members', function(req, res, next) {
  var currentUserId, departId;
  departId = req.params.id;
  departId = Utility.decodeIds(departId);
  currentUserId = Session.getCurrentUserId(req);
  return DepartmentMember.findAll({
    where: {
      userId: currentUserId
    },
    attributes: ['deptId', 'userId', 'displayName', 'managerId', 'timestamp']
  }).then(function(departMembers) {
    if (departMembers.length === 0) {
      return res.status(403).send('Have no members.');
    }
    return Cache.get("depart_members_" + departId).then(function(departMembers) {
      if (departMembers) {
        return res.send(new APIResult(200, departMembers));
      } else {
        return DepartmentMember.findAll({
          where: {
            deptId: departId
          },
          attributes: ['deptId', 'userId', 'displayName', 'managerId', 'timestamp'],
          include: [
            {
              model: User,
              attributes: ['id', 'nickname', 'portraitUri', 'region', 'phone', 'email']
            }, {
              model: Duty,
              attributes: ['dutyName']
            }
          ]
        }).then(function(departMembers) {
          var results;
          if (departMembers.length === 0) {
            return res.status(404).send('Have no members.');
          }
          results = Utility.encodeResults(departMembers, ['deptId', 'userId', 'managerId']);
          results = Utility.encodeResults(results, [['user', 'id']]);
          Cache.set("depart_members_" + departId, results);
          return res.send(new APIResult(200, results));
        });
      }
    })["catch"](next);
  });
});

router.get('/', function(req, res, next) {
  var currentUserId, parentid;
  currentUserId = Session.getCurrentUserId(req);
  parentid = req.query.parentid;
  if (parentid) {
    parentid = Utility.decodeIds(parentid);
  } else {
    parentid = 0;
  }
  return DepartmentMember.findAll({
    where: {
      userId: currentUserId
    },
    attributes: ['deptId', 'userId', 'displayName', 'managerId', 'timestamp']
  }).then(function(departMembers) {
    if (departMembers.length === 0) {
      return res.status(403).send('Have no members.');
    }
    return Cache.get("depart_list_" + parentid).then(function(departs) {
      if (departs) {
        return res.send(new APIResult(200, departs));
      } else {
        return Department.findAll({
          where: {
            parentid: parentid
          },
          attributes: ['id', 'deptName', 'sort', 'timestamp', 'parentId']
        }).then(function(departs) {
          var results;
          if (departs.length === 0) {
            return res.status(404).send('Have no departments.');
          }
          results = Utility.encodeResults(departs, ['id', 'parentId']);
          Cache.set("depart_list_" + parentid, results);
          return res.send(new APIResult(200, results));
        });
      }
    })["catch"](next);
  });
});

module.exports = router;
