var net = require('net');
var server = net.createServer(function(c) {
  server.socket = c;
  c.on('close', function() {
    console.log('close')
  });
  c.on('data', function(chunk) {
    if (!!socket && socket.connected) {
      socket.emit('data', { binary: chunk });
    }
  });
  c.on('error', function(e) {
    console.log(e);
  });
});
server.maxConnection = 1;
server.listen(8080);

var socket = require('socket.io-client')('http://localhost:3000/bridge/client');

socket.on('connect', function(){
  console.log('client connected');
});
socket.on('data', function(data){
  console.log('client data');
});
socket.on('disconnect', function(){
  console.log('client disconnected');
});
socket.on('res', function(data) {
  console.log('response length : ' + data.binary.length);
  if (data.binary) {
    server.socket.write(data.binary, function() {
      console.log('response to adb client');
    });
  }
});
socket.on('error', function(e) {
  console.log(e);
});
