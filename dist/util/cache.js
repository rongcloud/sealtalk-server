var Cache, Config, LRU, Utility;

LRU = require("lru-cache");

Config = require('../conf');

Utility = require('./util').Utility;

Cache = (function() {
  function Cache() {}

  Cache.cache = LRU({
    max: 100000,
    maxAge: 3600000
  });

  Cache.set = function(key, value) {
    Utility.log("Cache: set '%s'.", key);
    return Promise.resolve(this.cache.set(key, value));
  };

  Cache.get = function(key) {
    Utility.log("Cache: get '%s'.", key);
    return Promise.resolve(this.cache.get(key));
  };

  Cache.del = function(key) {
    Utility.log("Cache: del '%s'.", key);
    return Promise.resolve(this.cache.del(key));
  };

  return Cache;

})();

module.exports = Cache;
