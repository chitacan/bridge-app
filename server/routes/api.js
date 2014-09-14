var _ = require('lodash');
var express = require('express');
var r = express.Router();

r.route('/bridge')
  .get(function(req, res) {
    res.json({ result: 'hello' });
  })
  .put(function(req, res) {
    r.bridge.install({
      client : req.param('client'),
      daemon : req.param('daemon')
    });
    res.json({ result: 'created' });
  });

r.route('/bridge/client')
  .get(function(req, res) {
    var ids = getSocketInfo('/bridge/client')
    res.json(ids);
  });

r.route('/bridge/daemon')
  .get(function(req, res) {
    var ids = getSocketInfo('/bridge/daemon')
    res.json(ids);
  });

function getSocketInfo(namespace) {
  var nsps = r.bridge.io.of(namespace)
  var sockets = _.values(nsps.connected);
  return _(sockets)
  .reject('bridgeId')
  .map(function(val) {
    return {
      name  : val.hostInfo.toString(),
      value : val.id
    }
  })
  .valueOf();
}

module.exports = function(bridge) {
  r.bridge = bridge;
  return r;
};
