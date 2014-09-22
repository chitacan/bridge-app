'use strict'
var request = require('supertest')
  , should  = require('should')
  , app     = require('../app').app;

describe('Bridge API', function(){

  describe('GET /api/bridge', function(){
    it('should', function(done){
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
