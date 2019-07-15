var sequelize = require('./src/db')[0];

var argv = process.argv || [],
  command = argv[2] || '',
  isForce = command.indexOf('force') !== -1;

if (isForce) {
  console.log('Drop all schemas.');
  sequelize.drop();
}

console.log('Sync all schemas.');

sequelize.sync({
  force: isForce
}).then(function() {
  return console.log('All done!');
})["catch"](function(err) {
  return console.log(err);
});
