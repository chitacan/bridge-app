'use strict'
exports = module.exports = Bridge;

var net    = require('net')
  , util   = require('util')
  , events = require('events')
  , crypto = require('crypto')
  , D      = require('debug')('bridge')

function Bridge() {
  events.EventEmitter.call(this);

  var opt = arguments[0] || {};

  this.id;
  this.cache;
  this.socket_d;
  this.socket_c;

  this.pipe = pipe;
  this.generateId = generateId;

  this.adbd = createServer.call(this, 'adbd', onConnectDaemon);
  this.adbc = createServer.call(this, 'adbc', onConnectClient);

  this.adbd.port = opt.dPort || 0;
  this.adbc.port = opt.cPort || 0;
}

util.inherits(Bridge, events.EventEmitter);

function createServer(name, cb) {
  var server = net.createServer(cb);
  server.name = name;
  server.bridge = this;
  server.maxConnection = 1;
  server.on('error', function(e) {
    this.bridge.emit('error', e);
  });
  return server;
}

function generateId() {
  var unique = '' + this.adbd.port + this.adbc.port;
  var sha = crypto.createHash('sha1');
  sha.update(unique);
  this.id = sha.digest('hex');
}

function pipe() {
  this.socket_d.pipe(this.socket_c);
  this.socket_c.pipe(this.socket_d);
}

function onConnectClient(c) {
  var bridge = this.bridge;

  c.setKeepAlive(true);
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

  c.setKeepAlive(true);
  c.on('end', onEnd);
  c.on('close', onClose);
  c.on('error', onError);

  bridge.socket_d = c;
  bridge.emit('connect', c);
  if (bridge.cache) {
    c.write(bridge.cache, function() {
      bridge.cache = null;
    });
    bridge.pipe();
  }
}

function onEnd() {
  this.server.bridge.emit('end', this);
  D(this.server.name + ' end');
}

function onClose() {
  var bridge = this.server.bridge;
  if (bridge.cache)
    bridge.cache = null;
  bridge.emit('close', this);
  D(this.server.name + ' close');
}

function onError(e) {
  this.server.bridge.emit('error', e);
  D(this.server.name + ' error ---');
}

Bridge.prototype.install = function() {
  var self = this;
  var times = 2;

  function onListen() {
    this.port = this.address().port;
    if (times-- < 2) {
      self.generateId();
      self.emit('install');
    }
  }

  this.adbd.listen(this.adbd.port, onListen);
  this.adbc.listen(this.adbc.port, onListen);
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
  return {
    adbd: { 
      isConnected: !!this.socket_d && this.socket_d.writable, 
      port: this.adbd.port
    },
    adbc : {
      isConnected: !!this.socket_c && this.socket_c.writable,
      port: this.adbc.port
    }
  }
}
