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
  // TODO: DashboardJS GUI Specs (6 Dashboard, 4 editor)
  /*
   Button - Done
   Event - Done
   Finished - Done
   Form - Done
   Undo - Done
   EditorButton
   Editor
   EditorPanel
   EditorUnsaved
   */

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

  var eventStack = [];  // A list of the last 8 events.  Used for testing only.

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

      if (json.hasOwnProperty('undoActions')) {
        for (var k = 0; k < json.undoActions.length; k++) {
          undoActionList.push(json.undoActions[k]);
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

      // Clear the messages before we start the first activities
      ef.clearMessages();

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
      ef.postAjaxForm(formOrData, url, null,
        function (response) {
          dashboard.finished(panel);
          var res = JSON.parse(response);
          if (ef._isMessage(res)) {
            ef.displayMessage(res);
          }
          dashboard.checkForUndoActions(res);

          // TODO: Support options.otherData
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
      JL().trace(event);
      for (var p in panels) {
        // See if the activity has a handleEvent() function.
        var sharedVarName = '_' + p;
        var fn = window[sharedVarName].handleEvent;
        if (typeof fn === 'function') {
          JL().trace('handler function:' + fn);
          fn(event);
        }
      }
      // Now, Push the event into the stack for testing purposes.
      eventStack.push(event);
      if (eventStack.length > 4) {
        eventStack.shift();
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
    // Adds the buttons to the given div, if the page has the right ID in it.
    // if buttonsToDisplay is undefined, then use the current config.
    // clickScript - The script to handle the click. Default is the dashboard._clickButton().  If provided, then uses this as the onClick handler.
    // doubleClickScript - Optional double-click script handler.
    _addButtonsIfNeeded: function (panelName) {
      var parentViewName = 'Buttons' + panelName;
      var contentViewName = 'ButtonsContent' + panelName;
      var $element = $$(contentViewName);
      if ($element == undefined) {
        return;
      }

      if (buttons) {
        var buttonViews = [];
        for (var i = 0; i < buttons.length; i++) {
          var button = buttons[i];
          buttonViews[0] = {width: tk.pw("20%")};
          var size = button.size || 1.0;
          var css = button.css || '';
          var title = button.title ? button.title : '';
          buttonViews[buttonViews.length] = {
            align: "center,middle", width: tk.pw(button.label.length + "em"), body: {
              view: "button", id: button.id, type: 'form', value: button.label, autowidth: false,
              inputHeight: tk.ph(size + "em"), height: tk.ph(size + "em"), css: css,
              click: 'dashboard._clickButton(' + i + ')', tooltip: title
            }
          };

        }

        // Add spacer before and after the button list.
        buttonViews[buttonViews.length] = {};
        //console.log(buttonViews);
        // Remove current content.
        $$(parentViewName).removeView(contentViewName);
        $$(parentViewName).addView({cols: buttonViews}, 0);
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
    // Test support method to return the current event stack.
    _getEventStack: function () {
      return eventStack;
    },
    // Gets an array of maps from each loaded activity (panel).
    // This is used to find providedParameters from the activities.
    _getExtraParamsFromActivities: function (methodBaseName) {
      var extraParams = [];
      for (var p in panels) {
        // See if the activity has a provideParameters() method.
        var sharedVarName = '_' + p;
        var fn = window[sharedVarName].provideParameters;
        if (typeof fn === 'function') {
          // Call the method dynamically.
          var params = fn();
          if (params != undefined) {
            extraParams[extraParams.length] = params;
          }
          JL().trace('extra params panel=' + p + ', values=' + params);
        }
      }
      return extraParams;
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
    // any values from the first list.
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

      return res;
    },
    // Loads a single page into a panel.
    // panelName - must be a real panel if divInput is not given.
    // uri - The server page that provides the content.
    // extraParams: an array of objects with extra parameters to add to the page load URI (optional).
    _load: function (panelName, url, extraParams) {
      if (url) {
        // TODO: Use to toolkit._getPage() method instead.
        //console.log(' loading '+panelName+' with '+url);  // log.debug candidate
        url = eframe.addArgToURI(url, '_panel', panelName);
        var sharedVarName = '_' + panelName;
        url = eframe.addArgToURI(url, '_variable', sharedVarName);
        url = eframe.addArgToURI(url, '_panelCount', panels.length);

        // Now, add any extra params to the URI.
        var params = dashboard._buildParams(activityParams, extraParams);
        for (var param in params) {
          //console.log(param + ': ' + params[param]);
          url = eframe.addArgToURI(url, param, params[param]);
        }

        nonGUIPanels[panelName] = false;
        window[sharedVarName] = {};  // Clear any previous values from the shared object.
        ef.get(url, {}, function (src) {
          var s = ef._sanitizeJavascript(src);
          if (s.indexOf(sharedVarName + '.display') < 0) {
            nonGUIPanels[panelName] = true;
          }
          try {
            JL().trace(url);
            JL().trace(s);
            var res = eval(s);
          } catch (e) {
            var msg = "Invalid Javascript for dashboard. Error: '" + e.toString() + "' on " + url + '. (Set client.dashboard Trace log level for details).';
            ef.displayMessage({error: msg});
            JL().error(msg);
            JL().info(s);
          }
          //console.log(window[sharedVarName]);
          var content = window[sharedVarName].display;
          if (content) {
            // Remove current content.
            var parentViewName = 'Panel' + panelName;
            var contentViewName = 'Content' + panelName;
            $$(parentViewName).removeView(contentViewName);
            $$(parentViewName).addView({view: 'form', type: "clean", borderless: true, id: contentViewName, margin: 0, rows: [content]}, 0);
            dashboard._addButtonsIfNeeded(panelName);
          }
        });
      }
    },
    _resetToDefaults: function () {
      ef.clearMessages();
      for (var p in panels) {
        this._load(p, panels[p].defaultURL);
      }
    },
    _splitterResized: function (element, panel, resizer, vertical) {
      var value = 1;
      var maximum = 1;
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
