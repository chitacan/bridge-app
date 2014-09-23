var _ = require('lodash');
var express = require('express');
var r = express.Router();

r.route('/bridge')
  .get(function(req, res) {
    res.json(r.bridge.get());
  })
  .put(function(req, res) {
    r.bridge.install({
      client : req.param('client'),
      daemon : req.param('daemon')
    });
    res.json({ result: 'created' });
  })
  .delete(function(req, res) {
    r.bridge.remove({
      client : req.param('client'),
      daemon : req.param('daemon')
    });
    res.json(r.bridge.get());
  });

r.route('/bridge/client')
  .get(function(req, res) {
    var ids = r.bridge.getSocketByNsp('/bridge/client')
    res.json(ids);
  });

r.route('/bridge/daemon')
  .get(function(req, res) {
    var ids = r.bridge.getSocketByNsp('/bridge/daemon')
    res.json(ids);
  });

module.exports = function(bridge) {
  r.bridge = bridge;
  return r;
};
