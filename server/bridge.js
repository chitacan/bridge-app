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

  this.cache = '';

  return this;
}

Bridge.prototype.get = function() {
  var self = this;
  return _.map(self.bridges, function(ids) {
    var skt = self.getSocketById(ids);
    var sktc = skt.client;
    var sktd = skt.daemon;

    var client = {
      id        : sktc.id,
      connected : sktc.clientConnected,
      host      : sktc.hostInfo.hostname
    }
    var daemon = {
      id   : sktd.id,
      host : sktd.hostInfo.model + ' ' + sktd.hostInfo.version
    }

    return {
      client : client,
      daemon : daemon,
      transmit : sktc.transmit + sktd.transmit
    }
  });
}

Bridge.prototype.install = function(ids) {
  this.bridges.push(ids);
  var skt = this.getSocketById(ids);
  setBridgeId(skt, ids);

  if (skt.daemon && this.cache) {
    skt.daemon.emit('bs-data', this.cache);
    this.cache = '';
  }
}

Bridge.prototype.remove = function(ids) {
  var removed = _.remove(this.bridges, function(val) {
    return (ids.client === val.client) || (ids.daemon === val.daemon);
  });
  var skt = this.getSocketById(removed[0]);
  skt.client && skt.client.emit('bs-collapse');
  setBridgeId(skt);
  this.cache = '';
}

// support array argument
Bridge.prototype.getSocketById = function(ids) {
  ids = _.defaults(ids || {}, {client: '', daemon: ''});
  return {
    client : this.client.connected[ids.client],
    daemon : this.daemon.connected[ids.daemon]
  }
}

Bridge.prototype.getSocketByNsp = function(namespace) {
  var nsp     = this.io.of(namespace)
  var sockets = _.values(nsp.connected);
  return _(sockets)
  .reject('bridgeId')
  .map(function(val) {
    var host = !!val.hostInfo ? val.hostInfo.toString() : 'unknown';
    return {
      name  : val.hostInfo.toString(),
      value : val.id
    }
  })
  .valueOf();
}

function setBridgeId(sockets, ids) {
  ids = _.defaults(ids || {}, {client: '', daemon: ''});
  sockets.client && (sockets.client.bridgeId = ids.daemon);
  sockets.daemon && (sockets.daemon.bridgeId = ids.client);
}

function onClientConnect(socket) {
  var bridge = this.bridge;
  var daemon = this.server.of(DAEMON_NSP);
  debug_c('connected : ' + socket.id);
  socket.clientConnected = false;
  socket.transmit = 0;
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
    this.transmit += data.binary.length;
    if (this.bridgeId)
      daemon.to(this.bridgeId).emit('bs-data', data);
    else {
      var cmd = data.binary.toString().slice(0, 4);
      cmd === 'CNXN' && (bridge.cache = data);
    }
  });
  socket.on('bc-client-connected', function() {
    socket.clientConnected = true;
  });
  socket.on('bc-collapse', function() {
    socket.clientConnected = false;
    bridge.remove({client: socket.id});
  });
  socket.on('disconnect', function() {
    debug_c('disconnected : ' + socket.id);
    bridge.remove({client: socket.id});
    this.transmit = 0;
  });
}

function onDaemonConnect(socket) {
  var bridge = this.bridge;
  var client = this.server.of(CLIENT_NSP);
  debug_d('connected : ' + socket.id);
  socket.transmit = 0;
  socket.on('bd-data', function(data) {
    this.transmit += data.binary.length;
    if (this.bridgeId)
      client.to(this.bridgeId).emit('bs-data', data);
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
    bridge.remove({daemon: socket.id});
  });
  socket.on('disconnect', function() {
    debug_d('disconnected : ' + socket.id);
    if (this.bridgeId)
      client.to(this.bridgeId).emit('bs-collapse');
    bridge.remove({daemon: socket.id});
    this.transmit = 0;
  });
}
