// Define the 'module' for the enterprise framework API in case of name conflicts.
//noinspection JSUnusedAssignment
var md = md || {};
md.mes_dashboard = function () {
  'use strict';
  var scanEchoDivID = 'scanText';                 // The DIV ID for the 'echo' of the scan text

  var scanData = '';                              // Holds the current scan data.

  var scanParamsToAdd = '';                       // Parameters to add to the scan request.  Mainly for debug/test.

  //noinspection JSUnusedGlobalSymbols
  return {
    // Internal API functions. Not to be used directly by application pages.
    //
    // Add a single message to the given finished map.
    _addMessageToMap: function (finishedMap, messageMap) {
      // Adds a single message of the right type to the given Map as input for finished()
      if (messageMap.level == 'error') {
        if (finishedMap.error == undefined) {
          finishedMap.error = [];
        }
        finishedMap.error[finishedMap.error.length] = messageMap.text;
      } else if (messageMap.level == 'warn') {
        if (finishedMap.warn == undefined) {
          finishedMap.warn = [];
        }
        finishedMap.warn[finishedMap.warn.length] = messageMap.text;
      } else if (messageMap.level == 'info') {
        if (finishedMap.info == undefined) {
          finishedMap.info = [];
        }
        finishedMap.info[finishedMap.info.length] = messageMap.text;
      }
    },
    // Builds a display message map from an Ajax request.  The map can be passed to displayMessage().
    _buildDisplayMessageMap: function (json, divID) {
      var map;
      map = {};
      // Check top-level message first.
      eframe._addMessageToMap(map, json.message);
      // Now, check the other messages (if any)
      var moreMessages = json.message.moreMessages;
      if (moreMessages) {
        for (var i = 0; i < moreMessages.length; i++) {
          eframe._addMessageToMap(map, moreMessages[i]);
        }
      }
      if (divID != undefined) {
        map.divID = divID;
      }
      return map;
    },
    // Builds the ScanRequest object, based on the current state and the scanned
    _buildScanRequest: function (idToResolve) {
      var requestContent = {barcode: idToResolve};
      // Add any scan parameters.  Typically includes current order.
      var extraParams = this._getProvidedScanParamsFromActivities();
      for (var i = 0; i < extraParams.length; i++) {
        var map = extraParams[i];
        //console.log(map);
        Object.assign(requestContent, map);
      }
      //console.log(requestContent);
      return requestContent;
    },
    // Checks for messages and displays them.
    _displayScanMessages: function (messageHolder) {
      //console.log(messageHolder);
      if (messageHolder != undefined) {
        if (messageHolder.message.text == null) {
          // No message.
          return;
        }
        var messages = eframe._convertMessageHolder(messageHolder);
        if (messages.length > 0) {
          eframe.displayMessage(messages);
        }
      }
    },
    // Gets an array of maps from each loaded activity (panel) that provides a scan parameter.
    // This is used to find provideScanParameters from the activities.
    _getProvidedScanParamsFromActivities: function () {
      var extraParams = [];
      var variables = dashboard._getActivePanelVariables();
      for (var i = 0; i < variables.length; i++) {
        // See if the activity has a provideScanParameters() method.
        var fn = variables[i].provideScanParameters;
        if (typeof fn === 'function') {
          // Call the method dynamically.
          var params = fn();
          if (params != undefined) {
            extraParams[extraParams.length] = params;
          }
          JL().trace('scan params panel=' + i + ', values=' + params);
        }
      }
      return extraParams;
    },
    //
    // Handles the key press event for the entire document to support the scanner.
    _handleScanKeyPress: function (event) {
      var targetName = event.target.tagName.toLowerCase();
      if (targetName != "input") {
        // Keyboard input is not in an input field, so we can assume it was a scan.
        var key = mes_dashboard._resolveKey(event);
        var div = document.getElementById(scanEchoDivID);
        div.insertAdjacentHTML('beforeend', key);
        scanData = scanData + key;
      }
    },
    // Handles the key down event, which is triggered for the Tab key.
    _handleScanKeyDown: function (event) {
      if (event.which == 9 && scanData.length > 0) {
        //console.log('Scanned: ' + scanData);
        var scanToResolve = scanData;
        document.getElementById(scanEchoDivID).innerHTML = "";
        scanData = '';
        event.preventDefault();  // Eat the tab in this case
        mes_dashboard._handleScan(scanToResolve);
      }
    },
    // Handles the scan of an ID.
    _handleScan: function (idToResolve) {
      // Clear any displayed messages.
      eframe.clearMessages(dashboard.DIV_MESSAGES);
      var resolveIDRequest = mes_dashboard._buildScanRequest(idToResolve);
      var url = "/scan/scan" + scanParamsToAdd;
      //console.log(scanParamsToAdd);
      //console.log(resolveIDRequest);
      ef.post(url, resolveIDRequest, function (responseText) {
        mes_dashboard._handleScanResponse(JSON.parse(responseText));
      });
      /*
            ef.post({
              uri: url,
              type: 'post',
              postData: resolveIDRequest
            }).done(function (responseText, status, xhr) {
              mes_dashboard._handleScanResponse(xhr.responseJSON);
            }).error(function (xhr, status, statusText) {
              eframe._criticalError('Failed:' + url + ' with status ' + statusText + ' (' + xhr.status + ').');
            });
      */
    },
    // Handles the scan response.
    _handleScanResponse: function (response) {
      //console.log(response);
      if (ef._isPlainObject(response)) {
        //console.log(response);
        var scanResponse = response.scanResponse;
        if (scanResponse == undefined) {
          return;
        }
        // Check for messages to be displayed
        var messageHolder = scanResponse.messageHolder;
        mes_dashboard._displayScanMessages(messageHolder);
        // Handle un-resolved scenarios
        if (!scanResponse.resolved) {
          var msg = eframe.lookup('scanDashboard.couldNotFind.message', scanResponse.barcode);
          eframe.displayMessage({error: msg, divID: dashboard.DIV_MESSAGES});
        }
        // Check for client actions that need to be handled on the client.
        var scanActions = scanResponse.scanActions;
        if (scanActions != undefined && scanActions.length > 0) {
          for (var i = 0; i < scanActions.length; i++) {
            if (scanActions[i].type == 'BUTTON_PRESS') {
              if (scanActions[i].button == '_UNDO') {
                dashboard.undoAction();
              } else {
                dashboard.clickButton(scanActions[i].button);
              }
            } else {
              // Everything else, just send as an event.
              dashboard.sendEvent(scanActions[i]);
            }
          }
        }
        // Check for any undo actions
        dashboard.checkForUndoActions(scanResponse)
      } else {
        if (eframe.checkAndDisplayMessages(response, dashboard.DIV_MESSAGES)) {
          console.log(response);
        }
      }
    },
    // Initialize the scan logic.
    _initScanHandlers: function () {
      document.addEventListener("keypress", mes_dashboard._handleScanKeyPress);
      document.addEventListener("keydown", mes_dashboard._handleScanKeyDown);
    },
    // Determines the key pressed.
    // Selenium does strange things with key codes, so this will resolve the keyCode if needed.
    _resolveKey: function (event) {
      var key = event.key;
      if (key == undefined || key == 'Unidentified') {
        // Should only be needed in automated GUI tests.
        key = String.fromCharCode(event.which);
      }
      return key;
    },
    // Debugging flag to add additional paramters to the scan request.
    _setParamsToAdd: function (flags) {
      scanParamsToAdd = flags;
    }
  }
}();
var mes_dashboard = md.mes_dashboard;  // Simplified variable for access to dashboard API.
// tests needed
// test order change
// test LSN change
// test reverse start
