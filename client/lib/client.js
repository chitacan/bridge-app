module.exports = Client;

var net   = require('net')
  , u     = require('url')
  , os    = require('os')
  , rest  = require('restler')
  , debug = require('debug')
  , debug_a = debug('client:adb')
  , debug_c = debug('client:bridge');

function Client(opt) {
  if (!(this instanceof Client)) {
    return new Client(opt);
  }

  this.opt = opt;
  this.server = this.createServer();
  this.socket = this.createSocket();

  return this;
}

Client.prototype.createServer = function() {
  var self = this;
  var server = net.createServer(function(c) {
    server.socket = c;
    debug_a('connected');
    c.on('close', function() {
      debug_a('disconnected');
      if (!!self.socket && self.socket.connected)
        self.socket.emit('bc-collapse');
    });
    c.on('data', function(chunk) {
      if (!!self.socket && self.socket.connected) {
        self.socket.emit('bc-data', { binary: chunk });
      }
    });
    c.on('error', function(e) {
      debug_a(e);
    });
  });
  server.maxConnection = 1;
  server.listen(this.opt.port, function() {
    this.port = this.address().port;
    debug_a('Socket server listening on port ' + this.port);
  });

  return server;
}

Client.prototype.createSocket = function() {
  var self     = this;
  var url      = u.resolve(this.opt.url, 'bridge/client');
  var daemonId = this.opt.daemonId;
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

    if (daemonId) {
      installBridge(self.opt.url, {
        client: socket.io.engine.id,
        daemon: daemonId
      });
    }
  });
  socket.on('data', function(data){
    debug_c('client data');
  });
  socket.on('disconnect', function(){
    self.opt.daemonId = '';
    debug_c('disconnected');
  });
  socket.on('bs-data', function(data) {
    if (data.binary) {
      self.server.socket.write(data.binary);
    }
  });
  socket.on('error', function(e) {
    debug_c(e);
  });

  return socket;
}

function installBridge(url, ids) {
  url = u.resolve(url, 'api/bridge');
  rest.put(url, { data: ids })
  .on('complete', function(data) {
  });
}
