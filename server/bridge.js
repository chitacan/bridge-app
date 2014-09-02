module.exports = Bridge;

var _       = require('lodash')
  , debug   = require('debug')
  , debug_c = debug('server:client')
  , debug_d = debug('server:deamon');

function Bridge(io) {
  if (!(this instanceof Bridge)) {
    return new Bridge(io);
  }

  this.client = io.of('/bridge/client');
  this.daemon = io.of('/bridge/daemon');

  this.client.on('connection', onClientConnect);
  this.daemon.on('connection', onDaemonConnect);
}

function onClientConnect(socket) {
  if(_.size(this.connected) > 1) {
    socket.disconnect();
    return;
  }

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
