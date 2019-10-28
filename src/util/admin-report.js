/* 
此文件实现融云注册统计功能, 开发者可忽略
 */
var https = require('https'),
  qs = require('querystring');

var Utility = require('./util').Utility;

var DEMO_TYPE = 1; // SealTalk

var ADMIN_REPORT_URL = 'admin report url';

var request = (options) => {
  return new Promise((resolve, reject) => {
    var content = options.content;

    var req = https.request(options, function (res) {
      res.setEncoding('utf8');
      res.on('data', function (chunk) {
        resolve(chunk);
      });
    });
    req.on('error', function (e) {
      reject(e);
    });

    req.write(content);
    req.end();
  });
};

var reportRegister = (phone, region, isDebug) => {
  // TODO 上报至您的管理后台
};

module.exports = {
  reportRegister
};