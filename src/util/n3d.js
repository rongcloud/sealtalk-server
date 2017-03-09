var N3D;

N3D = (function() {
  var isUnsigned;

  function N3D(key, lower, upper) {
    var a, charMap, i, j, k, l, n, ref, s;
    this.lower = lower;
    this.upper = upper;
    charMap = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');
    this.radix = charMap.length;
    this.dict = [];
    this.keyCode = 0;
    if (!isUnsigned(this.lower) || !isUnsigned(this.upper)) {
      throw new Error('Parameter is error.');
    }
    if (this.upper <= this.lower) {
      throw new Error('The upper must be greater than the lower.');
    }
    if (typeof key !== 'string' || key.length === 0) {
      throw new Error('The key is error.');
    }
    for (i = n = 0, ref = key.length; 0 <= ref ? n < ref : n > ref; i = 0 <= ref ? ++n : --n) {
      a = key.charCodeAt(i);
      if (a > 127) {
        throw new Error('The key is error.');
      }
      this.keyCode += a * Math.pow(128, i % 7);
    }
    if (this.keyCode + this.radix < this.upper) {
      throw new Error('The secret key is too short.');
    }
    i = this.keyCode - this.radix;
    j = 0;
    while (i < this.keyCode) {
      this.dict[j] = [];
      k = this.radix;
      l = 0;
      while (k > 0) {
        s = i % k;
        this.dict[j][l] = charMap[s];
        charMap[s] = charMap[k - 1];
        k--;
        l++;
      }
      charMap = this.dict[j].slice(0);
      i++;
      j++;
    }
  }

  isUnsigned = function(num) {
    return Math.floor(num) === num && num > 0 && num < Number.MAX_VALUE;
  };

  N3D.prototype.encrypt = function(num) {
    var m, map, result, s;
    if (!isUnsigned(num) || num > this.upper || num < this.lower) {
      throw new Error('Parameter is error.');
    }
    num = this.keyCode - num;
    result = [];
    m = num % this.radix;
    map = this.dict[m];
    s = 0;
    result.push(this.dict[0][m]);
    while (num > this.radix) {
      num = (num - m) / this.radix;
      m = num % this.radix;
      if ((s = m + s) >= this.radix) {
        s -= this.radix;
      }
      result.push(map[s]);
    }
    return result.join('');
  };

  N3D.prototype.decrypt = function(str) {
    var chars, i, j, len, map, n, ref, result, s, t;
    if (typeof str !== 'string' && str.length === 0) {
      throw new Error('Parameter is error.');
    }
    chars = str.split('');
    len = chars.length;
    t = 0;
    s = 0;
    result = this.dict[0].join('').indexOf(chars[0]);
    if (result < 0) {
      throw new Error('Invalid string.');
    }
    map = this.dict[result].join('');
    for (i = n = 1, ref = len; 1 <= ref ? n < ref : n > ref; i = 1 <= ref ? ++n : --n) {
      j = map.indexOf(chars[i]);
      if (j < 0) {
        throw new Error('Invalid string.');
      }
      if ((s = j - t) < 0) {
        s += this.radix;
      }
      result += s * Math.pow(this.radix, i);
      t = j;
    }
    result = this.keyCode - result;
    if (result > this.upper || result < this.lower) {
      throw new Error('Invalid string.');
    }
    return result;
  };

  return N3D;

})();

module.exports = N3D;
