var app = angular.module('bridge', ['ngResource'])

app.controller('statusController', ['$scope', 'Bridges', function($scope, Bridges) {
  $scope.create = function() {
    Bridges.create().$promise.then(function(bridge) {
      $scope.bridges.push(bridge);
    });
  }
  
  $scope.delete = function(id) {
    Bridges.remove({id: id, closeServer: true}).$promise.then(function(bridges) {
      $scope.bridges = bridges;
    })
  }

  $scope.update = function(id, index) {
    Bridges.get({id: id}).$promise.then(function(bridge) {
      var conn = $scope.bridges[index].conn;
      conn.adbd = bridge.adbd;
      conn.adbc = bridge.adbc;
    });
  }

  Bridges.query().$promise.then(function(bridges) {
    $scope.bridges = bridges;
  });
}]);

app.factory('Bridges', function($resource) {
  return $resource(
    '/api/bridge/:id',
    {id:'@id'},
    {
      'query' : { method: 'GET', isArray: true },
      'get'   : { method: 'GET'},
      'create': { method: 'PUT'},
      'remove': { method: 'DELETE', isArray: true }
    }
  )
});
