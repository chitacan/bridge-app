module.exports = Bridge;

var _       = require('lodash')
  , debug   = require('debug')
  , debug_c = debug('server:client')
  , debug_d = debug('server:deamon');

var DAEMON_NSP = '/bridge/deamon'
  , CLIENT_NSP = '/bridge/client';

function Bridge(io) {
  if (!(this instanceof Bridge)) {
    return new Bridge(io);
  }
  this.io = io;

  this.client = io.of(DAEMON_NSP);
  this.daemon = io.of(CLIENT_NSP);

  this.client.on('connection', onClientConnect);
  this.daemon.on('connection', onDaemonConnect);

  return this;
}

function onClientConnect(socket) {
  if(_.size(this.connected) > 1) {
    socket.disconnect();
    return;
  }

  var daemon = this.server.of(DAEMON_NSP);
  debug_c('connected : ' + socket.id);
  socket.on('data', function(data) {
    daemon.emit('data', data);
  });
  socket.on('disconnect', function() {
    debug_c('disconnected : ' + socket.id);
  });
}

function onDaemonConnect(socket) {
  if(_.size(this.connected) > 1) {
    socket.disconnect();
    return;
  }

  var client = this.server.of(CLIENT_NSP);
  debug_d('connected : ' + socket.id);
  socket.on('res', function(data) {
    if (data.binary) {
      client.emit('res', data);
    }
  });
  socket.on('disconnect', function() {
    debug_d('disconnected : ' + socket.id);
  });
}
