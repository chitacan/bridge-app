var app = angular.module('bridge-manager', ['ngResource'])

app.controller('statusCtrl', ['$scope', 'bridge', function($scope, bridge) {
  bridge.query().$promise.then(function(bridges) {
    $scope.bridges = bridges;
  });
}]);

app.factory('bridge', function($resource) {
  return $resource(
    '/api/bridge',
    {},
    {
      'query'  : { method: 'GET', isArray: true },
      'get'    : { method: 'GET' },
      'create' : { method: 'PUT' },
      'remove' : { method: 'DELETE' }
    }
  )
});
