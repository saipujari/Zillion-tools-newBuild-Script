//oz:
define(['app', 'contentfulMgmt'], function (app, contentfulMgmt) {

    app.controller("UtilityController", ["$scope", "$timeout", function ($scope, $timeout) {

        //for logging creating/updating/publishing an entry/asset from one space to other
        $scope.logging = [];
        $scope.retryCount = 0;

        //will hold the history of entries that have created, so that wont create twice
        $scope.entriesCreatedHistory = [];
        $scope.entriesNotCreated = [];

        $scope.syncOptions = {
            option: "SYNC_ENTRIES"
        };

        $scope.confirmModal = {
            option: "",
            message: ""
        };

        $scope.alertModal = {
            message: "",
            model: {
                modelTitle: "",
                modelContentType: "",
                modelEntry: "",
                type: ""
            }
        };

        $scope.ctrlConstants = {
            singleRequestMinTime: 1000 / 3,
            parentsHolder: {},
            childrenHolder: {},
            blackout: false,
            blackoutText: "Creating please wait...",
            blackoutPleaseWait: false,
            isSyncFromDiffDialog: false,
            stopModalOpenTwice: false,
            ignoreKeys: ["UnresolvedLinks", "RateLimitExceeded", "NotFound"]
        };

        $scope.progressPercentage = 0;
        $scope.syncEntriesList = [];

        $scope.spaces = {
            sourceSpace: {
                cf_client: "",
                cf_space: "",
                entries: "",
                loading: false,
                loadingText: "Please wait...",
                accessCode: "",
                spaceId: "",
                contentTypesEntriesMap: {}, //it will hold all entries by their contentTypeId as mapKey
                type: "SOURCE_SPACE"
            },
            targetSpace: {
                cf_client: "",
                cf_space: "",
                loading: false,
                loadingText: "Please wait...",
                accessCode: "",
                spaceId: "",
                contentTypesEntriesMap: {}, //it will hold all entries by their contentTypeId as mapKey
                type: "TARGET_SPACE"
            }
        };

        //when 'Load Space" is clicked from the UI
        $scope.loadCFClient = function (sourceSpace) {
            if (contentfulMgmt) {
                sourceSpace.loading = true;
                sourceSpace.cf_client = contentfulMgmt.createClient({
                    accessToken: sourceSpace.accessCode,
                    rateLimit: 1,
                    secure: true,
                    retryOnTooManyRequests: true,
                    maxRetries: 5
                });
                sourceSpace.cf_client.getSpace(sourceSpace.spaceId).catch(function (error) {
                    console.log("catch-loadCFClient", error);
                    sourceSpace.loading = false;
                    var message = "Error in loading space";
                    if (error && error.message) {
                        message = error.message;
                        $scope.showAlertModal("ERROR loading space: " + message);
                    } else {
                        $scope.showAlertModal(message);
                    }
                    return error;
                }).then(function (space) {
                    sourceSpace.cf_space = space;
                    //loading all content_types first and then getting entries by content_type
                    if ($scope.syncOptions.option === "SPACE_DIFF") {
                        if (sourceSpace.type === "TARGET_SPACE") {
                            sourceSpace.entries = $scope.spaces.sourceSpace.entries;
                        }
                        $scope.loadContentTypesAndEntriesBySpace(sourceSpace);
                    } else {
                        sourceSpace.loading = false;
                    }
                    $scope.$apply();
                });
            } else {
                $scope.showAlertModal("Unable to load client, please reload the page.", true);
            }
        };

        //loads all content_types and their respective entries by contentTypeId
        $scope.loadContentTypesAndEntriesBySpace = function (sourceSpace) {

            var progressCount = 0;

            sourceSpace.loadingText = "Fetching space content... (0%)";

            if (sourceSpace.cf_space && sourceSpace.cf_space.name) {

                sourceSpace.cf_space.getContentTypes().catch(function (cterror) {

                    $scope.showAlertModal("Error in getting space content, please see browser console for details.");
                    console.log("catch-loadContentTypesAndEntriesBySpace-contentTypes", cterror);

                }).then(function (contentTypes) {

                    var total = contentTypes.length;

                    //for loading entries by their content_types
                    var getEntryByContentType = function (contentTypesList) {

                        if (contentTypesList && contentTypesList.length > 0) {
                            var startTime = new Date(),
                                perc = $scope.calculatePercentage(progressCount++, total);

                            sourceSpace.loadingText = "Fetching space content... (" + perc + "%)";

                            //gets the array first element
                            var contentType = contentTypesList[0],
                                contentTypeId = contentType.sys.id;

                            //query contentful to get entries of this contentType                     
                            //riz: added limit  
                            sourceSpace.cf_space.getEntries({ content_type: contentTypeId, limit: 1000 }).catch(function (error) {

                                console.log("catch-loadContentTypesAndEntriesBySpace-getEntries", "Error in getting entries, retrying..." + error);
                                progressCount = 0;
                                sourceSpace.loadingText = "Error in getting entries, retrying...";

                                //if we catch error while getting entries then again call that method to start getting entries again                     
                                getEntryByContentType(contentTypesList);

                            }).then(function (entries) {
                                $scope.checkContentFulRequestDelay(startTime);

                                //riz: added following check to display modal if it exceeds the limit
                                if (entries && entries.total > 0 && entries.total > entries.limit) {
                                    $scope.showAlertModal("Entries limit is insufficient");
                                    return;
                                }

                                //some of the entries returned from response have fields empty so ignore them before adding in map                                
                                var removeEmptyEntries = (entries && entries.length > 0) ? entries.filter(function (ent) {
                                    return !$.isEmptyObject(ent.fields);
                                }) : [];

                                var chosenEntries = removeEmptyEntries;
                                if (sourceSpace.entries) {
                                    chosenEntries = [];
                                    var splittedEntries = sourceSpace.entries.split(", ");
                                    removeEmptyEntries.forEach(function (element) {
                                        if (splittedEntries.indexOf(element.sys.id) > -1) {
                                            chosenEntries.push(element);
                                        }
                                    });
                                }

                                //push contentType with all its entries in a array for later use while calculating difference
                                if (chosenEntries.length > 0) {
                                    sourceSpace.contentTypesEntriesMap[contentTypeId] = {
                                        "entries": chosenEntries,
                                        "contentType": contentType
                                    };
                                }

                                //remove the first element from array                              
                                contentTypesList.shift();

                                //recursive call again for next array index                    
                                getEntryByContentType(contentTypesList);
                            });
                        } else {
                            sourceSpace.loading = false;
                            sourceSpace.loadingText = "Please wait...";
                            progressCount = 0;
                        }

                        $scope.$apply();
                    };

                    getEntryByContentType(contentTypes);
                });
            } else {
                $scope.showAlertModal("Error in getting space content, please see browser console for details.");
            }
        };

        //riz: following method to grab only entries with following keywords
        $scope.isEntryWithRequiredKeyword = function (fields) {

            var keys = Object.keys(fields);
            if (keys) {
                for (var ind = 0; ind < keys.length; ind++) {
                    if (fields[keys[ind]] !== null && typeof fields[keys[ind]] == 'object' &&
                        (
                            (fields[keys[ind]]['en-US'] === 'ORBERA' && (!fields.mainKeyword || (fields.mainKeyword && fields.mainKeyword['en-US'] == 'ORBERA'))) ||
                            (fields[keys[ind]]['en-US'] === 'GE_ORBERA') ||
                            (fields[keys[ind]]['en-US'] === 'CENSEO') ||
                            (fields[keys[ind]]['en-US'] === 'ZILLION') ||
                            (fields[keys[ind]]['en-US'] === 'REALAPPEAL') ||
                            (fields[keys[ind]]['en-US'] === 'TEMPLATE_ORBERA') ||
                            (fields[keys[ind]]['en-US'] === 'TEMPLATE_CENSEO') ||
                            (fields[keys[ind]]['en-US'] === 'TEMPLATE_CENTRE_LOGIN') ||
                            (keys[ind] == 'keyword' && fields[keys[ind]]['en-US'] === 'DEFAULT') ||
                            (keys[ind] == 'moduleKeyword' && fields[keys[ind]]['en-US'] === 'DEFAULT') ||
                            (keys[ind] == 'orgMainKeyword' && fields[keys[ind]]['en-US'] === 'DEFAULT' && (!fields.mainKeyword || (fields.mainKeyword && fields.mainKeyword['en-US'] == 'DEFAULT')))

                        )
                    ) {
                        return true;
                    }
                }
            }
        };

        //when 'Clear' pressed from the UI
        $scope.clearSpace = function (space) {
            space.loading = false;
            space.accessCode = "";
            space.spaceId = "";
            $scope.cancelSpace();
        };

        //when 'Cancel' pressed from the UI
        $scope.cancelSpace = function (space) {
            space.cf_client = "";
            space.cf_space = "";
            if (space.hasOwnProperty("entries")) {
                space.entries = "";
            }
        };

        //shows alert modal
        $scope.showAlertModal = function (message, callApply) {

            if ($scope.ctrlConstants.stopModalOpenTwice) return;

            $("#alertModal").modal("show");
            $scope.alertModal.message = message;

            $("#btnAlertModelOk").off("click").on("click", function (e) {
                $scope.ctrlConstants.stopModalOpenTwice = false;
            });

            if (!callApply) {
                $scope.$apply();
            }
        };

        //shows confirmation modal
        $scope.showConfirmModal = function (message, yesCallback, noCallback) {
            $("#confirmationModal").modal("show");
            $scope.confirmModal.message = message;
            $("#btnConfirmationModalYes").off("click").on("click", yesCallback);
            $("#btnConfirmationModalNo").off("click").on("click", noCallback);
            $scope.$apply();
        };

        //show/hide blackout "Syncing please wait..." spinner
        $scope.showHideBlackout = function (showOrHide) {
            $scope.ctrlConstants.blackout = showOrHide;
            $scope.ctrlConstants.blackoutText = "Creating please wait...";
            $scope.$apply();
        };

        //show/hide blackout "please wait..." spinner
        $scope.showHideBlackoutPleaseWait = function (showOrHide, isApply) {
            $scope.ctrlConstants.blackoutPleaseWait = showOrHide;
            if (!isApply) {
                $scope.$apply();
            }
        };

        //finds json node recursive and returns in an array
        $scope.findJsonNode = function (entry, key) {
            var foundData = [];
            var recursiveFilter = function (obj, key, rKey, obj2) {
                var objects = [];
                var arrayFilter = function (arr, key) {
                    $.each(arr, function (index, v) {
                        objects = objects.concat(recursiveFilter(v, "sys", key, obj2));
                    });
                };
                for (var i in obj) {
                    if (!obj.hasOwnProperty(i)) continue;
                    if (typeof obj[i]["en-US"] === "object" && i !== "sys" && !$.isArray(obj[i]["en-US"])) {
                        objects = objects.concat(recursiveFilter(obj[i]["en-US"], key, i));
                    } else if (typeof obj[i] === "object" && i !== "sys" && !$.isArray(obj[i]["en-US"])) {
                        objects = objects.concat(recursiveFilter(obj[i], key, i, obj));
                    } else if ($.isArray(obj[i]["en-US"])) {
                        arrayFilter(obj[i]["en-US"], i);
                    } else if (i === key && obj[i]) {
                        foundData.push({ "key": rKey, "value": obj });
                        break;
                    }
                }
            };
            recursiveFilter(entry, key);
            return foundData;
        };

        //filters by type and then return its sys.id
        $scope.filterByAssetOrEntry = function (arr, type) {
            return arr.filter(function (ent) {
                return ent.value.sys.linkType === type;
            }).map(function (ent) {
                return ent.value.sys.id;
            });
        };

        //load all child refs sys object in map 
        $scope.loadChildReferences = function (entry, jsonMap) {
            //finds all sys nodes and combine them in one array
            var referenceFields = $scope.findJsonNode(entry.fields, "sys"),
                children = [];

            //if we find any child reference we separate it in and add it in map against that entry for later process at once
            if (referenceFields && referenceFields.length > 0) {
                angular.copy(referenceFields, children);
            }

            //here building map which contains entry key and array of its child
            //commented this condition as it was not creating entries which dont have children
            //if (children.length > 0) {
            jsonMap[entry.sys.id] = {
                "entry": entry,
                "children": children
            };
            //}
        };

        //called as callback when clicked from 'Sync' button from UI
        $scope.getEntryData = function (entries, sourceSpace, parent, refMap) {

            var stackEntries = [],
                entryWithChildren = {},
                childrenWithChildren = {};

            //will tell whether its available in array
            var isExistInArray = function (arr, id) {
                var newArr = arr.filter(function (obj) {
                    return obj.sys.id === id;
                });
                return newArr.length > 0;
            };

            //calls recursively to get parents and their children
            var buildEntryAndChildrenDataMap = function (entries, sourceSpace, parent, refMap) {
                if (entries && entries.length > 0) {
                    angular.forEach(entries, function (entry) {
                        $scope.loadChildReferences(entry, entryWithChildren);
                    });
                }

                var startTime = new Date();

                //gets children array and load from contentful
                var getEntryDetailFromContentful = function (jsonMap, entryObject, childrenList, isPrepend) {
                    if (childrenList && childrenList.length > 0) {
                        var childObj = childrenList[0],
                            queryType = (childObj.value.sys.linkType) ? childObj.value.sys.linkType : childObj.value.sys.type;

                        sourceSpace.cf_space["get" + queryType](childObj.value.sys.id).then(function (ger) {

                            $scope.checkContentFulRequestDelay(startTime);
                            //if we have found an entry
                            if (ger) {

                                //call again for getting this children further children
                                $scope.loadChildReferences(ger, childrenWithChildren);

                                if (isPrepend) {
                                    if (!isExistInArray(stackEntries, ger.sys.id)) {
                                        stackEntries.unshift(ger); //prepend child in array    
                                    }
                                } else {
                                    if (!isExistInArray(stackEntries, ger.sys.id)) {
                                        stackEntries.push(ger); //push child in array
                                    }
                                }

                                //remove element from array
                                childrenList.shift();

                                //now again call for next index
                                getEntryDetailFromContentful(jsonMap, entryObject, childrenList);
                            }
                        }).catch(function (ex) {
                            //oz: sometimes we don't find an entry in the target space so skip here that entry
                            console.log("Exception - could not find resource with id in source space so skipping it", ex);

                            //remove element from array
                            childrenList.shift();

                            //now again call for next index
                            getEntryDetailFromContentful(jsonMap, entryObject, childrenList);
                        });
                    } else {
                        if (isPrepend) {
                            if (!isExistInArray(stackEntries, entryObject.sys.id)) {
                                stackEntries.unshift(entryObject); //once all children are in array then prepend their parent
                            }
                        } else {
                            if (!isExistInArray(stackEntries, entryObject.sys.id)) {
                                stackEntries.push(entryObject); //once all children are in array then push their parent  
                            }
                        }

                        //and remove that entry from the map
                        //delete jsonMap[entryObject.sys.id];
                        $scope.removeJsonKey(jsonMap, entryObject.sys.id);

                        //again call for next iteration in map
                        processEntryAndChildrenDataMap(jsonMap, childrenWithChildren);
                    }
                };

                //it process each node from jsonMap and gets its children and store in an array
                var processEntryAndChildrenDataMap = function (parentJsonMap, childJsonMap) {
                    if (!$.isEmptyObject(parentJsonMap)) {
                        var key = Object.keys(parentJsonMap)[0],
                            firstObject = parentJsonMap[key],
                            entryObj = firstObject.entry,
                            child = firstObject.children;

                        //call each entry to get its children from map    
                        getEntryDetailFromContentful(parentJsonMap, entryObj, child);
                    } else {
                        if (!$.isEmptyObject(childJsonMap)) {
                            var key = Object.keys(childJsonMap)[0],
                                firstObject = childJsonMap[key],
                                entryObj = firstObject.entry,
                                child = firstObject.children;

                            //call each entry to get its children from map    
                            getEntryDetailFromContentful(childJsonMap, entryObj, child, true);
                        } else {
                            console.log("DONE", "fetching all parent and their children if exists.");

                            //some times same child could be reference in both parent entries so to remove that
                            $scope.syncEntriesList = $scope.removeDuplicatesBySysId(stackEntries);

                            //if 'Sync' is pressed from Diff dialog then directly save data in contentful
                            if ($scope.ctrlConstants.isSyncFromDiffDialog) {
                                $scope.saveDataRecursive($scope.syncEntriesList);
                            } else {
                                $scope.openSelectedEntryModal();
                            }
                        }
                    }
                };

                processEntryAndChildrenDataMap(entryWithChildren);
            };

            buildEntryAndChildrenDataMap(entries, sourceSpace, parent, refMap);
        };

        //opens the selected list entries modal
        $scope.openSelectedEntryModal = function () {

            $("#alertModal").modal("hide");

            $("#btnSyncSubmit").removeProp("disabled").val("Sync");

            var $modal = $("#syncEntriesDialog").modal("show");

            $("#btnContinue").off("click").on("click", function () {
                $modal.modal("hide");
                $scope.saveDataRecursive($scope.syncEntriesList);
            });

            $scope.$apply();
        };

        //saves data recursively and delete the node once it is published
        $scope.saveDataRecursive = function (stackEntries) {

            var unPublishedEntries = [],
                totalEntries = stackEntries.length,
                progressCount = 0,
                contentType = $scope.getContentTypeFromEntries(stackEntries);

            $timeout(function () {
                $scope.ctrlConstants.blackout = true;
            });

            var skipOrCreateEntry = function (stackEntries, cer) {
                $scope.calculateProgress(++progressCount, totalEntries);
                if (cer && _.has(cer, "fields")) {
                    unPublishedEntries.push(cer);
                }
                stackEntries.shift();
                recursiveCreateEntries(stackEntries, undefined, contentType);
            };

            var recursiveCreateEntries = function (stackEntries, action, contentType) {
                if (stackEntries && stackEntries.length > 0) {
                    if (!action) {
                        var firstEntry = stackEntries[0];
                        //if entry id is found in history array then just remove it and move further without having to create it again and again
                        if (~$scope.entriesCreatedHistory.indexOf(firstEntry.sys.id)) {
                            //log
                            $scope.addLog("INFO", "duplicateFound", "This entry has already been created, so skipping it. ID: " + firstEntry.sys.id);
                            skipOrCreateEntry(stackEntries);
                        } else {
                            var eCallBack = function(response){
                                //if entry in source and target are same, then skip creating it and move on
                                if(response.isEntrySame){
                                    //log
                                    $scope.addLog("INFO", "same"+response.type+"Found", "Both source and target are same, so skipping it. ID: " + firstEntry.sys.id);
                                    skipOrCreateEntry(stackEntries);
                                }else{
                                    $scope.createResource($scope.spaces.targetSpace, firstEntry, function (cer) { //cer: created entry response
                                        $scope.entriesCreatedHistory.push(cer.sys.id);
                                        skipOrCreateEntry(stackEntries, cer);
                                    });
                                }
                            };
                            $scope.evaluateSourceAndTargetEntrySame(firstEntry, eCallBack);
                        }
                    } else if (action === "publishEntry") {
                        var lastEntryId = stackEntries[0];
                        $scope.publishResource($scope.spaces.targetSpace, lastEntryId, function (per) { //per: published entry response
                            $scope.calculateProgress(++progressCount, totalEntries);
                            stackEntries.shift();
                            recursiveCreateEntries(stackEntries, "publishEntry", contentType);
                        });
                    }
                } else if (unPublishedEntries.length > 0) {
                    progressCount = 0;
                    $scope.retryCount = 0;
                    $scope.ctrlConstants.blackoutText = "Publishing please wait...";
                    if (unPublishedEntries.length > 0) {
                        totalEntries = unPublishedEntries.length;
                        stackEntries = unPublishedEntries;
                        recursiveCreateEntries(stackEntries, "publishEntry", contentType);
                    }
                } else {
                    progressCount = 0;
                    $scope.showHideBlackout(false);
                    var message = "Synced entry(s) successfully.";
                    if ($scope.entriesNotCreated.length > 0) {
                        $scope.entriesNotCreated = $scope.removeDuplicatesBySysId($scope.entriesNotCreated);
                        var ids = $scope.entriesNotCreated.map(ent => ent.sys.id).join(", ");
                        message = "Some of the following entries are not created/published:<br/><br/>" + ids + "<br/><br/><b>Please re-sync them again</b>.";
                    }
                    $scope.showAlertModal(message);
                    $scope.ctrlConstants.stopModalOpenTwice = true;
                    //log                        
                    $scope.addLog("SUCCESS", "DONE", "All created and published.");

                    $scope.confirmModal.option = false;
                    $scope.ctrlConstants.blackoutText = "Creating please wait...";
                    $scope.entriesCreatedHistory = [];
                    $scope.entriesNotCreated = [];

                    //if we have contentTypes of entry then refresh the synced contentType and its entries
                    if (contentType && contentType.length > 0 && $scope.ctrlConstants.isSyncFromDiffDialog) {
                        angular.forEach(contentType, function (ct) {
                            if (ct && ct.hasOwnProperty("sys")) {
                                $scope.refreshDataBySpaceAndContentType($scope.spaces.targetSpace, ct.sys.id);
                            }
                        });
                    }

                    $scope.ctrlConstants.isSyncFromDiffDialog = false;
                }
            };

            recursiveCreateEntries(stackEntries, undefined, contentType);
        };

        //when "Sync" is pressed from the UI
        $scope.syncEntries = function (sourceSpace) {

            //if not target space is loaded then first load it
            if (!$scope.spaces.targetSpace.cf_space && !$scope.spaces.targetSpace.cf_space.hasOwnProperty("name")) {
                $timeout(function () {
                    $("#btnLoadTargetSpace").trigger("click");
                });
            }

            $scope.ctrlConstants.parentsHolder = {};
            $scope.ctrlConstants.childrenHolder = {};
            $scope.syncEntriesList = [];
            $scope.progressPercentage = 0;
            $scope.entriesCreatedHistory = [];
            $scope.retryCount = 0;
            $scope.entriesNotCreated = [];

            if ($scope.syncOptions.option === "SPACE_DIFF") {
                $scope.openContentTypeEntriesAccordionDialog(true);
            } else {
                $("#btnSyncSubmit").prop("disabled", "disabled").val("Fetching data...");
                $scope.queryContentfulBySpace(sourceSpace, "getEntries", sourceSpace.entries, $scope.getEntryData);
            }
        };

        //open contentType and their Entries Accordion Dialog    
        $scope.openContentTypeEntriesAccordionDialog = function (shouldCollapse) {
            //riz: as it was not opening accordion dialog because target space content is empty
            //if (!$.isEmptyObject($scope.spaces.targetSpace.contentTypesEntriesMap)) {
            var accordionClasses = {
                activeAccordion: "panel-heading-active",
                rightArrow: "glyphicon-chevron-right",
                downArrow: "glyphicon-chevron-down"
            },
                $allPanels = $(".panel-heading"),
                setAccordionCollapse = function () {
                    $(".panel-collapse").collapse("hide");
                    $allPanels.removeClass(accordionClasses.activeAccordion);
                    $allPanels.find("i.glyphicon").removeClass(accordionClasses.downArrow).addClass(accordionClasses.rightArrow);

                    $scope.removeEmptyAccordions();
                };

            $allPanels.off("click").on("click", function () {

                var $this = $(this),
                    $icon = $this.find("i.glyphicon");

                if ($this.hasClass(accordionClasses.activeAccordion)) {
                    $this.removeClass(accordionClasses.activeAccordion);
                    $icon.removeClass(accordionClasses.downArrow).addClass(accordionClasses.rightArrow);
                } else {
                    setAccordionCollapse();
                    $this.addClass(accordionClasses.activeAccordion);
                    $icon.removeClass(accordionClasses.rightArrow).addClass(accordionClasses.downArrow);
                }
            });

            if (shouldCollapse) {
                $timeout(setAccordionCollapse);
            }

            $scope.scrollTopOrBottom("divContentTypeEntriesAccordionDialog");
            $("#contentTypeEntriesAccordionDialog").modal("show");
            // } else {
            //     $scope.showAlertModal("Target space content is not loaded. Press Cancel/Clear and then 'Load Space' ");
            // }
        };

        //query contentful for getting entries/assets by providing comma separated ids
        $scope.queryContentfulBySpace = function (space, queryType, entryIds, syncCallback, parentEntry, referenceFields) {

            //riz: Split the entry ids and check if the ids length is greater than 100 then get the first 100 ids only
            var entryIdsList = entryIds.split(",");
            if (entryIdsList.length > 100) {
                entryIds = "";
                for (var i = 0; i < 100; i++) {
                    entryIds = entryIds + "," + entryIdsList[i];
                }
            }

            $scope.showHideBlackoutPleaseWait(false, true);

            space.cf_space[queryType]({ 'sys.id[in]': entryIds.replace(/\s/g, '') })
                .then(function (entries) {
                    if (entries.total > 0) {
                        if (syncCallback) {
                            syncCallback.call(null, entries, space, parentEntry, referenceFields);
                        } else {
                            $scope.showAlertModal("ERROR: no callback provided for syncing.");
                        }
                    } else {
                        $("#btnSyncSubmit").removeProp("disabled").val("Sync");
                        if (entryIds.split(",").length > 1) {
                            //for the rest skip the invalid one, because this is called recursively as a callback from $scope.getEntryData function                                
                            console.log("queryContentfulBySpace", "Skipping invalid entry id(s)");
                        } else {
                            if ($scope.syncOptions.option === "SPACE_DIFF") {
                                //log
                                $scope.addLog("SUCCESS", "entryNotFound", "Unable to find entry with the given id, creating new one.");

                            } else {
                                //in case of one entry wrong show alert
                                if ($scope.syncEntriesList.length === 0) {
                                    $scope.showAlertModal("Invalid entry id, please add correct one.");
                                }
                            }
                        }
                    }
                });
        };

        //creates entry asset in the space provided
        $scope.createResource = function (targetSpace, entry, successCallback) {

            //deletes source space object from entry
            if (entry && entry.sys && entry.sys.space) {
                $scope.removeJsonKey(entry.sys, "space");
            }

            var startTime = new Date(),
                sysId = "";

            $scope.removeJsonKey(entry, "$$hashKey");

            if (targetSpace && targetSpace.cf_space && targetSpace.cf_space.hasOwnProperty("name")) {
                if (entry && entry.sys && entry.sys.hasOwnProperty("contentType")) {
                    var startTime = new Date();

                    var createEntry = function (newEntry) {

                        var contentTypeId = newEntry.sys.contentType.sys.id;
                        sysId = newEntry.sys.id;
                        $scope.removeJsonKey(newEntry, "sys");
                        newEntry.sys = {
                            "id": sysId
                        };

                        $scope.removeFieldsNode(newEntry);

                        //create it
                        targetSpace.cf_space.createEntry(contentTypeId, newEntry).catch(function (cerror) {
                            console.log("PHEW", contentTypeId, newEntry);
                            $scope.entriesNotCreated.push(newEntry);
                            return $scope.catchError("catch-found-createEntry", cerror, newEntry);
                        }).then(function (response) {
                            $scope.checkContentFulRequestDelay(startTime);
                            if (response && _.has(response, "fields")) {
                                //log                                                
                                $scope.addLog("SUCCESS", "createEntry", response);
                            }
                            if (successCallback) {
                                successCallback.call(null, response);
                            }
                        });
                    };

                    //checks the entry
                    targetSpace.cf_space.getEntry(entry.sys.id).catch(function (gerror) {
                        return $scope.catchError("catch-checks-getEntry", gerror);
                    }).then(function (ger) {
                        $scope.checkContentFulRequestDelay(startTime);
                        //if we have found an entry
                        if (ger) {
                            if (ger.sys && ger.sys.publishedVersion) {
                                //un-publish it
                                targetSpace.cf_space.unpublishEntry(ger).catch(function (uerror) {
                                    return $scope.catchError("catch-found-unpublishEntry", uerror);
                                }).then(function (uer) {
                                    $scope.checkContentFulRequestDelay(startTime);
                                    if (uer) {
                                        //log
                                        $scope.addLog("SUCCESS", "unpublishEntry", uer);

                                        //delete it
                                        targetSpace.cf_space.deleteEntry(uer).catch(function (derror) {
                                            return $scope.catchError("catch-found-deleteEntry", derror);
                                        }).then(function (der) {
                                            //log                                                
                                            $scope.addLog("SUCCESS", "deleteEntry", uer);

                                            $scope.checkContentFulRequestDelay(startTime);
                                            $scope.removeJsonKey(entry, "$$hashKey");
                                            //create it
                                            createEntry(entry);
                                        });
                                    }
                                });
                            } else {
                                //delete it
                                targetSpace.cf_space.deleteEntry(ger.sys.id).catch(function (derror) {
                                    return $scope.catchError("catch-found-deleteEntry-2", derror);
                                }).then(function (der) {
                                    //log                                                
                                    $scope.addLog("SUCCESS", "deleteEntry", ger);

                                    $scope.checkContentFulRequestDelay(startTime);
                                    $scope.removeJsonKey(entry, "$$hashKey");
                                    //create it
                                    createEntry(entry);
                                });
                            }
                        } else {
                            //just create it and on its response publish it as well in callback
                            $scope.removeJsonKey(entry, "$$hashKey");
                            createEntry(entry);
                        }
                    });
                } else {
                    if (entry && entry.sys) {
                        sysId = entry.sys.id;
                    }
                    $scope.removeJsonKey(entry, "sys");
                    entry.sys = {
                        "id": sysId
                    };
                    startTime = new Date();

                    var processAsset = function (createdAsset, forwardCallback) {
                        //process asset so that the url will be constructed of the target space
                        targetSpace.cf_space.processAssetFile(createdAsset, "en-US")
                            .catch(function (proError) {
                                return $scope.catchError("catch-found-processAsset", proError);
                            }).then(function (response) {
                                $scope.checkContentFulRequestDelay(startTime);

                                //log                                                
                                $scope.addLog("SUCCESS", "processAsset", createdAsset);

                                if (forwardCallback) {
                                    forwardCallback.call(null, createdAsset);
                                }
                            });
                    };

                    var createAsset = function (asset) {

                        var newAsset = $scope.buildAssetObject(asset);

                        targetSpace.cf_space.createAsset(newAsset).catch(function (cerror) {
                            return $scope.catchError("catch-found-createAsset", cerror);
                        }).then(function (response) {
                            $scope.checkContentFulRequestDelay(startTime);
                            if (response) {
                                //log                                                
                                $scope.addLog("SUCCESS", "createAsset", response);

                                processAsset(response, successCallback);
                            } else {
                                //log                                                
                                $scope.addLog("ERROR", "createAsset", response);
                            }
                        });
                    };

                    //checks the asset
                    targetSpace.cf_space.getAsset(entry.sys.id).catch(function (gerror) {
                        return $scope.catchError("catch-checks-getAsset", gerror);
                    }).then(function (ger) {
                        $scope.checkContentFulRequestDelay(startTime);
                        //if we have found an entry
                        if (ger) {
                            if (ger.sys && ger.sys.publishedVersion) {
                                //un-publish it
                                targetSpace.cf_space.unpublishAsset(ger).catch(function (uerror) {
                                    return $scope.catchError("catch-found-unpublishAsset", uerror);
                                }).then(function (uer) {
                                    $scope.checkContentFulRequestDelay(startTime);
                                    if (uer) {
                                        //log                                                
                                        $scope.addLog("SUCCESS", "unpublishAsset", uer);

                                        //delete it
                                        targetSpace.cf_space.deleteAsset(uer).catch(function (derror) {
                                            return $scope.catchError("catch-found-deleteAsset", derror);
                                        }).then(function (der) {
                                            //log                                                
                                            $scope.addLog("SUCCESS", "deleteAsset", uer);

                                            $scope.checkContentFulRequestDelay(startTime);
                                            //create it
                                            $scope.removeJsonKey(entry, "$$hashKey");
                                            if (entry.fields.hasOwnProperty("file")) {
                                                createAsset(entry);
                                            }
                                        });
                                    }
                                });
                            } else {
                                //delete it                                    
                                targetSpace.cf_space.deleteAsset(ger.sys.id).catch(function (derror) {
                                    return $scope.catchError("catch-found-deleteAsset-2", derror);
                                }).then(function (der) {
                                    //log                                                
                                    $scope.addLog("SUCCESS", "deleteAsset", ger);

                                    $scope.checkContentFulRequestDelay(startTime);
                                    //create it
                                    $scope.removeJsonKey(entry, "$$hashKey");
                                    if (entry.fields.hasOwnProperty("file")) {
                                        createAsset(entry);
                                    }
                                });
                            }
                        } else {
                            //just create it and on its response publish it as well in callback
                            if (entry.fields.hasOwnProperty("file")) {
                                $scope.removeJsonKey(entry, "$$hashKey");
                                createAsset(entry);
                            }
                        }
                    });
                }
            } else {
                $scope.showHideBlackout(false);
            }
        };

        //publishes the asset and entry in the space
        $scope.publishResource = function (targetSpace, entry, successCallback) {
            if (targetSpace && targetSpace.cf_space && targetSpace.cf_space.hasOwnProperty("name")) {
                var startTime = new Date();
                if (entry && entry.sys && entry.sys.hasOwnProperty("contentType")) {
                    targetSpace.cf_space.getEntry(entry.sys.id).then(function (gotEntry) {
                        $scope.checkContentFulRequestDelay(startTime);
                        targetSpace.cf_space.publishEntry(gotEntry).catch(function (perror) {
                            //if we found 'notResolvable' error then again call until it gives success
                            if (perror && perror.details && perror.details.errors) {
                                var errorName = perror.details.errors[0].name;
                                switch (errorName) {
                                    case "notResolvable":
                                        $scope.addLog("ERROR", "publishEntry", "Error in publishing, retrying to publish.");
                                        $scope.delay(2500);
                                        $scope.retryCount++;
                                        if ($scope.retryCount < 2) { //try publishing twice
                                            $scope.publishResource(targetSpace, entry, successCallback);
                                        } else {
                                            //phew! tried to publish but failed
                                            $scope.entriesNotCreated.push(entry);
                                            $scope.addLog("ERROR", "re-publishEntry", perror);
                                            $scope.retryCount = 0;
                                            if (successCallback) {
                                                successCallback.call(null, { status: "skip" });
                                            }
                                        }
                                        break;
                                    default:
                                        $scope.entriesNotCreated.push(entry);
                                        $scope.addLog("ERROR", "unprocessableEntry", JSON.stringify(perror));
                                        break;
                                }
                            }
                            return perror;
                        }).then(function (response) {
                            $scope.checkContentFulRequestDelay(startTime);
                            if (response && _.has(response, "fields")) {
                                //log                                                
                                $scope.addLog("SUCCESS", "publishEntry", response);
                            }
                            if (successCallback) {
                                successCallback.call(null, response);
                            }
                        });
                    }).catch(function (gerror) {
                        console.log("publishResource-getEntry-catch", gerror);
                        return gerror;
                    });
                } else {
                    targetSpace.cf_space.getAsset(entry.sys.id).then(function (gAsset) {
                        targetSpace.cf_space.publishAsset(gAsset).catch(function (paerror) {
                            //if we found 'notResolvable' error then again call until it gives success
                            if (paerror && paerror.details && paerror.details.errors && paerror.details.errors[0].name === "notResolvable") {
                                $scope.addLog("ERROR", "publishAsset", "Error in publishing, retrying to publish.");
                                $scope.delay(2500);
                                $scope.publishResource(targetSpace, entry, successCallback);
                            }
                            return paerror;
                        }).then(function (response) {
                            $scope.checkContentFulRequestDelay(startTime);
                            if (response && _.has(response, "fields")) {
                                //log                                                
                                $scope.addLog("SUCCESS", "publishAsset", response);
                            }
                            if (successCallback) {
                                successCallback.call(null, response);
                            }
                        });
                    });
                }
            } else {
                $scope.showHideBlackout(false);
            }
        };

        //checks for request time and add delay if exceeds the limit
        $scope.checkContentFulRequestDelay = function (requestStartTime) {
            var currentTime = new Date(),
                timeDiff = currentTime.getTime() - requestStartTime.getTime();

            $scope.delay(500);

            /*console.log("REQUEST_TIME", timeDiff + " Should > " + $scope.ctrlConstants.singleRequestMinTime + "So Delay: " + ($scope.ctrlConstants.singleRequestMinTime - timeDiff))
             if ($scope.ctrlConstants.singleRequestMinTime > timeDiff) {
             $scope.delay($scope.ctrlConstants.singleRequestMinTime - timeDiff);
             }*/
        };

        //adds delay between multiple requests
        $scope.delay = function (sleepTime) {
            var requestTime = new Date().getTime();
            while (new Date().getTime() < requestTime + sleepTime) {
            }
        };

        //calculates percentage only
        $scope.calculatePercentage = function (value, total) {
            var pValue = Math.round((value / total) * 100);
            if (pValue > 100) {
                pValue = 100;
            }
            return pValue;
        };

        //calculates percentage when saving in contentful
        $scope.calculateProgress = function (value, total) {
            $scope.progressPercentage = $scope.calculatePercentage(value, total);
            $scope.$apply();
        };

        //plucks description from the fields object
        $scope.pluckDescription = function (entry) {
            var keys = Object.keys(entry.fields),
                description = keys.filter(function (key) {
                    return key.toLowerCase().indexOf("description") > -1;
                }),
                entryDescription = "[No Description Found]";

            if (description && description.length > 0) {
                entryDescription = entry.fields[description[0]]["en-US"];
            } else if (keys.indexOf("title") > -1) {
                entryDescription = entry.fields.title["en-US"];
            } else if (keys.indexOf("moduleKeyword") > -1) {
                entryDescription = entry.fields.moduleKeyword["en-US"];
            }

            return entryDescription;
        };

        //opens diff view modal to show difference between sourceSpace and targetSpace content_types, entries etc
        $scope.openDiffViewModal = function (contentType, type, modelTitle, entry) {

            $scope.alertModal.model = {
                modelContentType: contentType,
                modelEntry: entry,
                modelTitle: modelTitle,
                type: type
            };

            var jsonDiff = $scope.getJsonDifference(contentType, type, entry),
                differenceText = diffview.buildView({
                    baseTextLines: jsonDiff.sourceText,
                    newTextLines: jsonDiff.targetText,
                    opcodes: jsonDiff.opCodes,
                    baseTextName: "Source Space Content",
                    newTextName: "Target Space Content",
                    viewType: 0
                });

            $("#diffOutputDiv").empty().append(differenceText);

            $scope.scrollTopOrBottom("divContentTypeEntriesAccordionDialog");
            $("#contentTypeEntriesAccordionDialog").modal("hide");

            $scope.scrollTopOrBottom("divContentDiffDialog");
            $("#contentDiffDialog").modal("show");
        };

        //checks two objects are equal, type: contentType, entries as our json has these nodes
        $scope.isAnyDifference = function (firstObject, type, entry) {
            //gets the same entry from target space map and see difference with the source space entry
            var secondObject = $scope.spaces.targetSpace.contentTypesEntriesMap[firstObject.sys.id],
                returnObject = {
                    "found": false,
                    "status": "NOT_EQUAL"
                };
            if (secondObject) {
                var data = secondObject[type];
                if (data) {
                    if (type === "entries") {
                        data = $scope.findEntryById(data, entry);
                        //in case of entries also get from source space to match the entry with target space
                        firstObject = $scope.spaces.sourceSpace.contentTypesEntriesMap[firstObject.sys.id];
                        firstObject = $scope.findEntryById(firstObject.entries, entry);
                    }
                    var isEqual = false;
                    if (data.fields) {
                        isEqual = angular.equals(firstObject.fields, data.fields);
                        if (isEqual) {
                            returnObject.status = "EQUAL";
                        }
                    } else {
                        returnObject.status = "NOT_IN_TARGET";
                    }
                    returnObject.found = isEqual;
                    return returnObject;
                }
                return returnObject;
            } else {
                returnObject.status = "NOT_IN_TARGET";
            }
            return returnObject;
        };

        //gets json difference between source and target entry
        $scope.getJsonDifference = function (contentType, type, entry) {

            var jsonDiff = {
                "opCodes": undefined,
                "diffCount": 0,
                "sourceText": "",
                "targetText": "",
                "status": ""
            };

            if (contentType && contentType.sys) {
                var targetEntry = $scope.spaces.targetSpace.contentTypesEntriesMap[contentType.sys.id],
                    targetText = "";

                if (targetEntry) { //type: contentType, entries as our json has these nodes
                    var data = targetEntry[type];
                    if (type === "entries") {
                        data = $scope.findEntryById(data, entry);
                    }
                    if (data && data.fields) {
                        targetText = !JSON.stringify(data.fields, null, 2) ? "" : JSON.stringify(data.fields, null, 2);
                    }
                }

                var sourceText = JSON.stringify(contentType.fields, null, 2);
                if (type === "entries") {
                    //in case of entries also get from source space to match the entry with target space
                    var sourceData = $scope.spaces.sourceSpace.contentTypesEntriesMap[contentType.sys.id];
                    if (sourceData && sourceData.entries) {
                        sourceData = $scope.findEntryById(sourceData.entries, entry);
                        sourceText = JSON.stringify(sourceData.fields, null, 2);
                    }
                }

                sourceText = difflib.stringAsLines(sourceText);
                targetText = difflib.stringAsLines(targetText);

                var matcher = new difflib.SequenceMatcher(sourceText, targetText);

                jsonDiff.opCodes = matcher.get_opcodes();
                jsonDiff.diffCount = Math.abs((matcher.a.length - matcher.b.length) + 1);
                jsonDiff.sourceText = sourceText;
                jsonDiff.targetText = targetText;

                if (targetText.length === 1 && targetText[0] === "") {
                    jsonDiff.status = "NOT_IN_TARGET"; //means not available in target
                }

                if (sourceText.length === 1 && sourceText[0] === "{}") {
                    jsonDiff.status = "EMPTY_OBJECT"; //means source has empty object
                }
            }

            return jsonDiff;
        };

        //shows diff count
        $scope.showDiffCount = function (contentType, entry, type) {
            return $scope.getJsonDifference(contentType, type, entry).diffCount;
        };

        //when 'Sync from Source to Target' is pressed from diffView dialog
        $scope.syncFromDialog = function (contentType, entry, type, entriesProvided) {
            var sourceSpace = $scope.spaces.sourceSpace;
            sourceSpace.entries = (type === "entries") ? (entry && entry.hasOwnProperty("sys") ? entry.sys.id : "") : "";
            $("#contentDiffDialog").modal("hide");
            $scope.ctrlConstants.blackoutPleaseWait = true;
            //check here if the contentType is available in target space                
            $scope.spaces.targetSpace.cf_space.getContentType(contentType.sys.id).catch(function (cterror) {
                $scope.catchError("catch-syncFromDialog-getContentType", cterror);
                if (cterror && cterror.sys && cterror.sys.id === "NotFound") {
                    if (type === "entries") {
                        $scope.confirmModal.option = false;
                        $scope.showAlertModal("Content type not found in target space. Please sync content type first.");

                        //log                                                
                        $scope.addLog("ERROR", "getContentType", "Content type not found in target space. Please sync content type first.");

                    } else {
                        var yesCallback = function () {
                            $("#confirmationModal").modal("hide");
                            $scope.showHideBlackoutPleaseWait(true);
                            //create contentType only
                            var sysId = contentType.sys.id;
                            $scope.removeJsonKey(contentType, "sys");
                            contentType.sys = {
                                "id": sysId
                            };
                            var startTime = new Date();
                            //create contentType                      
                            $scope.removeJsonKey(contentType, "$$hashKey");
                            $scope.spaces.targetSpace.cf_space.createContentType(contentType).catch(function (cerror) {
                                return $scope.catchError("catch-syncFromDialog-createContentType", cerror);
                            }).then(function (cType) {
                                $scope.checkContentFulRequestDelay(startTime);
                                if (cType) {
                                    //log                                                
                                    $scope.addLog("SUCCESS", "createContentType", cType);

                                    //publish contentType
                                    $scope.spaces.targetSpace.cf_space.publishContentType(cType, 1).catch(function (perror) {
                                        return $scope.catchError("catch-syncFromDialog-createContentType", perror);
                                    }).then(function (response) {
                                        $scope.checkContentFulRequestDelay(startTime);
                                        $scope.showHideBlackoutPleaseWait(false);
                                        if (response) {
                                            //log                                                
                                            $scope.addLog("SUCCESS", "publishContentType", response);

                                            //if 'Also sync entries' option is selected then also create all entries of this contentType in the target space
                                            if ($scope.confirmModal.option) {
                                                if (entriesProvided && entriesProvided.length > 0) {
                                                    $scope.grabAllEntriesByContentTypeAndCreateThem(sourceSpace, undefined, entriesProvided);
                                                } else {
                                                    $scope.grabAllEntriesByContentTypeAndCreateThem(sourceSpace, contentType);
                                                }
                                            } else {
                                                $scope.showAlertModal("Content type synced successfully.");

                                                //refresh the synced contentType and its entries
                                                $scope.refreshDataBySpaceAndContentType($scope.spaces.targetSpace, contentType.sys.id);
                                            }
                                        }
                                    });
                                }
                            });
                        };
                        $scope.showConfirmModal("Content type not found in target space. Do you want to create it?", yesCallback, null);
                    }
                } else {
                    $scope.showHideBlackoutPleaseWait(false);
                }
                $("#btnAlertModelOk").off("click").on("click", function () {
                    $scope.ctrlConstants.stopModalOpenTwice = false;
                    $("#contentTypeEntriesAccordionDialog").modal("show");
                });
            }).then(function (response) {
                if (response) {
                    //confirmationModal 'yes' callback
                    var yesCallback = function () {
                        $("#confirmationModal").modal("hide");
                        $scope.showHideBlackoutPleaseWait(true);
                        var startTime = new Date();
                        //gets contentType
                        $scope.spaces.targetSpace.cf_space.getContentType(contentType.sys.id).catch(function (gerror) {
                            $scope.confirmModal.option = false;
                            return $scope.catchError("catch-syncFromDialog-getContentType", gerror);
                        }).then(function (response) {
                            if (response) {
                                //updates the target contentType with source changed content_type fields
                                response.fields = contentType.fields;
                                //update contentType
                                $scope.spaces.targetSpace.cf_space.updateContentType(response).catch(function (uerror) {
                                    $scope.showHideBlackoutPleaseWait(false);
                                    return $scope.catchError("catch-syncFromDialog-updateContentType", uerror);
                                }).then(function (response) {
                                    if (_.has(response, "fields")) {
                                        //log
                                        $scope.addLog("SUCCESS", "updateContentType", response);

                                        //publish contentType
                                        $scope.spaces.targetSpace.cf_space.publishContentType(response).catch(function (perror) {
                                            return $scope.catchError("catch-syncFromDialog-updateContentType", perror);
                                        }).then(function (response) {
                                            $scope.showHideBlackoutPleaseWait(false);
                                            $scope.checkContentFulRequestDelay(startTime);
                                            if (response) {
                                                //log
                                                $scope.addLog("SUCCESS", "publishContentType", response);
                                                //if 'Also sync entries' option is selected then also create all entries of this contentType in the target space
                                                if ($scope.confirmModal.option) {
                                                    if (entriesProvided && entriesProvided.length > 0) {
                                                        $scope.grabAllEntriesByContentTypeAndCreateThem(sourceSpace, undefined, entriesProvided);
                                                    } else {
                                                        $scope.grabAllEntriesByContentTypeAndCreateThem(sourceSpace, contentType);
                                                    }
                                                } else {
                                                    $scope.showAlertModal("Content type synced successfully.");

                                                    //refresh the synced contentType and its entries
                                                    $scope.refreshDataBySpaceAndContentType($scope.spaces.targetSpace, contentType.sys.id);
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    };

                    //removing extra jsonNodes that are causing issue, so remove them here to avoid conflict 
                    $scope.removeContentTypeExtraNodes(contentType);
                    $scope.removeContentTypeExtraNodes(response);

                    //check if both(source, target) contenttypes of this entry are equals then go in else show alert                                                
                    var isContentTypeSame = angular.equals(contentType.fields, response.fields);
                    console.log("CONTENT_TYPE_SAME", isContentTypeSame);
                    if (isContentTypeSame || type === "entries") {
                        $("#btnAlertModelOk").off("click").on("click", function (e) {
                            $scope.ctrlConstants.stopModalOpenTwice = false;
                        });
                        if (type === "entries" && isContentTypeSame) {
                            if (sourceSpace.entries) {
                                //if we have comma separated entries then pass it to following function to get their data from contentful
                                $scope.ctrlConstants.isSyncFromDiffDialog = true;
                                $scope.queryContentfulBySpace(sourceSpace, "getEntries", sourceSpace.entries, $scope.getEntryData);
                            } else {
                                //else we have entries here just pass them to contentful for their data fetching from it
                                if (entriesProvided && entriesProvided.length > 0) {
                                    $scope.grabAllEntriesByContentTypeAndCreateThem(sourceSpace, undefined, entriesProvided);
                                } else {
                                    $scope.grabAllEntriesByContentTypeAndCreateThem(sourceSpace, contentType);
                                }
                            }
                        } else {
                            $scope.showHideBlackoutPleaseWait(false);
                            $scope.showConfirmModal("We have found the content type differences. Do you want to sync the content type in the target space?", yesCallback, null);
                        }
                    } else {
                        $scope.showHideBlackoutPleaseWait(false);
                        $scope.showConfirmModal("We have found the content type differences. Do you want to sync the content type in the target space?", yesCallback, null);
                    }
                }
            });
        };

        //finds entry from list by entry id
        $scope.findEntryById = function (entriesList, entry) {
            var foundEntry = "";
            if (entriesList && entriesList.length > 0) {
                foundEntry = entriesList.filter(function (ent, index) {
                    return ent.sys.id === entry.sys.id;
                });
                if (foundEntry && foundEntry.length > 0) {
                    foundEntry = foundEntry[0];
                }
            }
            return foundEntry;
        };

        //removes key from jsonObject
        $scope.removeJsonKey = function (jsonObject, keyToRemove) {
            if (jsonObject.hasOwnProperty(keyToRemove)) {
                delete jsonObject[keyToRemove];
            }
        };

        //grab all entries of this contentType from source entry map
        $scope.grabAllEntriesByContentTypeAndCreateThem = function (sourceSpace, contentType, entriesProvided) {
            var entriesList = entriesProvided || sourceSpace.contentTypesEntriesMap[contentType.sys.id].entries;
            if (entriesList && entriesList.length > 0) {
                sourceSpace.entries = entriesList.map(ent => ent.sys.id).join(",");
                $scope.ctrlConstants.isSyncFromDiffDialog = true;
                $scope.queryContentfulBySpace(sourceSpace, "getEntries", sourceSpace.entries, $scope.getEntryData);
            }
        };

        //for catching errors
        $scope.catchError = function (action, error, entry) {
            var returnVal = (error) ? error : false;

            //if we have found 'NotFound' then return 'undefined' to go in the .then clause and continue execution
            if (error && error.sys && error.sys.id === "NotFound") {
                returnVal = undefined;
            }

            //logs in console
            if (returnVal) {
                console.log("catchError", action, error, returnVal);
                var errorMessage = "Error see browser console for details.";
                if (angular.isObject(returnVal)) {
                    //add some delay in case of limit exceeds
                    if (returnVal.hasOwnProperty("sys") && $scope.ctrlConstants.ignoreKeys.indexOf(returnVal.sys.id) > -1) {
                        if (returnVal.sys.id.toLowerCase() === "ratelimitexceeded") {
                            $scope.delay(2500);
                            errorMessage = "RateLimitExceeded adding some delay.";
                        } else {
                            if ($scope.ctrlConstants.ignoreKeys.indexOf(returnVal.sys.id) === -1) {
                                $scope.addLog("ERROR", action, errorMessage);
                            }
                        }
                    } else {
                        errorMessage = JSON.stringify(error);
                        if (entry) {
                            errorMessage += "<br/> ============ <br/>" + JSON.stringify(entry);
                        }
                        $scope.addLog("ERROR", action, errorMessage);
                    }
                } else {
                    $scope.addLog("ERROR", action, errorMessage);
                }
            }

            return returnVal;
        };

        //after syncing refresh the contentType data to get the latest changes we just made
        $scope.refreshDataBySpaceAndContentType = function (sourceSpace, contentTypeId) {
            var startTime = new Date();
            if (contentTypeId) {
                //first get the contentType
                sourceSpace.cf_space.getContentType(contentTypeId).catch(function (gcterror) {
                    return $scope.catchError("catch-refreshDataBySpaceAndContentType-getContentType", gcterror);
                }).then(function (response) {
                    $scope.checkContentFulRequestDelay(startTime);
                    if (response) {
                        startTime = new Date();
                        //query contentful to get entries of this contentType       
                        //adding a small amount of time to fetch from contentful as it is not published yet    
                        $scope.delay(1000);

                        sourceSpace.cf_space.getEntries({ content_type: contentTypeId }).catch(function (gerror) {
                            return $scope.catchError("catch-refreshDataBySpaceAndContentType-getEntries", gerror);
                        }).then(function (entries) {
                            $scope.checkContentFulRequestDelay(startTime);
                            //override the only local data map of contentType and its entries that we just synced
                            sourceSpace.contentTypesEntriesMap[contentTypeId] = {
                                "entries": entries,
                                "contentType": response
                            };
                        });
                    }
                });
            }
        };

        //gets the content type of passing entries
        $scope.getContentTypeFromEntries = function (entries) {
            var contentTypes = _.filter(entries, e => (e.sys && e.sys.contentType && e.sys.contentType.sys.id)).map(m => m.sys.contentType);
            return _.uniq(contentTypes, e => e.sys.id);
        };

        //will sync all entries to target space of provided contentType
        $scope.syncAllByContentType = function (contentType) {
            $("#contentTypeEntriesAccordionDialog").modal("hide");
            $scope.confirmModal.option = true;
            $scope.syncFromDialog(contentType, undefined, "entries");
        };

        //will sync all new entries (not available in target) to target space of provided contentType
        $scope.syncNewEntries = function (contentType) {
            $("#contentTypeEntriesAccordionDialog").modal("hide");
            var entries = $scope.isNewEntriesInSpace(contentType);
            $scope.confirmModal.option = true;
            $scope.syncFromDialog(contentType, undefined, "entries", entries.newEntries);
        };

        //will tell if any of the child entries of given content type is not available in target content
        $scope.isNewEntriesInSpace = function (contentType, fromUI) {
            var sourceEntries = $scope.spaces.sourceSpace.contentTypesEntriesMap[contentType.sys.id].entries,
                foundDifference = "",
                notInTargetEntries = [],
                changedEntries = [];

            if (sourceEntries && sourceEntries.length > 0) {
                $.each(sourceEntries, function (index, entry) {
                    foundDifference = $scope.isAnyDifference(contentType, "entries", entry);
                    if (foundDifference.status === "NOT_IN_TARGET") {
                        notInTargetEntries.push(entry);
                    } else if (foundDifference.status === "NOT_EQUAL") {
                        changedEntries.push(entry);
                    }
                });
            }

            if (fromUI) {
                return {
                    "found": notInTargetEntries.length > 0, //will indicate whether any new entries found which are not available in target space (its new ones only)
                    "totalCount": notInTargetEntries.length + changedEntries.length
                };
            }

            return {
                "found": notInTargetEntries.length > 0, //will indicate whether any new entries found which are not available in target space (its new ones only)
                "newEntries": notInTargetEntries
            };
        };

        //removes empty accordions  
        $scope.removeEmptyAccordions = function () {
            $(".panel-default").each(function (index, panel) {
                var $panel = $(panel),
                    $li = $panel.find(".panel-collapse").find("ul.list-group li");
                if ($li.has("div.row").length === 0 && $panel.has("a.only-show-diff").length === 0) {
                    $panel.hide();
                }
            });
        };

        //logs error in Logs VIEW
        $scope.addLog = function (type, action, message) {
            var clazz = (type === "ERROR") ? "text-danger" : (type === "SUCCESS") ? "text-success" : "text-info";

            if (message && typeof message === "object") {
                var logText = "";

                //grab its sys.id
                if (message.hasOwnProperty("sys")) {
                    //adding some delay in case of limit exceeds
                    if (message.sys.id === "RateLimitExceeded") {
                        $scope.delay(2500);
                    } else {
                        if ($scope.ctrlConstants.ignoreKeys.indexOf(message.sys.id) === -1) {
                            logText = "ID: " + message.sys.id;

                            //grab description or title whatever json node we find
                            var jsonNodes = [];
                            if (message.hasOwnProperty("fields") && message.fields) {
                                jsonNodes = Object.keys(message.fields).filter(function (jsonKey) {
                                    return (jsonKey.toLowerCase().indexOf("title") > -1 || jsonKey.toLowerCase().indexOf("description") > -1);
                                });
                            }

                            if (jsonNodes && jsonNodes.length > 0) {
                                //gets the jsonNode value from the founded one jsonKey
                                //will run for entries
                                if (message.fields.hasOwnProperty(jsonNodes[0])) {
                                    logText += ", " + jsonNodes[0].toUpperCase() + ": " + message.fields[jsonNodes[0]]["en-US"];
                                }
                            } else if (message.hasOwnProperty("name")) {//this case probably will run in case of create/publish contentTypes
                                logText += ", NAME: " + message.name;
                            }

                            logText += " in target space (" + $scope.spaces.targetSpace.cf_space.name + ")";

                        } else {
                            clazz = "text-danger";
                            logText = "Something went wrong, please see browser console for details.";
                            return;
                        }

                        message = logText;
                    }
                }
            }

            if (action && message && message.indexOf("undefined") === -1) {
                message = "[" + action + "]: " + message;
                $scope.logging.push({ "color": clazz, "message": message });
            }

            $scope.scrollTopOrBottom("divLoggingContainer", true);
            $scope.$apply();
        };

        //clears log window
        $scope.clearLogWindow = function () {
            $scope.logging = [];
        };

        //removes json node in the contenttype fields object
        $scope.removeContentTypeExtraNodes = function (contentType) {
            var nodesToRemove = ["localized", "disabled", "required"];
            contentType.fields.forEach(function (cType) {
                nodesToRemove.forEach(function (node) {
                    if (cType.hasOwnProperty(node) && !cType[node]) {
                        $scope.removeJsonKey(cType, node);
                    }
                });
            });
        };

        //copy to clipboard
        $scope.copyToClipboard = function () {

            var $tempTextArea = $(".copy-text-area"),
                $divLoggingContainer = $("#divLoggingContainer");

            $tempTextArea.val($divLoggingContainer.text().trim()).select();

            try {
                var successful = document.execCommand("copy"),
                    msg = successful ? "successful" : "unsuccessful";

                if (msg === "successful") {
                    $scope.showAlertModal("Copied to your clipboard.", true);
                } else {
                    console.log("Copying text command was " + msg);
                }
            } catch (err) {
                console.log("Oops, unable to copy");
            }
        };

        //remove duplicates from list having same sys.id
        $scope.removeDuplicatesBySysId = function (arr) {
            return _.uniq(arr, item => item.sys.id);
        };

        //scrolls to top of the container
        $scope.scrollTopOrBottom = function (divId, bottomOrTop) {
            var $elem = !(divId) ? $("body") : $("#" + divId);
            $elem.stop().animate({
                scrollTop: (bottomOrTop) ? $elem[0].scrollHeight : 0
            }, 400);
        };

        //builds asset object
        $scope.buildAssetObject = function (fileObject) {

            var nullSafe = function (node, nextAttr) {
                var returnedValue = "";

                if (fileObject.fields[node]) {
                    returnedValue = fileObject.fields[node]["en-US"];
                    if (nextAttr) {
                        returnedValue = fileObject.fields[node]["en-US"][nextAttr];
                    }
                }
                return returnedValue;
            };

            return {
                "fields": {
                    "title": { "en-US": nullSafe("title") },
                    "file": {
                        "en-US": {
                            "upload": encodeURI("https:" + fileObject.fields.file["en-US"].url),
                            "fileName": nullSafe("file", "fileName"),
                            "contentType": nullSafe("file", "contentType")
                        }
                    },
                    "description": {
                        "en-US": nullSafe("description")
                    }
                },
                "sys": {
                    "id": fileObject.sys.id
                }
            };
        };

        //TODO: refactor later via model value
        $scope.showMessage = function () {
            var $accordionParent = $("#contentTypeEntriesAccordion"),
                $panel = $accordionParent.children(".panel");

            return $panel.length === $panel.filter(":not(:visible)").length;
        };

        //this method evaluates whether source entry are same as target.
        $scope.evaluateSourceAndTargetEntrySame = function(sourceEntry, callback) {
            var queryType = sourceEntry.sys.linkType || sourceEntry.sys.type;
            $scope.spaces.targetSpace.cf_space["get" + queryType](sourceEntry.sys.id).then(targetEntry => {
                if (targetEntry) {
                    var isSame = angular.equals(sourceEntry.fields, targetEntry.fields);
                    //if target entry is not published somehow, then don't skip it even both are same
                    if (!_.has(targetEntry.sys, "publishedVersion")) {
                        isSame = false;
                    }
                    callback.call(null, {isEntrySame: isSame, "type": queryType});
                } else {
                    callback.call(null, {isEntrySame: false, "type": queryType});
                }
            }).catch(error => {
                callback.call(null, {isEntrySame: false, "type": queryType});
            });
        };

        //removes fields node and just return sys node for child references
        $scope.removeFieldsNode = function(entry) {
            var sysNodes = $scope.findJsonNode(entry.fields, "sys");

            var updateEntryWithSysNodeOnly = function (ent) {
                $scope.removeJsonKey(ent, "fields");
                $scope.removeJsonKey(ent, "$$hashKey");
                ent.sys = {
                    "id": ent.sys.id,
                    "type": "Link",
                    "linkType": ent.sys.linkType || "Entry"
                };
            };

            if (sysNodes && sysNodes.length > 0) {
                angular.forEach(sysNodes, (node, index) => {
                    var key = node.key;
                    if (_.has(entry.fields, key) && _.has(entry.fields[key], "en-US")) {
                        var entryFieldsObject = entry.fields[key]["en-US"];
                        if (_.isArray(entryFieldsObject)) {
                            entryFieldsObject.forEach(ent => {
                                if (ent && _.has(ent, "fields")) {
                                    updateEntryWithSysNodeOnly(ent);
                                }
                            });
                        } else if (_.has(entryFieldsObject, "fields")) {
                            updateEntryWithSysNodeOnly(entry.fields[key]["en-US"]);
                        }
                    }
                });
            }
        };
    }]).filter("unsafe", ["$sce", function ($sce) {
        return function (val) {
            return $sce.trustAsHtml(val);
        };
    }]);
});