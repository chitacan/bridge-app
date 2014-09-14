module.exports = Bridge;

var _       = require('lodash')
  , debug   = require('debug')
  , debug_c = debug('server:client')
  , debug_d = debug('server:daemon');

var DAEMON_NSP = '/bridge/daemon'
  , CLIENT_NSP = '/bridge/client';

function Bridge(io) {
  if (!(this instanceof Bridge)) {
    return new Bridge(io);
  }
  this.io = io;
  this.bridges = [];

  this.client = io.of(CLIENT_NSP);
  this.daemon = io.of(DAEMON_NSP);

  this.client.on('connection', onClientConnect);
  this.daemon.on('connection', onDaemonConnect);

  return this;
}

Bridge.prototype.install = function(ids) {
  this.bridges.push(ids);
  var clientSocket = this.client.connected[ids.client];
  var daemonSocket = this.daemon.connected[ids.daemon];

  if (clientSocket)
    clientSocket.bridgeId = ids.daemon;
  if (daemonSocket)
    daemonSocket.bridgeId = ids.client;
}

function onClientConnect(socket) {
  var daemon = this.server.of(DAEMON_NSP);
  debug_c('connected : ' + socket.id);
  socket.on('bc-host', function(data) {
    data.toString = function() {
      return this.type
      .concat(' ').concat(this.arch)
      .concat(' ').concat(this.hostname)
      .concat(' ').concat(this.version)
    }
    this.hostInfo = data;
  });
  socket.on('bc-data', function(data) {
    daemon.to(this.bridgeId).emit('bs-data', data);
  });
  socket.on('disconnect', function() {
    debug_c('disconnected : ' + socket.id);
  });
}

function onDaemonConnect(socket) {
  var client = this.server.of(CLIENT_NSP);
  debug_d('connected : ' + socket.id);
  socket.on('bd-data', function(data) {
    if (data.binary) {
      client.to(this.bridgeId).emit('bs-data', data);
    }
  });
  socket.on('bd-host', function(data) {
    data.toString = function() {
      return this.manufacturer
      .concat(' ').concat(this.model)
      .concat(' ').concat(this.brand)
      .concat(' ').concat(this.version)
    }
    this.hostInfo = data;
  });
  socket.on('disconnect', function() {
    debug_d('disconnected : ' + socket.id);
  });
}
