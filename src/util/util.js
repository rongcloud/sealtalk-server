var APIResult, Config, HTTPError, N3D, Utility, crypto, debug, process, xss,
  extend = function(child, parent) { for (var key in parent) { if (hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
  hasProp = {}.hasOwnProperty;

crypto = require('crypto');

process = require('process');

debug = require('debug');

xss = require('xss');

Config = require('../conf');

N3D = require('./n3d');

Utility = (function() {
  function Utility() {}

  Utility.n3d = new N3D(Config.N3D_KEY, 1, 4294967295);

  Utility.log = debug('app:log');

  Utility.logPath = debug('app:path');

  Utility.logError = debug('app:error');

  Utility.logResult = debug('app:result');

  Utility.encryptText = function(text, password) {
    var cipher, crypted, salt;
    salt = this.random(1000, 9999);
    text = salt + '|' + text + '|' + Date.now();
    cipher = crypto.createCipher('aes-256-ctr', password);
    crypted = cipher.update(text, 'utf8', 'hex');
    return crypted += cipher.final('hex');
  };

  Utility.decryptText = function(text, password) {
    var dec, decipher, strs;
    decipher = crypto.createDecipher('aes-256-ctr', password);
    dec = decipher.update(text, 'hex', 'utf8');
    dec += decipher.final('utf8');
    strs = dec.split('|');
    if (strs.length !== 3) {
      throw new Error('Invalid cookie value!');
    }
    return strs[1];
  };

  Utility.hash = function(text, salt) {
    var sha1;
    text = text + '|' + salt;
    sha1 = crypto.createHash('sha1');
    sha1.update(text, 'utf8');
    return sha1.digest('hex');
  };

  Utility.getFromArrayById = function (array, id, key) {
    var item = array.filter(function (obj) {
      var objId = key ? obj[key] : obj.id;
      return objId === id;
    });
    if (!item || !item.length) {
      return;
    }
    return item[0];
  };

  Utility.random = function(min, max) {
    return Math.floor(Math.random() * (max - min)) + min;
  };

  Utility.isEmpty = function(obj) {
    return obj === '' || obj === null || obj === void 0 || (Array.isArray(obj) && obj.length === 0);
  };

  Utility.decodeIds = function(obj) {
    if (obj === null) {
      return null;
    }
    if (Array.isArray(obj)) {
      return obj.map(function(element) {
        if (typeof element !== 'string') {
          return null;
        }
        return Utility.stringToNumber(element);
      });
    } else if (typeof obj === 'string') {
      return Utility.stringToNumber(obj);
    } else {
      return null;
    }
  };

  Utility.encodeId = function(str) {
    return Utility.numberToString(str);
  };
  Utility.encodeIds = function (idList) {
    return idList.map(function (id) {
      return Utility.encodeId(id);
    });
  }

  Utility.encodeResults = function(results, keys) {
    var isSubArrayKey, replaceKeys, retVal;
    replaceKeys = function(obj) {
      if (obj === null) {
        return null;
      }
      if (isSubArrayKey) {
        keys.forEach(function(key) {
          var subObj;
          subObj = obj[key[0]];
          if (subObj) {
            if (subObj[key[1]]) {
              return subObj[key[1]] = Utility.numberToString(subObj[key[1]]);
            }
          }
        });
      } else {
        keys.forEach(function(key) {
          if (obj[key]) {
            return obj[key] = Utility.numberToString(obj[key]);
          }
        });
      }
      return obj;
    };
    if (results === null) {
      return null;
    }
    if (results.toJSON) {
      results = results.toJSON();
    }
    if (!keys) {
      keys = 'id';
    }
    if (typeof keys === 'string') {
      keys = [keys];
    }
    isSubArrayKey = keys.length > 0 && Array.isArray(keys[0]);
    if (Array.isArray(results)) {
      retVal = results.map(function(item) {
        if (item.toJSON) {
          item = item.toJSON();
        }
        return replaceKeys(item);
      });
    } else {
      retVal = replaceKeys(results);
    }
    return retVal;
  };

  Utility.stringToNumber = function(str) {
    try {
      return this.n3d.decrypt(str);
    } catch (_error) {
      return null;
    }
  };

  Utility.numberToString = function(num) {
    try {
      return this.n3d.encrypt(num);
    } catch (_error) {
      return null;
    }
  };

  Utility.xss = function(str, maxLength) {
    var result;
    result = xss(str, {
      whiteList: []
    });
    if (str.length <= maxLength) {
      if (result.length > maxLength) {
        return result.substr(0, maxLength);
      }
    }
    return result;
  };

  return Utility;

})();

APIResult = (function() {
  function APIResult(code, result1, message) {
    this.code = code;
    this.result = result1;
    this.message = message;
    if (this.code === null || this.code === void 0) {
      throw new Error('Code is null.');
    }
    Utility.logResult(JSON.stringify(this));
    if (this.result === null) {
      delete this.result;
    }
    if (this.message === null || process.env.NODE_ENV !== 'development') {
      delete this.message;
    }
  }

  return APIResult;

})();

HTTPError = (function(superClass) {
  extend(HTTPError, superClass);

  function HTTPError(message, statusCode) {
    this.message = message;
    this.statusCode = statusCode;
  }

  return HTTPError;

})(Error);

var getClientIp = function (req) {
  var ipStr = req.headers['x-forwarded-for'] ||
      req.ip ||
      req.connection.remoteAddress ||
      req.socket.remoteAddress ||
      req.connection.socket.remoteAddress || '';
  var ipReg = /\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}/;
  if (ipStr.split(',').length > 0) {
      ipStr = ipStr.split(',')[0]
  }
  var ip = ipReg.exec(ipStr);
  return ip[0];
};

var formatRegion = function (region) {
  region = String(region);
  var plusPrefix = region.indexOf('+');
  if (plusPrefix > -1) {
    region = region.substring(plusPrefix + 1);
  }
  return region;
};

var isString = function (str) {
  return Object.prototype.toString.call(str) === '[object String]';
};

var isObject = function (obj) {
  return Object.prototype.toString.call(obj) === '[object Object]';
};

var isArray = function (obj) {
  return Object.prototype.toString.call(obj) === '[object Array]';
};

var isInt = function (number) {
  number = parseInt(number);
  return Object.prototype.toString.call(number) === '[object Number]' && !isNaN(number);
};

var addUpdateTimeToList = (list, options) => {
  options = options || {};
  var objName = options.objName,
    updateKeys = options.updateKeys || ['updatedAt', 'createdAt'];
  var updateKeyMap = {
    updatedAt: 'updatedTime',
    createdAt: 'createdTime'
  };
  list.forEach((item) => {
    item = (objName ? item[objName] : item) || {};
    updateKeys.forEach((updateKey) => {
      var updatedData = item[updateKey];
      if (updatedData) {
        updateKey = updateKeyMap[updateKey] || updateKey + 'Time';
        item[updateKey] = +new Date(updatedData);
      }
    });
  });
  return list;
};

module.exports = {
  Utility,
  APIResult,
  HTTPError,

  getClientIp,
  formatRegion,

  isString,
  isObject,
  isArray,
  isInt,

  addUpdateTimeToList
};