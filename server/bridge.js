module.exports = function(io) {
  var _ = require('lodash');
  var debug = require('debug');
  var debug_c = debug('server:client');
  var debug_d = debug('server:deamon');

  var client = io.of('/bridge/client');
  var daemon = io.of('/bridge/daemon');

  client.on('connection', function(socket) {
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
  });

  daemon.on('connection', function(socket) {
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
  });
}
