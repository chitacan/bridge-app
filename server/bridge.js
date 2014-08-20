'use strict'
exports = module.exports = Bridge;

var net    = require('net')
  , util   = require('util')
  , events = require('events')

function Bridge(options) {
  events.EventEmitter.call(this);

  this.port_d = options.port       || 0;
  this.port_c = options.clientPort || 0;

  this.cache;
  this.socket_d;
  this.socket_c;

  this.pipe = pipe;

  this.adbd = createServer.call(this, 'adbd', onConnectDaemon);
  this.adbc = createServer.call(this, 'adbc', onConnectClient);
}

util.inherits(Bridge, events.EventEmitter);

function createServer(name, cb) {
  var server = net.createServer(cb);
  server.name = name;
  server.bridge = this;
  server.maxConnection = 1;
  return server;
}

function pipe() {
  this.socket_d.pipe(this.socket_c);
  this.socket_c.pipe(this.socket_d);
}

function onConnectClient(c) {
  var bridge = this.bridge;

  c.setKeepAlive(true, 10*1000);
  c.on('end', onEnd);
  c.on('close', onClose);
  c.on('error', onError);

  bridge.socket_c = c;
  bridge.emit('connect', c);
  if (bridge.socket_d && bridge.socket_d.writable) {
    bridge.pipe();
  } else {
    // adbc connected without adbd connection.
    // We need to cache first request.
    c.once('data', function(chunk) {
      bridge.cache = new Buffer(chunk.length);
      chunk.copy(bridge.cache);
    });
  }
}

function onConnectDaemon(c) {
  var bridge = this.bridge;

  c.setKeepAlive(true, 10*1000);
  c.on('end', onEnd);
  c.on('close', onClose);
  c.on('error', onError);

  bridge.socket_d = c;
  bridge.emit('connect', c);
  if (bridge.cache) {
    c.write(bridge.cache);
    bridge.pipe();
  }
}

// socket callbacks
function onEnd() {
  this.server.bridge.emit('end', this);
}

function onClose() {
  var bridge = this.server.bridge;
  if (bridge.cache)
    bridge.cache = null;
  bridge.emit('close', this);
}

function onError(e) {
  this.server.bridge.emit('error', e);
}

Bridge.prototype.install = function() {
  var self  = this;
  var addrs = [];

  function onListen() {
    var addr = this.address();
    addr.name = this.name
    addrs.push(addr);

    if (addrs.length > 1) {
      self.emit('install', addrs);
    }
  }

  this.adbd.listen(this.Port_d, onListen);
  this.adbc.listen(this.port_c, onListen);
}

Bridge.prototype.removal = function() {
  if (this.socket_d) {
    this.socket_d.destroy();
  }
  if (this.socket_c) {
    this.socket_c.destroy();
  }

  this.adbd.close();
  this.adbc.close();
}

Bridge.prototype.getConnection = function() {
  return [
    { type: 'adbd', isConnected: !!this.socket_d && this.socket_d.writable },
    { type: 'adbc', isConnected: !!this.socket_c && this.socket_c.writable }
  ]
}
