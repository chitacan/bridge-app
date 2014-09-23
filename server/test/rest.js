'use strict'
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
    this.bridges.push(ids)
  }
  MockBridge.prototype.remove = function(ids) {
    return this.bridges.pop();
  }
  MockBridge.prototype.getSocketByNsp = function() {
    return [{
      name  : '',
      value : ''
    }]
  }

  var app = require('../app')({bridge: MockBridge}).app;

  describe('/api/bridge', function(){

    it('GET should response bridges', function(done){
      request(app)
      .get('/api/bridge')
      .set('Accept', 'application/json')
      .expect('Content-Type', /json/)
      .expect(200, function(err, res) {
        should.not.exist(err);
        done();
      });
    });

  });
});
