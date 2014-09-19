var app = angular.module('bridge-manager', ['ngResource', 'ngRoute'])

app.config(function($routeProvider, $locationProvider) {
  $routeProvider
  .when('/', {
    templateUrl: 'partials/status_tmpl',
    controller : 'statusCtrl'
  })
  .when('/create', {
    templateUrl: 'partials/create_tmpl',
    controller : 'createCtrl'
  });
});

app.controller('statusCtrl', function($scope, Bridge, $location) {
  $scope.remove = function(client, daemon) {
    Bridge.remove({
      client: client,
      daemon: daemon
    }).$promise.then(function(bridges) {
      $scope.bridges = bridges;
    });
  }
  $scope.createBridge = function() {
    $location.path('/create');
  }
  Bridge.query().$promise.then(function(bridges) {
    $scope.bridges = bridges;
  });
});

app.controller('createCtrl', function($scope, Bridge, $location, $q) {
  $scope.selected = {};
  $q.all({
    daemon: Bridge.daemon().$promise,
    client: Bridge.client().$promise
  }).then(function(res) {
    $scope.client = res.client;
    $scope.daemon = res.daemon;
  })
  $scope.createBridge = function(selected) {
    Bridge.create(selected).$promise.then(function(res) {
      $location.path('/');
    });
  }
});

app.factory('Bridge', function($resource) {
  return $resource(
    '/api/bridge/:type',
    {},
    {
      'query'  : { method: 'GET', isArray: true },
      'get'    : { method: 'GET' },
      'create' : { method: 'PUT' },
      'remove' : { method: 'DELETE', isArray: true },
      'client' : { method: 'GET', isArray: true, params: {type: 'client'} },
      'daemon' : { method: 'GET', isArray: true, params: {type: 'daemon'} }
    }
  )
});
