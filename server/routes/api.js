var express = require('express')
  , _       = require('underscore')
  , B       = require('../bridge')
  , D       = require('debug')('route:api');

var api = express.Router()
var bridges = {};

function getConnection() {
  return _.map(bridges, function(v, k) {
    return {
      id : k,
      conn : v.getConnection()
    }
  });
}

function removeBridge(id, closeServer) {
  var b = bridges[id];
  b.removal(closeServer);
  if (closeServer)
    delete bridges[id];
}

api.param('id', function(req, res, next, id) {
  req.id = id;
  next();
});

api.get('/', function(req, res) {
  res.send('api works!!');
});

api.route('/bridge')
  .get(function(req, res, next) {
    res.json(getConnection());
  })
  .put(function(req, res, next) {
    var b = new B({});
    b.install();
    b.on('connect', function(c) {
      D('connected on ' + c.localPort + ', from ' + c.remoteAddress + ':' + c.remotePort);
    });
    b.on('install', function() {
      var c = b.getConnection();
      D(b.id.slice(0,6)  + ', adbd:' + c.adbd.port + ', adbc:' + c.adbc.port);
      bridges[b.id] = b;
      res.json({
        id : b.id,
        conn : c
      });
    });
    b.on('error', function(e) {
      D(e);
      b.removal();
    });
  });

api.route('/bridge/:id')
  .get(function(req, res, next) {
    res.json(bridges[req.id].getConnection());
  })
  .delete(function(req, res, next) {
    var closeServer = req.param('closeServer') == 'true';
    removeBridge(req.id, closeServer);
    res.json(getConnection());
  });

module.exports = api;
