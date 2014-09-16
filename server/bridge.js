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

  this.client.bridge = this;
  this.daemon.bridge = this;

  this.client.on('connection', onClientConnect);
  this.daemon.on('connection', onDaemonConnect);

  return this;
}

Bridge.prototype.get = function() {
  return this.bridges;
}

Bridge.prototype.install = function(ids) {
  this.bridges.push(ids);
  var skt = this.getSockets(ids);
  setBridgeId(skt, ids);
}

Bridge.prototype.remove = function(ids) {
  var removed = _.remove(this.bridges, function(val) {
    return (ids.client === val.client) || (ids.daemon === val.daemon);
  });
  var skt = this.getSockets(removed[0]);
  setBridgeId(skt);
}

// support array argument
Bridge.prototype.getSockets = function(ids) {
  ids = _.defaults(ids || {}, {client: '', daemon: ''});
  return {
    client : this.client.connected[ids.client],
    daemon : this.daemon.connected[ids.daemon]
  }
}

function setBridgeId(sockets, ids) {
  ids = _.defaults(ids || {}, {client: '', daemon: ''});
  !sockets.client || (sockets.client.bridgeId = ids.daemon);
  !sockets.daemon || (sockets.daemon.bridgeId = ids.client);
}

function onClientConnect(socket) {
  var self   = this.bridge;
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
  socket.on('bc-collapse', function() {
    self.remove({client: socket.id});
  });
  socket.on('disconnect', function() {
    debug_c('disconnected : ' + socket.id);
    self.remove({client: socket.id});
  });
}

function onDaemonConnect(socket) {
  var self   = this.bridge;
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
  socket.on('bd-collapse', function() {
    self.remove({daemon: socket.id});
  });
  socket.on('disconnect', function() {
    debug_d('disconnected : ' + socket.id);
    self.remove({daemon: socket.id});
  });
}
