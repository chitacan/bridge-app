module.exports = Client;

var net   = require('net')
  , u     = require('url')
  , debug = require('debug')
  , debug_a = debug('client:adb')
  , debug_c = debug('client:bridge');

function Client(opt) {
  if (!(this instanceof Client)) {
    return new Client(opt);
  }

  var port = opt.port;
  var url  = u.resolve(opt.url, 'bridge/client');

  this.server = createServer(port);
  this.socket = createSocket(url);

  return this;
}

function createServer(port) {
  var server = net.createServer(function(c) {
    server.socket = c;
    debug_a('connected');
    c.on('close', function() {
      debug_a('disconnected');
    });
    c.on('data', function(chunk) {
      if (!!socket && socket.connected) {
        socket.emit('data', { binary: chunk });
      }
    });
    c.on('error', function(e) {
      debug_a(e);
    });
  });
  server.maxConnection = 1;
  server.listen(port, function() {
    this.port = this.address().port;
    debug_a('Socket server listening on port ' + this.port);
  });

  return server;
}

function createSocket(url) {
  debug_c('connecting to : ' + url);
  var socket = require('socket.io-client')(url);

  socket.on('connect', function(){
    debug_c('connected');
  });
  socket.on('data', function(data){
    debug_c('client data');
  });
  socket.on('disconnect', function(){
    debug_c('disconnected');
  });
  socket.on('res', function(data) {
    if (data.binary) {
      server.socket.write(data.binary);
    }
  });
  socket.on('error', function(e) {
    debug_c(e);
  });

  return socket;
}
