'use strict'
delete require.cache[require.resolve('../routes/api')]

var request = require('supertest')
  , should  = require('should')

describe('Bridge API', function(){

  function MockBridge() {
    if (!(this instanceof MockBridge))
      return new MockBridge();

    this.bridge = [];
  }
  MockBridge.prototype.get = function() {
    return this.bridge;
  }
  MockBridge.prototype.install = function(ids) {
    this.bridge.push(ids)
  }
  MockBridge.prototype.remove = function(ids) {
    this.bridge.pop();
  }
  MockBridge.prototype.getSocketByNsp = function(nsp) {
    var sockets = {
      '/bridge/client': [{ name  : 'client-host', value : 'client-id' }],
      '/bridge/daemon': [{ name  : 'daemon-host', value : 'daemon-id' }]
    }
    return sockets[nsp];
  }

  var app = require('../app')({bridge: MockBridge}).app;

  describe('/api/bridge', function() {

    it('PUT should response "created"', function(done){
      request(app)
      .put('/api/bridge')
      .send({
        client: 'client-id',
        daemon: 'daemon-id'
      })
      .set('Accept', 'application/json')
      .expect('Content-Type', /json/)
      .expect(200, function(err, res) {
        should.not.exist(err);
        (res.body).should.eql({ result: 'created' });
        done();
      });
    });

    it('GET should response bridge info', function(done){
      request(app)
      .get('/api/bridge')
      .set('Accept', 'application/json')
      .expect('Content-Type', /json/)
      .expect(200, function(err, res) {
        should.not.exist(err);
        (res.body).should.eql( [{ client: 'client-id', daemon: 'daemon-id' }] );
        done();
      });
    });

    it('DELETE should response ', function(done){
      request(app)
      .delete('/api/bridge')
      .set('Accept', 'application/json')
      .expect('Content-Type', /json/)
      .expect(200, function(err, res) {
        should.not.exist(err);
        (res.body).should.eql( [] );
        done();
      });
    });

  });

  describe('/api/bridge/client', function(){

    it('GET should response connected client', function(done){
      request(app)
      .get('/api/bridge/client')
      .set('Accept', 'application/json')
      .expect('Content-Type', /json/)
      .expect(200, function(err, res) {
        should.not.exist(err);
        (res.body).should.eql( [{ name: 'client-host', value: 'client-id' }] );
        done();
      });
    });

  });

  describe('/api/bridge/daemon', function(){

    it('GET should response connected daemon', function(done){
      request(app)
      .get('/api/bridge/daemon')
      .set('Accept', 'application/json')
      .expect('Content-Type', /json/)
      .expect(200, function(err, res) {
        should.not.exist(err);
        (res.body).should.eql( [{ name: 'daemon-host', value: 'daemon-id' }] );
        done();
      });
    });

  });

  describe('Page routes', function(){

    it('should response index page', function(done){
      request(app)
      .get('/')
      .expect('Content-Type', /text\/html/)
      .expect(200, function(err, res) {
        should.not.exist(err);
        done();
      });
    });

    it('should response status partial page', function(done){
      request(app)
      .get('/partials/status_tmpl')
      .expect('Content-Type', /text\/html/)
      .expect(200, function(err, res) {
        should.not.exist(err);
        done();
      });
    });

    it('should response create partial page', function(done){
      request(app)
      .get('/partials/create_tmpl')
      .expect('Content-Type', /text\/html/)
      .expect(200, function(err, res) {
        should.not.exist(err);
        done();
      });
    });

  });
});
