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

  it('Send client cache', function(done) {
    client_socket.emit('bc-data', { binary: new Buffer('CNXN') });
    done();
  });

  it('Create birdge & ensure client cache', function(done) {
    rest.put('http://localhost:9500/api/bridge', {
      data: {
        client: client_socket.io.engine.id,
        daemon: daemon_socket.io.engine.id
      }
    })
    .on('2XX', function(res) {
      res.should.eql({ result: 'created' });
    });
    daemon_socket.on('bs-data', function(data) {
      daemon_socket.off('bs-data');
      (data.binary.toString()).should.equal('CNXN');
      done();
    });
  });

  it('Client >> Daemon (bc-data)', function(done) {
    var dummyData = { binary: [0,0] };
    daemon_socket.on('bs-data', function(data) {
      daemon_socket.off('bs-data');
      data.should.eql(dummyData);
      done();
    });
    client_socket.emit('bc-data', dummyData);
  });

  it('Daemon >> Client (bd-data)', function(done) {
    var dummyData = { binary: [1,1] };
    client_socket.on('bs-data', function(data) {
      client_socket.off('bs-data');
      data.should.eql(dummyData);
      done();
    });
    daemon_socket.emit('bd-data', dummyData);
  });

  it('remove birdge', function(done) {
    done();
  });

});
