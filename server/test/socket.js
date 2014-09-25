'use strict'
delete require.cache[require.resolve('../routes/api')]
delete require.cache[require.resolve('../routes/index')]

var should  = require('should')
  , rest    = require('restler')
  , www     = require('../app')({})
  , socket  = require('socket.io-client')

var app    = www.app
  , server = www.server;

var client_socket
  , daemon_socket;

describe('Socket.io API', function() {

  before(function(done) {
    server.listen(9500, done);
  });

  it('Client connect', function(done) {
    client_socket = socket('http://localhost:9500/bridge/client');
    client_socket.on('connect', done);
  });

  it('Client host info', function() {
    client_socket.emit('bc-host', {
      type : 'type',
      arch : 'arch',
      hostname : 'client-test',
      version : 'ver'
    });
  });

  it('Daemon connect', function(done){
    daemon_socket = socket('http://localhost:9500/bridge/daemon');
    daemon_socket.on('connect', done);
  });

  it('Daemon host info', function() {
    daemon_socket.emit('bd-host', {
      manufacturer: 'manu',
      model : 'model',
      brand : 'brand'
    });
  });

  it('Create birdge', function(done) {
    rest.get('http://localhost:9500/api/bridge/client')
    .on('2XX', function(ids) {
      (ids).should.eql( [{
        name  : 'type arch client-test ver',
        value : client_socket.io.engine.id
      }] );
      done();
    });
  });

  it('Client >> Daemon', function(done) {
    done();
  });

  it('Daemon >> Client', function(done) {
    done();
  });

  it('Collpase birdge', function(done) {
    done();
  });

  it('Client Cache', function(done) {
    done();
  });
});
