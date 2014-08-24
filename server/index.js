var B = require('./bridge')
  , D = require('debug')('index')

var b = new B();
b.install();
b.on('install', function() {
  D('installed : ' + b.id);
  D(b.getConnection());
});
b.on('connect', function(c) {
  D(c.server.name + ' connected');
  D(this.getConnection());
});
b.on('close', function(c) {
  D(c.server.name + ' close');
  D(this.getConnection());
});
b.on('error', function(e) {
  D(e);
});
