// Configuration Options
require.config({
  baseUrl: "js/", //for reading app.js and main.js under js/ folder
  // paths: maps ids with paths (no extension)
  paths: {
    "contentfulMgmt": ["../scripts/lib/contentful/dist/contentful-management.min"],
    "jquery": ["../scripts/lib/jquery/jquery.min"],
    "underscore": ["../scripts/lib/underscorejs/underscore-min"]
  },
  // shim: makes external libraries reachable
  shim: {
    contentfulMgmt: {
      exports: "contentfulMgmt"
    },
    jquery: {
      exports: "$"
    },
    underscore: {
      exports: "_"
    }
  }
});

require(["jquery", "underscore", "contentfulMgmt"], function ($, _, contentfulMgmt) {

  /*=============================================
  
  How to use this:
  
  1- Please change deliveryToken, mgmtToken and space id of the target space where you wanna add
  2- imagePlaceholdersIds, i have added hardcoded ids of the images from space 2.0, these ids will remain same as the data is copied from DEV to QA
     somehow if you are going to add recipes in the environment where these asset ids are different then please update them before running this utility
  
     Right now, all the tokens and space are of DEV, please update it before running
  
  ============================================*/

  var deliveryToken = "acd9b54346d75299a6c38495e86897a006064738b3bbb70d75949e0d353fbecc",// "403746bf68bfe9076cdc7a4921eb60fca2dbdb400d0500aebfca422465ecc6f6",
    mgmtToken = "d931928a371a2c2881ab2ee40ac32933dbbf3a4cb957949a559498353d27c703",
    spaceId = "uaa09ql9k0rz";// "webmcjs3o6bq";

  var url = "http://cdn.contentful.com/spaces/" + spaceId + "/entries?access_token=" + deliveryToken + "&content_type=16cqEGHkksoeC2musKO2w0&limit=1000";

  var delay = function (sleepTime) {
    var requestTime = new Date().getTime();
    while (new Date().getTime() < requestTime + sleepTime) { }
  };

  var imagePlaceholdersIds = {
    "thumbnailSmall": "ZiZPnRLlQqu0aw4SYaIMs",
    "thumbnailLarge": "7ztrZm0V8ce8iUACwyAAmQ",
  };

  var flushToDOM = function (clazz, text) {
    $("#divLogging").append($("<div/>").addClass("alert " + clazz).append($("<p/>").text(text)));
  };

  var buildAssetObject = function (fieldName) {
    return {
      "en-US": {
        "sys": {
          "type": "Link",
          "linkType": "Asset",
          "id": imagePlaceholdersIds[fieldName]
        }
      }
    }
  };

  $.get(url).then(function (response) {

    var entries = response.items,
      idList = entries.filter(function (entry) {
        return !entry.fields.hasOwnProperty("thumbnailSmall") && !entry.fields.hasOwnProperty("thumbnailLarge")
      }).map(function (entry) {
        return entry.sys.id;
      });

    var client = contentfulMgmt.createClient({
      accessToken: mgmtToken,
      rateLimit: 1,
      secure: true,
      retryOnTooManyRequests: true,
      maxRetries: 5
    });

    client.getSpace(spaceId).then(function (space) {

      var getEntries = function () {
        if (idList.length > 0) {
          var id = idList[0];
          space.getEntry(id).then(function (entry) {

            delay(500);

            if (entry) {

              entry.fields["thumbnailSmall"] = buildAssetObject("thumbnailSmall");
              entry.fields["thumbnailLarge"] = buildAssetObject("thumbnailLarge");

              space.updateEntry(entry).then(function (uEntry) {

                space.getEntry(uEntry.sys.id).then(function (gEntry) {

                  delay(500);

                  space.publishEntry(gEntry).then(function (pEntry) {

                    delay(500);

                    flushToDOM("alert-success", JSON.stringify(pEntry.sys));

                    idList.shift();
                    getEntries();

                  }).catch(function (err) {
                    idList.shift();
                    getEntries();
                    flushToDOM("alert-danger", JSON.stringify(err));
                  });
                });

              });
            }
          }).catch(function (err) {
            idList.shift();
            getEntries();
            if (err && err.hasOwnProperty("sys") && err.sys.id !== "NotFound") {
              flushToDOM("alert-danger", JSON.stringify(err));
            }

          });
        } else {
          console.log("END");
          flushToDOM("alert-info", "Finished adding images, phew!");
        }
      };

      getEntries();
    });
  });
});
