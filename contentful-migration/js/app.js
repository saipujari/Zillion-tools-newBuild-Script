define(['angular'], function (angular) {
  var app = angular.module('SyncUtilityApp', []);

  app.init = function () {
    angular.bootstrap(document, ['SyncUtilityApp']);
  };
  
  return app;
});