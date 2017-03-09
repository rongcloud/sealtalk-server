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

module.exports.Utility = Utility;

module.exports.APIResult = APIResult;

module.exports.HTTPError = HTTPError;
