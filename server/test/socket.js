'use strict'
delete require.cache[require.resolve('../routes/api')]
delete require.cache[require.resolve('../routes/index')]

var should  = require('should')
  , www     = require('../app')({})
  , socket  = require('socket.io-client')

var app    = www.app
  , server = www.server;

var client_socket
  , daemon_socket;

describe('Socket.io API', function(){

  before(function(done) {
    server.listen(9500, done);
  });

  it('Client connect', function(done){
    client_socket = socket('http://localhost:9500/bridge/client');
    client_socket.on('connect', done);
  });

  it('Daemon connect', function(done){
    daemon_socket = socket('http://localhost:9500/bridge/daemon');
    daemon_socket.on('connect', done);
  });
});
