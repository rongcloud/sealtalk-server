/*
  云片短信服务
 */
var request = require('http').request,
  qs = require('querystring'),
  _ = require('underscore');

var Config = require('../conf.js'),
  apikey = Config.YUNPIAN_API_KEY,
  smsHost = Config.YUNPIAN_SMS_HOST || 'sms.yunpian.com',
  internalSmsHost = Config.YUNPIAN_INTERNAL_SMS_HOST || 'us.yunpian.com',
  sendSmsUri = Config.YUNPIAN_SEND_SMS_URI || '/v2/sms/tpl_single_send.json',
  getSmsTplUrl = Config.YUNPIAN_GET_TPL_URI || '/v2/tpl/get.json',
  tplValueKey = '#code#',
  chineseRegion = '86';

var ErrorCodeMap = {
  ypServerFailed: {
    code: 3000,
    msg: 'YunPian server error' // 云片服务错误
  },
  tplFailed: {
    code: 3001,
    msg: 'Failed to get YunPian template' // 获取云片模板失败
  },
  tplEmpty: {
    code: 3002,
    msg: 'YunPian SMS template is empty' // 云片模板为空
  },
  sendFaild: {
    code: 3003,
    msg: 'Send YunPian SMS code failed' // 发送验证码失败
  },
  violation: {
    code: 3004,
    msg: 'Too many times sent' // ip 发送次数过多
  }
};

// 云片错误码文档: https://www.yunpian.com/doc/zh_CN/returnValue/common.html
var getErrorCodeByYP = function (ypCode) {
  if (ypCode < 0) {
    ypCode = 100 - ypCode;
  }
  // 云片错误码已 3100 开头
  return 3100 + Number(ypCode);
};

var getTplLangByRegion = function (region) {
  // 地区对应的模板语言
  var regionMapTplLang = {
    '86': 'zh_cn', // 中国大陆
    '852': 'zh_tw', // 中国香港
    '853': 'zh_tw', // 中国澳门
    '886': 'zh_tw', // 中国台湾
    '81': 'ja', // 日本
    '82': 'ko', // 韩国
    'other': 'en' // 其他都为英文
  };
  return regionMapTplLang[region] || regionMapTplLang['other'];
};

var generateSMSCode = function () {
  return _.random(1000, 9999);
};

var post = function (url, content, region) {
  var isChinese = region == chineseRegion;
  var hostname = isChinese ? smsHost : internalSmsHost;
  var body = qs.stringify(content);
  var options = {
    hostname: hostname,
    path: url,
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
    }
  };
  return new Promise(function (resolve, reject) {
    var req = request(options, function (res) {
      res.setEncoding('utf8');
      res.on('data', function (resultText) {
        try {
          var result = JSON.parse(resultText);
          resolve(result);
        } catch(err) {
          reject(err);
        }
      });
    });
    req.on('error', function (err) {
      reject(err);
    });
    req.write(body);
    req.end();
  });
};

/**
 * 根据地区和模板列表 匹配模板, 获取模板 id
 * @param  {Number} region   地区号, 如 86
 * @param  {Array} tempList  模板列表
 * @return {String}          模板 id
 */
var getTplIdByList = function (list, region) {
  var tempLang = getTplLangByRegion(region);
  var regionTplList = list.filter(function (temp) {
    temp = temp || {};
    return temp.lang === tempLang.toLowerCase();
  });
  var tplListLength = regionTplList.length;
  var tpl = tplListLength ? regionTplList[tplListLength - 1] : {};
  tpl = tpl || {};
  return tpl.tpl_id;
};

/**
 * 根据地区, 获取模板 id
 * 实现: 先从云片服务器拿取模板列表, 再根据地区匹配对应模板
 */
var getSmsTplId = function (region) {
  var content = {
    apikey: apikey
  };
  return new Promise(function (resolve, reject) {
    post(getSmsTplUrl, content, region).then(function (tempList) {
      if (!_.isArray(tempList)) {
        return reject(ErrorCodeMap.tplFailed);
      }
      var isEmptyTemp = !tempList.length;
      if (isEmptyTemp) {
        return reject(ErrorCodeMap.tplEmpty);
      }
      var tplId = getTplIdByList(tempList, region);
      tplId ? resolve(tplId) : reject(ErrorCodeMap.tplFailed);
    }, function () {
      reject(ErrorCodeMap.ypServerFailed);
    });
  });
};

var sendCode = function (region, phone) {
  var send = function (tplId) {
    var code = generateSMSCode();
    var tplValue = {};
    tplValue[tplValueKey] = code;
    var regionPrefix = `+${region}`;
    phone = `${regionPrefix}${phone}`;
    var content = {
      apikey: apikey,
      mobile: phone,
      tpl_id: tplId,
      tpl_value: qs.stringify(tplValue)
    };
    return new Promise(function (resolve, reject) {
      post(sendSmsUri, content, region).then(function (result) {
        result = result || {};
        if (result.code === 0) { // 云片 0 为调用成功
          result.sessionId = code;
          resolve(result);
        } else if (result.code) {
          result.code = getErrorCodeByYP(result.code);
          reject(result);
        } else {
          reject(ErrorCodeMap.sendFaild);
        }
      }, function () {
        reject(ErrorCodeMap.ypServerFailed);
      });
    });
  };

  return getSmsTplId(region).then(function (tplId) {
    return send(tplId);
  });
};

module.exports = {
  sendCode,
  ErrorCodeMap
};