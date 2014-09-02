var net   = require('net');
var debug = require('debug');
var debug_a = debug('client:adb');
var debug_c = debug('client:bridge');

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
server.listen(8080);

var socket = require('socket.io-client')('http://localhost:3000/bridge/client');

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
