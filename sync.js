var sequelize;

sequelize = require('./src/db')[0];

console.log('Drop all schemas.');

sequelize.drop();

console.log('Sync all schemas.');

sequelize.sync({
  force: true
}).then(function() {
  return console.log('All done!');
})["catch"](function(err) {
  return console.log(err);
});
