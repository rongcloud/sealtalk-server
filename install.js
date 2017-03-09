var exec = require('child_process').exec;
var os = require('os');

var showResult = function(err, stdout) {
    console.log(err || stdout);
};

var execShell = function(shell, callback) {
    exec(shell, function(err, stdout, stderr) {
        callback(err, stdout);
    });
};

var addSudo = function() {
    return os.platform() == 'linux' ? 'sudo ' : '';
};

var getMethod = function() {
    return os.platform() == 'linux' ? 'export' : 'set';
};

var initGrunt = function() {
    var shell = addSudo() + 'npm install -g grunt-cli';
    execShell(shell, function(err, stdout) {
        showResult(err, stdout);
        next();
    });
};

var initDeps = function() {
    var shell = addSudo() + 'npm install';
    execShell(shell, function(err, stdout) {
        showResult(err, stdout);
        next();
    });
};

var initDB = function() {
    var shell = 'node sync.js --harmony';
    execShell(shell, function(err, stdout) {
        showResult(err, stdout);
        var logs = ['',
            '初始化已完成，请在下方命令行执行: ', 
            '-----------------------------------------------',
            '1、设置环境变量:                               |',
            '                                               |', 
            '   Windows  : set NODE_ENV=development         |', 
            '                                               |', 
            '   Mac/Linux: export NODE_ENV=development      |',  
            '-----------------------------------------------',
            '2、启动服务:                                   |', 
            '                                               |', 
            '   grunt nodemon                               |',
            '-----------------------------------------------'];
        !err && showResult(logs.join('\n'));
    });
};

var methods = [initGrunt, initDeps, initDB],
    index = 0;
var next = function() {
    showResult('正在初始化，请耐心等待...');
    methods[index]();
    index++;
};

next();
// initDB();