// Configuration Options
require.config({
  baseUrl: "js/", //for reading app.js and main.js under js/ folder
  // paths: maps ids with paths (no extension)
  paths: {
    "angular": ["../scripts/lib/angularjs/angular.min"],
    "contentfulMgmt": ["../scripts/lib/contentful/dist/contentful-management.min"],
    "jquery": ["../scripts/lib/jquery/jquery.min"],
    "bootstrap": ["../scripts/lib/bootstrap/js/bootstrap"],
    "diffview": ["../scripts/lib/jsdiff/diffview"],
    "difflib": ["../scripts/lib/jsdiff/difflib"],
    "underscore": ["../scripts/lib/underscorejs/underscore-min"]
  },
  // shim: makes external libraries reachable
  shim: {
    angular: {
      exports: "angular"
    },
    contentfulMgmt: {
      exports: "contentfulMgmt"
    },
    jquery: {
      exports: "$"
    },
    bootstrap: {
      exports: "bootstrap",
      deps: ["jquery"] 
    },
    diffview: {
      exports: "diffview"
    },
    difflib: {
      exports: "difflib"
    },
    underscore: {
      exports: "_"
    }
  }
});

// Angular Bootstrap 
require(["app", "../controllers/main", "jquery", "underscore", "bootstrap", "diffview", "difflib"], function (app, controllers) {
  // initialisation code defined within app.js
  app.init();
});