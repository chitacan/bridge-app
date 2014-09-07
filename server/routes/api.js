var _ = require('lodash');
var express = require('express');
var r = express.Router();

r.route('/bridge')
  .get(function(req, res) {
    res.json({ result: 'hello' });
  });

r.route('/bridge/client')
  .get(function(req, res) {
    var ids = getSocketIds('/bridge/client')
    res.json(ids);
  });

r.route('/bridge/daemon')
  .get(function(req, res) {
    var ids = getSocketIds('/bridge/daemon')
    res.json(ids);
  });

function getSocketIds(namespace) {
  var nsps = r.io.of(namespace)
  return _.keys(nsps.connected);
}

module.exports = function(io) {
  r.io = io;
  return r;
};
