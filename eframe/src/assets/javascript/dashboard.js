/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

// Define the dashboard 'module' for the enterprise framework Dashboard display library.
// noinspection JSUnusedAssignment JSUnusedGlobalSymbols
/*
 * Support functions for the client-side eframe dashboard API.
 * See Dashboard section of <a href="http://simplemes.org/doc/latest/guide/single.html">User's Guide</a> for details this API.
 *
 */

// Define the 'module' for the enterprise framework dashboard API in case of name conflicts.
// Allows access to the dashboard as: 'dashboard.finished()' or 'ef.dashboard.finished()'
var ef = ef || {};
ef.dashboard = function () {

  var panels;               // An array of maps, one for each panel. Map elements: defaultURL.  The key is the panel (name).
  var pendingPages;        // A map of arrays of maps that define pages pending for each panel (map elements: url).  The key is the panel (name).
  var pendingExtraParams;  // An arrays of maps contains extra parameters needed for new activities.
  var buttons;             // An Array of Maps with elements that match the DashboardButton fields: [label:'',..,activities[ [url:'page1', panel: 'A'] ]]
  var activityParams = {}; // A map of additional params to send to each activity when loaded.
  var nonGUIPanels = [];   // An array of panels that are currently executing non-GUI activities.  Index is the panelName.
  //noinspection JSUnusedLocalSymbols
  var currentDashboard; // The name of the current dashboard.
  //noinspection JSUnusedLocalSymbols
  var currentCategory;  // The current dashboard category.

  var undoActionStackSize = 8;  // max size of the undo action stack.
  var undoActionStack = [];  // The stack of undo action lists.  Each entry is a list of undoActions

  var eventStack = [];          // A list of the last 8 events.  Used for testing only.
  var cachedActivities = {};    // The cached activities that don't need to be reloaded from the server.
  var displacedActivities = {}; // A holder for temporarily displaced activities that are displaced by non-GUI activities.
  var loadingPanels = 0;        // The current number of panels being loaded.

  // Constants
  var ATTRIBUTE_PANEL_INDEX = 'panel-index';      // The attribute name of the panel index (name)


  // noinspection
  // noinspection JSUnusedLocalSymbols
  // noinspection JSUnusedGlobalSymbols
  return {
    checkForUndoActions: function (json) {
      if (json == undefined) {
        return;
      }

      // Find a plain list of undoActions, depending on format of the input json.
      var undoActionList = [];

      // Handle simple object or array as input.
      if (ef._isPlainObject(json)) {
        // Convert to array
        json = [json];
      }

      for (var i = 0; i < json.length; i++) {
        var row = json[i];
        JL().trace(["checkForUndoActions():", row]);
        if (row.hasOwnProperty('undoActions')) {
          for (var k = 0; k < row.undoActions.length; k++) {
            undoActionList.push(row.undoActions[k]);
          }
        }
      }

      // Now, store for later undo and update the counters/button state.
      if (undoActionList.length > 0) {
        // Enabled the undo button
        dashboard._setUndoButtonState(true);
        // Now, Push the response into the stack.
        undoActionStack.push(undoActionList);
        if (undoActionStack.length > undoActionStackSize) {
          undoActionStack.shift();
        }
      }
      //console.log('stack: '+undoActionStack.length);
    },
    clickButton: function (buttonID) {
      for (var i = 0; i < buttons.length; i++) {
        if (buttonID == buttons[i].id) {
          dashboard._clickButton(i);
        }
      }
    },
    finished: function (arg1) {
      var map;

      // Handle various inputs and convert to consistent form.
      if (ef._isPlainObject(arg1)) {
        // setting a map
        map = arg1;
      } else {
        map = {"panel": arg1}
      }

      // Display any messages.
      for (var key in map) {
        if (key == 'info' || key == 'warn' || key == 'warning' || key == 'error') {
          var msgMap = {};
          msgMap[key] = map[key];
          eframe.displayMessage(msgMap);
        }
      }
      var panelName = map.panel;
      if (!this._validatePanelIndex(panelName, 'finished()')) {
        return;
      }

      // Check for a cancel flag.
      if (map.cancel == true) {
        //noinspection JSUnresolvedVariable,JSUnusedGlobalSymbols
        this._load(panelName, panels[panelName].defaultURL);
        pendingPages[panelName] = null;
        return;
      }

      // Now, display the next activity
      // Check for any pending pages to be displayed next.
      var pendingList = pendingPages[panelName];
      if (pendingList && pendingList.length > 0) {
        // load the next page.
        this._load(panelName, pendingList[0].url, pendingExtraParams);
        // and remove it from the array.
        pendingList.shift();
      } else {
        // Nothing pending, so return to default page if not already displayed.
        var url = panels[panelName].defaultURL;
        if (!(nonGUIPanels[panelName])) {
          // Only load the default if this is a GUI activity.
          // Current content in the panel is not the default, so re-load it.
          //noinspection JSUnresolvedVariable,JSUnusedGlobalSymbols
          this._load(panelName, url);
        }
      }
    },
    // Gets an array of maps from each loaded activity (panel).
    // This is used to find providedParameters from the activities.
    getCurrentProvidedParameters: function () {
      return dashboard._getExtraParamsFromActivities();
    },
    // Loads one or more pages.  Clear messages before starting.
    // extraParams will contain the list of additional parameters to pass to the loaded page(s).
    load: function (pageMap, extraParams) {
      var maps;

      //console.log(pageMap);
      // Handle various inputs and convert to consistent form (array of maps).
      if (ef._isPlainObject(pageMap)) {
        // Input is a map, so place it in the array
        maps = [pageMap];
      } else if (ef._isArray(pageMap)) {
        // Array of maps
        maps = pageMap;
      } else {
        ef._criticalError('load() called with invalid map ' + pageMap);
        return;
      }
      // Now, combine all of the panel's pages into one entry per panel.
      // This is an array of activity arrays of maps.
      var pending = [];

      for (var i = 0; i < maps.length; i++) {
        var map = maps[i];
        //console.log(map);    // log.debug candidate
        if (map.url) {
          // Add to the list of activities for the panel.
          //noinspection JSDuplicatedDeclaration
          var panelActivities = pending[map.panel];
          //console.log(panelActivities);
          if (panelActivities == undefined) {
            panelActivities = [];
            pending[map.panel] = panelActivities;
          }
          panelActivities[panelActivities.length] = {url: map.url};
          //console.log(map.page);
        }
      }

      //console.log(pending);
      pendingExtraParams = undefined;
      // Now, start the first activities in each panel.
      for (var p in pending) {
        //noinspection JSDuplicatedDeclaration
        var panelActivities2 = pending[p];
        if (panelActivities2 != undefined) {
          this._load(p, panelActivities2[0].url, extraParams);
          // Now, add any remaining elements to the pending area.
          pendingPages[p] = [];
          for (var k = 1; k < panelActivities2.length; k++) {
            pendingPages[p][k - 1] = panelActivities2[k];
          }
          if (panelActivities2.length > 1) {
            // Make sure we have any extra params stored for any later pending activities.
            pendingExtraParams = extraParams;
          }
        }
      }
    },
    postActivity: function (formOrData, url, panel, options) {
      // Support options.otherData
      if (options != undefined) {
        let map = options.otherData;
        if (map) {
          for (var key in map) {
            url = ef.addArgToURI(url, key, map[key]);
          }
        }
      }
      ef.postAjaxForm(formOrData, url, null,
        function (response) {
          dashboard.finished(panel);
          var res = JSON.parse(response);
          if (ef._isMessage(res)) {
            ef.displayMessage(res);
          }
          dashboard.checkForUndoActions(res);

          // Call the success function.
          if (options != undefined) {
            if (options.success) {
              options.success(response);
            }
          }
        });
    },
    // Sends an event to all panel activities.  Event has only one require field 'type'.
    sendEvent: function (event) {
      JL().debug("sendEvent(): Sending event: " + JSON.stringify(event));
      var variables = dashboard._getActivePanelVariables();
      var handledCount = 0;
      for (var i = 0; i < variables.length; i++) {
        // See if the activity has a handleEvent() function.
        var fn = variables[i].handleEvent;
        if (typeof fn === 'function') {
          JL().debug("sendEvent(): Delivering event " + event.type + " to panel " + variables[i]._panel + ". Url:" + variables[i]._url);
          JL().trace('  handler function:' + fn);
          fn(event);
          handledCount++;
        }
      }
      // Now, Push the event into the stack for testing purposes.
      eventStack.push(event);
      if (eventStack.length > 4) {
        eventStack.shift();
      }
      if (handledCount > 0) {
        JL().debug("sendEvent(): Sent event to " + handledCount + " handler functions.");
      }
    },
    // Performs the next undo action available.
    undoAction: function () {
      if (undoActionStack.length > 0) {
        ef.clearMessages();
        var undoActionList = undoActionStack.pop();
        for (var i = 0; i < undoActionList.length; i++) {
          var jsonString = undoActionList[i].json.replace(new RegExp('&quot;', 'g'), '"');
          var infoMsg = undoActionList[i].infoMsg;
          var url = undoActionList[i].uri;
          var successEvents = undoActionList[i].successEvents;
          // Send the undo request.
          ef.post(url, jsonString, function (responseText) {
            var res = JSON.parse(responseText);
            if (ef._isMessage(res)) {
              ef.displayMessage(res);
            }
            // noinspection JSReferencingMutableVariableFromClosure
            var msg = infoMsg;
            if (msg == undefined) {
              msg = 'Undo performed';  // Fallback message.
            }
            eframe.displayMessage({info: msg});
            // Now, publish any success events from the undo action.
            //console.log(successEvents);
            // noinspection JSReferencingMutableVariableFromClosure
            if (successEvents != undefined) {
              for (var k = 0; k < successEvents.length; k++) {
                dashboard.sendEvent(successEvents[k]);
              }
            }
          });
        }
      }
      if (undoActionStack.length <= 0) {
        this._setUndoButtonState(false);
      }
      //console.log('  undo Stack: '+undoActionStack.length);
    },
    // *******************************************************************************
    // Internal API functions. Not to be use by dashboard activities in most cases.
    // *******************************************************************************
    //
    // Adds the buttons to the given panel (if any are defined on the dashboard configuration).
    _addButtonsIfNeeded: function (panelName) {
      dashboard._addButtonsIfNeededInternal(panelName, buttons)
    },
    // Adds the buttons to the given panel, internal version. Used by dashboard and editor.
    _addButtonsIfNeededInternal: function (panelName, allButtons, suffix) {
      if (!suffix) {
        suffix = '';
      }
      var parentViewName = 'Buttons' + suffix + panelName;
      var contentViewName = 'ButtonsContent' + suffix + panelName;
      var $element = $$(contentViewName);
      if ($element == undefined) {
        return;
      }

      if (allButtons) {
        var buttonViews = [];
        for (var i = 0; i < allButtons.length; i++) {
          var button = allButtons[i];
          buttonViews[0] = {width: tk.pw("20%")};
          var size = button.size || 1.0;
          var css = button.css || '';
          var title = button.title ? button.title : '';
          var id = button.id ? button.id : button.buttonID;
          if (suffix) {
            id = id + suffix;
          }
          buttonViews[buttonViews.length] = {
            align: "center,middle", width: tk.pw(button.label.length + "em"), body: {
              view: "button", id: id, type: 'form', value: button.label, autowidth: false,
              inputHeight: tk.ph(size + "em"), height: tk.ph(size + "em"), css: css,
              click: 'dashboard' + suffix + '._clickButton(' + i + ')', tooltip: title
            }
          };
        }

        // Add spacer before and after the button list.
        buttonViews[buttonViews.length] = {};
        //console.log(buttonViews);
        // Remove current content.
        $$(parentViewName).removeView(contentViewName);
        $$(parentViewName).addView({cols: buttonViews, id: contentViewName}, 0);
      }

    },
    // Adds one more parameter to the params to be sent to each activity when it is loaded.
    _addActivityParameter: function (name, value) {
      activityParams[name] = value;
    },
    // Clicks a dashboard button to load the right activities.
    _clickButton: function (buttonIndex) {
      // Get any parameters from current panel contents.
      var extraParams = dashboard._getExtraParamsFromActivities('provideParameters');
      //console.log(extraParams);
      var button = buttons[buttonIndex];
      // Clear the messages before we start the first activities
      ef.clearMessages();

      dashboard.load(button.activities, extraParams);

      dashboard.sendEvent({type: 'BUTTON_PRESSED', button: button});
    },
    // Defines the buttons for this page.
    _defineButtons: function (inputButtons) {
      buttons = inputButtons;
      //console.log(buttons);
    },
    // Defines the panels for this page.
    _definePanelsAndLoad: function (inputPanels) {
      panels = inputPanels;
      pendingPages = [];
      this._resetToDefaults();
    },
    // Returns a list of shared variables for all active parameters.  Includes any temporarily displaced activities.
    _getActivePanelVariables: function () {
      var list = [];
      // Find all currently displayed activities.
      for (var p in panels) {
        var sharedVarName = '_' + p;
        if (window[sharedVarName]) {
          list[list.length] = window[sharedVarName];
        }
      }
      // Now, add any variables from displaced activities too.
      for (var a in displacedActivities) {
        if (displacedActivities[a]) {
          list[list.length] = displacedActivities[a];
        }
      }
      return list;
    },
    // Test support method to return the current event stack.
    _getEventStack: function () {
      return eventStack;
    },
    // Gets an array of maps from each loaded activity (panel).
    // This is used to find providedParameters from the activities.
    _getExtraParamsFromActivities: function () {
      var extraParams = [];
      var variables = dashboard._getActivePanelVariables();
      for (var i = 0; i < variables.length; i++) {
        // See if the activity has a provideParameters() method.
        var fn = variables[i].provideParameters;
        if (typeof fn === 'function') {
          // Call the method dynamically.
          var params = fn();
          if (params != undefined) {
            extraParams[extraParams.length] = params;
          }
          JL().trace('_getExtraParamsFromActivities(): extra params panel=' + variables[i]._panel + ', values=' + params);
        }
      }
      return extraParams;
    },
    // Returns the number of panels still being loaded.  used only for GUI tests.
    _getLoadingPanelCount: function () {
      return loadingPanels;
    },
    // Handles the missing parameter exception dialog display.
    _handleMPEDialog: function (msg) {
      //console.log(msg);
      eframe.displayDialog({
        contentsURL: msg.additionalDataURI,
        width: '50%',
        height: '40%',
        sourceFormID: msg.sourceForm.attr('id'),
        mpeKey: msg.mpeKey,
        autoSubmitSourceForm: true
      });
    },
    // Combines the given params and list of params objects into on Map.  The second list (paramsList) overwrites
    // any values from the first list.  Also adds any parameters from the dashboard's URI that are not already in the list.
    // paramBase - A Map with the basic params.
    // paramsList - An array of Maps with added params.
    _buildParams: function (paramBase, paramsList) {
      //console.log(paramBase);
      //console.log(paramsList);
      var res = {};

      // Start with the base list of params.
      for (var param1 in paramBase) {
        res[param1] = paramBase[param1];
      }

      // Now, add the second list.
      for (var listIndex in paramsList) {
        var params = paramsList[listIndex];
        for (var param in params) {
          res[param] = params[param];
        }
      }

      // Now add any parameters from the dashboard's URI that are not already in the parameters.
      var url = new URL(window.location.href);
      for (const [key, value] of new URLSearchParams(url.search)) {
        if (res[key] == undefined) {
          res[key] = value;
        }
      }

      return res;
    },
    // Loads a single page into a panel.
    // panelName - must be a real panel if divInput is not given.
    // uri - The server page that provides the content.
    // extraParams: an array of objects with extra parameters to add to the page load URI (optional).
    _load: function (panelName, url, extraParams) {
      if (url) {
        // Consider using toolkit._getPage() method instead.  A lot of overlap, but no cache support.
        //console.log(' loading '+panelName+' with '+url);  // log.debug candidate
        url = eframe.addArgToURI(url, '_panel', panelName);
        var sharedVarName = '_' + panelName;
        url = eframe.addArgToURI(url, '_variable', sharedVarName);
        url = eframe.addArgToURI(url, '_pageSrc', window.location.pathname);

        // Now, add any extra params to the URI.
        var params = dashboard._buildParams(activityParams, extraParams);
        for (var param in params) {
          //console.log(param + ': ' + params[param]);
          url = eframe.addArgToURI(url, param, params[param]);
        }

        nonGUIPanels[panelName] = false;
        var originalVariable = window[sharedVarName];  // Save the original value, in case we are about to execute a non-GUI activity.
        var loadFunction = function (src) {
          var s = ef._sanitizeJavascript(src);
          if (s.indexOf(sharedVarName + '.display') < 0) {
            nonGUIPanels[panelName] = true;
          }
          try {
            window[sharedVarName] = {};  // Clear any previous values from the shared object.
            //console.log(url);
            //console.log(s);
            JL().debug('_load() loading:' + url);
            JL().trace('_load(): loading panel contents for panel ' + sharedVarName + ': ' + s);
            var res = eval(s);
            window[sharedVarName]._panel = panelName;
            window[sharedVarName]._url = url;
          } catch (e) {
            var msg = "Invalid Javascript for dashboard. Error: '" + e.toString() + "' on " + url + '. (Set client.dashboard Trace log level for details).';
            ef.displayMessage({error: msg});
            JL().error(msg);
            JL().info(s);
          }

          if (window[sharedVarName].cache) {
            let s1 = dashboard._stringAfter(url, '&_pageSrc=');
            cachedActivities[s1] = s;
            JL().trace('_load(): Caching[' + s1 + ']: ' + s);
          }
          //console.log(window[sharedVarName]);
          var content = window[sharedVarName].display;
          if (content) {
            // Has something to display, so remove current content.
            var parentViewName = 'Panel' + panelName;
            var contentViewName = 'Content' + panelName;
            $$(parentViewName).removeView(contentViewName);
            $$(parentViewName).addView({view: 'form', type: "clean", borderless: true, id: contentViewName, margin: 0, rows: [content]}, 0);
            dashboard._addButtonsIfNeeded(panelName);
            dashboard._runScriptOrFunction(window[sharedVarName].postScript);
          } else {
            // Non-GUI activity.

            // Make sure the original activity can receive events from this non-GUI activity.
            displacedActivities[sharedVarName] = originalVariable;
            try {
              var execute = window[sharedVarName].execute;
              if (execute) {
                JL().debug('_load(): Executing ' + sharedVarName + '.execute()');
                execute();
              }
              dashboard._runScriptOrFunction(window[sharedVarName].postScript);
            } catch (e) {
              var msg2 = "Invalid Javascript for dashboard. Error: '" + e.toString() + "' on " + url + '. (Set client.dashboard Trace log level for details).';
              ef.displayMessage({error: msg2});
              JL().error(msg2);
              JL().info(window[sharedVarName]);
            }
            // We need to restore the panel's original shared data for event handling after the non-GUI activity is executed.
            window[sharedVarName] = originalVariable;
            displacedActivities[sharedVarName] = undefined;
          }
          loadingPanels--;
        };

        var src = cachedActivities[dashboard._stringAfter(url, '&_pageSrc=')];
        if (src) {
          JL().trace('_load(): Using value from cache for url: ' + url);
          //console.log('Using value from cache for url: '+url);
          loadFunction(src);
        } else {
          loadingPanels++;
          ef.get(url, {}, loadFunction);
        }
      }
    },
    _runScriptOrFunction: function (scriptOrFunction) {
      // Runs the given script/function.
      if (scriptOrFunction) {
        if (ef._isString(scriptOrFunction)) {
          eval(scriptOrFunction);
        } else {
          scriptOrFunction();
        }
      }
    },
    _resetToDefaults: function () {
      //ef.clearMessages();
      for (var p in panels) {
        this._load(p, panels[p].defaultURL);
      }
    },
    _splitterResized: function (element, panel, resizer, vertical) {
      var value;
      var maximum;
      if (vertical) {
        value = $$("Panel" + panel).$width;
        maximum = window.innerWidth;
      } else {
        value = $$("Panel" + panel).$height;
        maximum = window.innerHeight;
      }
      if (value > 0 && maximum > 0) {
        var percent = value / maximum * 100.0;
        percent = percent.toFixed(2);
        //console.log(element+"("+resizer+"):"+panel+" size: "+ percent+"("+value+" pixels)"+" vertical:"+vertical);
        var uri = window.location.pathname;
        var data;
        data = {"event": "SplitterResized", "pageURI": uri, "element": element};
        data.size = percent;
        data.resizer = resizer;
        ef.post("/userPreference/guiStateChanged", data);
      }
    },
    // Sets the undo button appearance (css class) for the given state.
    _setUndoButtonState: function (state) {
      var element = document.getElementById("undoButton");
      if (state == true) {
        element.classList.remove("undo-button-disabled");
        element.classList.add("undo-button");
      } else {
        element.classList.remove("undo-button");
        element.classList.add("undo-button-disabled");
      }
    },
    _stringAfter: function (s, stripAfter) {
      // Strips the trailing text after the given text.
      var loc = s.indexOf(stripAfter);
      if (loc > 0) {
        s = s.substring(0, loc + stripAfter.length)
      }
      return s;
    },
    _validatePanelIndex: function (panelName, functionName) {
      var panelIsValid = true;
      if (panelName == undefined) {
        panelIsValid = false;
      }
      if (panels[panelName] == undefined) {
        panelIsValid = false;
      }
      if (!panelIsValid) {
        eframe._criticalError('Dashboard function ' + functionName + ' called with invalid panel "' + panelName + '"');
      }
      return panelIsValid;
    }
  }
}();
var dashboard = ef.dashboard;  // Simplified variable for access to dashboard API.

