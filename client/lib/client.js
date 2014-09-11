module.exports = Client;

var net   = require('net')
  , u     = require('url')
  , os    = require('os')
  , debug = require('debug')
  , debug_a = debug('client:adb')
  , debug_c = debug('client:bridge');

function Client(opt) {
  if (!(this instanceof Client)) {
    return new Client(opt);
  }

  var port = opt.port;
  var url  = u.resolve(opt.url, 'bridge/client');

  this.server = this.createServer(port);
  this.socket = this.createSocket(url);

  return this;
}

Client.prototype.createServer = function(port) {
  var self = this;
  var server = net.createServer(function(c) {
    server.socket = c;
    debug_a('connected');
    c.on('close', function() {
      debug_a('disconnected');
    });
    c.on('data', function(chunk) {
      if (!!self.socket && self.socket.connected) {
        self.socket.emit('data', { binary: chunk });
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

Client.prototype.createSocket = function(url) {
  var self = this;
  debug_c('connecting to : ' + url);
  var socket = require('socket.io-client')(url);

  socket.on('connect', function(){
    debug_c('connected');
    socket.emit('bc-host', {
      type     : os.type(),
      arch     : os.arch(),
      hostname : os.hostname(),
      version  : os.release()
    });
  });
  socket.on('data', function(data){
    debug_c('client data');
  });
  socket.on('disconnect', function(){
    debug_c('disconnected');
  });
  socket.on('res', function(data) {
    if (data.binary) {
      self.server.socket.write(data.binary);
    }
  });
  socket.on('error', function(e) {
    debug_c(e);
  });

  return socket;
}
