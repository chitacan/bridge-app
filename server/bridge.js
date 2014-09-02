module.exports = function(io) {
  var _ = require('lodash');
  var client = io.of('/bridge/client');
  var daemon = io.of('/bridge/daemon');

  client.on('connection', function(socket) {
    if(_.size(this.connected) > 1) {
      socket.disconnect();
      return;
    }

    console.log('client connected : ' + socket.id);
    socket.on('data', function(data) {
      daemon.emit('data', data);
    });
    socket.on('disconnect', function() {
      console.log('client disconnected : ' + socket.id);
    });
  });

  daemon.on('connection', function(socket) {
    if(_.size(this.connected) > 1) {
      socket.disconnect();
      return;
    }

    console.log('daemon connected : ' + socket.id);
    socket.on('res', function(data) {
      if (data.binary) {
        client.emit('res', data);
      }
    });
    socket.on('disconnect', function() {
      console.log('daemon disconnected : ' + socket.id);
    });
  });
}
