var B = require('./bridge');

var b = new B({});
b.install();
b.on('install', function() {
  console.log('installed : ' + b.id);
  console.log(b.getConnection());
});
b.on('connect', function(c) {
  console.log(c.server.name + ' connected');
  console.log(this.getConnection());
});
b.on('close', function(c) {
  console.log(c.server.name + ' close');
  console.log(this.getConnection());
});
b.on('error', function(e) {
  console.log(e);
});
