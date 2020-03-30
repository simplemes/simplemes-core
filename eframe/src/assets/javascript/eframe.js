/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

// Define the eframe 'module' for the enterprise framework API in case of name conflicts.
// Provides generic javascript functions that are not directly dependent on the GUI toolkit.
// noinspection JSUnusedAssignment JSUnusedGlobalSymbols
var _ef = _ef || {};
_ef.eframe = function () {
  'use strict';

  /**
   * A list of pre-loaded and localized message texts for use in the GUI.  Used with the lookup() method.
   * This is an object, with properties that are the message key (codes) and the value is the localized value.
   */
  var preloadedMessages = {};
  var _messageDiv = 'messages';          // The ID of the DIV to display standard messages in.
  var _pageOptions = {};                 // A map containing the current page options for get/setPageOption().
  var _configActions = [];               // An array of configuration actions for the current page.

  // noinspection
  // noinspection JSUnusedLocalSymbols
  // noinspection JSUnusedGlobalSymbols
  return {
    // Adds a single argument to the given page URI.
    // uri - The URI to add the argument to as a URL parameter.
    // argumentName - The name of the argument.
    // argumentValue - The argument value.
    addArgToURI: function (uri, argumentName, argumentValue) {
      if (uri == undefined || uri.length == 0) {
        return uri;
      }
      var argument = argumentName + '=' + argumentValue;
      if (uri.indexOf('?') > 0) {
        return uri + '&' + argument;
      } else {
        return uri + '?' + argument;
      }
    },
    alert: function (msg) {
      tk._alert(msg);
    },
    clearMessages: function (divID) {
      var id = divID || _messageDiv;
      eframe._getElement(id).innerHTML = '';
    },
    // Closes a dialog.  See toolkit for details.
    closeDialog: function (dialogID) {
      return tk._closeDialog(dialogID);
    },
    // Executes the given function after the given delay.  The delay defaults to 500ms.
    delay: function (func, delay) {
      if (delay == undefined) {
        delay = 500;
      }
      return setTimeout(func, delay);
    },
    // Displays a dialog.  See toolkit for details.
    displayDialog: function (dialogMap) {
      return tk._displayDialog(dialogMap);
    },
    // Displays one or more messages in the standard message area (_messageDiv).
    displayMessage: function (msg) {
      var msgs;  // A list of maps.
      //console.log(msg);
      // Handle various inputs and convert to a list of maps (plain object).
      if (ef._isPlainObject(msg)) {
        // Input is a single map.
        msgs = ef._convertMessageHolder(msg);
      } else if (ef._isArray(msg)) {
        msgs = msg;
      } else {
        // Assume a simple string.
        msgs = [{"info": msg}];
      }

      // Figure out the options from the first map (maybe).
      var options = {};
      if (ef._isPlainObject(msgs[0])) {
        options = msgs[0]
      }
      //console.log(options);
      var divID = options.divID || msg.divID || _messageDiv;
      //console.log(divID+":"+msg.divID);

      var msgElement = ef._getElement(divID);
      if (msgElement == undefined) {
        // Fallback to the console if not message area found.
        console.log(msg);
        return;
      }
      if (options.clear) {
        eframe.clearMessages(divID)
      }

      // Dump each message
      for (var i = 0; i < msgs.length; i++) {
        var oneMsg = msgs[i];
        if (!ef._isPlainObject(oneMsg)) {
          // Make sure it is a map.
          oneMsg = {info: oneMsg};
        }
        for (var key in oneMsg) {
          if (key == 'error' || key == 'warn' || key == 'warning' || key == 'info') {
            var values = oneMsg[key];
            if (typeof (values) == 'string') {
              // A single string, make it a list.
              values = [values];
            }
            var msgClass = 'info-message';
            if (key == 'error') {
              msgClass = 'error-message';
            } else if (key == 'warn' || key == 'warning') {
              msgClass = 'warning-message';
            }
            for (var j = 0; j < values.length; j++) {
              msgElement.insertAdjacentHTML("beforeend", '<div class="message ' + msgClass + '" >' + ef._encodeHTML(values[j]) + '</div>');
            }
          }
        }
      }
    },
    // Simplified dialog for question dialogs.
    displayQuestionDialog: function (dialogMap) {
      // Check input types.
      if (!ef._isPlainObject(dialogMap)) {
        this._criticalError('displayQuestionDialog() called with invalid argument ' + dialogMap);
        return;
      }
      if (this._checkMissing(dialogMap.question, 'question', 'displayQuestionDialog()')) {
        return;
      }

      if (dialogMap.buttons == undefined) {
        // Build the default buttons
        if (this._checkMissing(dialogMap.ok, 'ok', 'displayQuestionDialog()')) {
          return;
        }
        dialogMap.buttons = ['ok', 'cancel'];
      }

      dialogMap.body = '<div style="text-align:center;"><br><span id="QuestionText">' + dialogMap.question + '</span><br><br>';

      if (dialogMap.width == undefined && dialogMap.height == undefined) {
        // Default question dialogs smaller than Std dialogs
        dialogMap.width = '30%';
        dialogMap.height = '30%';
      }

      return this.displayDialog(dialogMap);
    },
    // Displays a dialog with a text field.  See toolkit for details.
    displayTextFieldDialog: function (dialogMap) {
      return tk._displayTextFieldDialog(dialogMap);
    },
    // Finds the given field and sets focus on it.  Will also select the field if it has a select() method
    focus: function (fieldName) {
      tk.focus(fieldName);
    },
    // Sends an AJAX GET request to the server and returns the results as a JSON object.
    get: function (url, args, success) {
      var uri = url;
      if (args) {
        for (var argName in args) {
          if (args.hasOwnProperty(argName)) {
            uri = ef.addArgToURI(uri, argName, args[argName]);
          }
        }
      }

      var xhr = new XMLHttpRequest();
      xhr.open('GET', uri, true);
      xhr.onerror = function () {
        console.log("error" + xhr.status)
      };
      xhr.onload = function () {
        if (ef._isStatusOk(xhr.status)) {
          if (success) {
            success(xhr.responseText);
          }
        } else {
          // Bad status, so log it to GUI.
          var msg = "Server Request (GET) at '" + uri + "' failed with status " + xhr.statusText + " (" + xhr.status + ")";
          ef.displayMessage({error: msg});
        }
      };
      xhr.onerror = function () {
        var msg = "Server Request (GET) to '" + uri + "' failed with a network error. See browser console for details. ";
        ef.displayMessage({error: msg});
      };
      xhr.setRequestHeader('Cache-Control', 'no-cache');  // Never cache these.
      xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
      xhr.setRequestHeader('Accept', 'application/json');
      xhr.send();
      return xhr;
    },
    // Gets a page-level option.
    getPageOption: function (name, defaultValue) {
      return _pageOptions[name] || defaultValue;
    },
    // Loads all of the dialog preferences for the current page and caches them for later use.
    loadDialogPreferences: function () {
      tk._loadDialogPreferences();
    },
    // Looks up the given code for localized values.  All localized text comes from messages.properties.
    // Supports variable number of arguments.
    lookup: function (code) {
      var s = preloadedMessages[code];
      if (s == undefined) {
        s = code;
      }
      // Convert to an object(map) with the index as the 'key'.
      var params = {};
      //console.log(arguments);
      var parameterIndex = 0;
      if (arguments.length > 1) {
        for (var j = 1; j < arguments.length; j++) {
          var arg = arguments[j];
          var paramKey = parameterIndex.toString();
          if (ef._isArray(arg)) {
            // Add all array members
            for (var k = 0; k < arg.length; k++) {
              paramKey = parameterIndex.toString();
              params[paramKey] = arg[k];
              parameterIndex++;
            }
          } else if (ef._isNumber(arg)) {
            var decimalString = preloadedMessages['_decimal_'];
            if (decimalString == undefined) {
              decimalString = '.';
            }
            arg = arg.toString().replace(/\./g, decimalString);
            params[paramKey] = arg;
            parameterIndex++;
          } else {
            // Just store in the params array
            params[paramKey] = arg;
            parameterIndex++;
          }
        }
      }
      // Replace any parameters.
      for (var i in params) {
        if (params.hasOwnProperty(i)) {
          s = s.replace(new RegExp(('{' + i + '}').replace(/[.^$*+?()[{|]/g, '\\$&'), 'g'), params[i]);
        }
      }
      return s;
    },
    // Sends an AJAX POST request to the server as a JSON body.
    post: function (url, data, success, options) {
      var body = data;
      if (ef._isPlainObject(data)) {
        body = JSON.stringify(data);
      }

      var xhr = new XMLHttpRequest();
      xhr.open('POST', url, true);

      xhr.onload = function () {
        if (ef._isStatusOk(xhr.status)) {
          if (success) {
            success(xhr.responseText);
          }
        } else {
          // Bad status, so log it to GUI.
          var msg = "Server (POST: '" + url + "') failed with status " + xhr.statusText + " (" + xhr.status + ")";
          if (xhr.responseText != undefined) {
            var errorText = ef._extractErrorMsg(xhr.responseText);
            if (errorText) {
              // Has a standard message format content, so use it.
              msg = errorText + ". " + msg + '.';
            }
          }
          var msgOptions = {error: msg};
          if (options) {
            if (options.divID) {
              msgOptions.divID = options.divID;
            }
          }
          ef.displayMessage(msgOptions);
        }
      };
      xhr.onerror = function () {
        var msg = "Server Request (POST) to '" + url + "' failed with a network error. See browser console for details. ";
        ef.displayMessage({error: msg});
      };
      xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
      xhr.setRequestHeader('Content-Type', 'application/json');
      xhr.setRequestHeader('Accept', 'application/json');
      xhr.send(body);
      console.log('POST to ' + url + ': ' + body);
      return xhr;
    },
    // Sends an AJAX POST request to the server as a JSON body.  Uses the fields from the given form.
    postAjaxForm: function (formOrData, url, otherData, success, options) {
      var values;
      if (ef._isPlainObject(formOrData)) {
        values = formOrData;
      } else {
        values = $$(formOrData).getValues();
      }
      var data = Object.assign({}, values, otherData);
      ef.post(url, data, success, options);
    },
    // Sets a page-level option.
    setPageOption: function (name, value) {
      _pageOptions[name] = value;
    },
    // Submits a toolkit form as a normal page.
    submitForm: function (formID, url, otherData) {
      var values = {};
      if (formID) {
        values = $$(formID).getValues();
      }
      if (otherData) {
        Object.assign(values, otherData);
      }
      webix.send(url, values);
    },
    // *************************************
    // Internal Function
    // *************************************
    // Placeholder
    _addPreloadedMessages: function (messages) {
      for (var i = 0; i < messages.length; i++) {
        // Add the message as a new name/value pair in the preloadedMessages object.
        ef._extendObject(preloadedMessages, messages[i]);
      }
    },
    _checkMissing: function (arg, name, caller) {
      if (arg == undefined) {
        this._criticalError("Argument '" + name + "' missing in call to " + caller);
        return true;
      }
      return false;
    },
    // Converts the given MessageHolder (in Javascript Object) to a map suitable for displayMessage use.
    _convertMessageHolder: function (msg) {
      if (msg.message == undefined) {
        return [msg];
      }
      var messages = [];
      // Add top-level message to the array.
      var level = msg.message.level;
      var obj = {};
      obj[level] = msg.message.text;
      if (msg.divID) {
        obj.divID = msg.divID;
      }
      messages[messages.length] = obj;

      // Now, add the other messages.
      for (var i = 0; i < msg.message.otherMessages.length; i++) {
        level = msg.message.otherMessages[i].level;
        obj = {};
        obj[level] = msg.message.otherMessages[i].text;
        messages[messages.length] = obj;
      }

      return messages;
    },
    _criticalError: function (error) {
      //console.log('Critical error:' + error);
      eframe.displayMessage({error: error});
      //JL().error(error);
    },
    // Encodes HTML for display.  Prevents XSS attacks.
    _encodeHTML: function (str) {
      var temp = document.createElement('div');
      temp.textContent = str;
      return temp.innerHTML;
    },
    // Extends object1 with fields from object2.
    _extendObject: function (object1, object2) {
      for (var fieldName in object2) {
        if (object2.hasOwnProperty(fieldName)) {
          object1[fieldName] = object2[fieldName];
        }
      }
    },
    // Extracts the error text message, if the given response is a valid message holder with an error.
    _extractErrorMsg: function (jsonResponse) {
      if (jsonResponse.length < 1) {
        return undefined;
      }
      if (jsonResponse[0] == '{' && jsonResponse.indexOf('"message"') >= 0) {
        try {
          var msg = JSON.parse(jsonResponse);
          var message = msg.message;
          if (message) {
            return message.text;
          }
        } catch (e) {
          // Ignore any parse/etc errors
        }
      }
      return undefined;
    },
    // Gets the HTML element by its HTML ID/
    _getElement: function (id) {
      return document.getElementById(id);
    },
    // Gets the inner HTML to the given element.
    _getInnerHTML: function (id, text) {
      var element = document.getElementById(id);
      return element.innerHTML;
    },
    // Gets the member of an object, safely.  Returns '' if not found.  Can get subField too.
    _getMemberSafely: function (obj, field, subField) {
      if (typeof obj === "undefined") {
        return '';
      }
      var o = obj[field] || '';
      if (o == '') {
        return o;
      }
      if (typeof subField === "undefined") {
        return o;
      }
      return o[subField] || '';
    },
    // Returns the current path, minus the object ID if the last element is a numeric.
    _getURIPath: function () {
      var uri = window.location.pathname;
      var loc = uri.lastIndexOf('/');
      if (loc > 0 && loc < (uri.length - 1)) {
        var lastElement = uri.substring(loc + 1);
        if (ef._isNumber(lastElement)) {
          uri = uri.substring(0, loc);
        }
      }

      return uri;
    },
    // Returns true if the given object has the given function.
    _hasFunction: function (obj, functionName) {
      var t = typeof obj[functionName];
      return t === 'function';
    },
    // Returns true if the given variable is an array.
    _isArray: function (n) {
      return Array.isArray(n);
    },
    // Returns true the given object is a standard error message object.
    // Returns false if not a message or not the error level.
    _isErrorMessage: function (object) {
      if (object.message == undefined) {
        return false;
      }
      return object.message.level == 'error';
    },
    // Returns true the given object is a standard message object.
    _isMessage: function (object) {
      return (object.message != undefined);
    },
    // Returns true if the given variable is a number.  return Object.prototype.toString.call(obj) === '[object Object]';
    _isNumber: function (n) {
      return !isNaN(parseFloat(n)) && isFinite(n);
    },
    // Returns true if the given variable is a plain javascript object (e.g. a map).
    _isPlainObject: function (o) {
      return Object.prototype.toString.call(o) === '[object Object]';
    },
    // Returns true if the given status is one of the 200 series status which mean Ok.
    _isStatusOk: function (status) {
      var i = Number(status);
      return (i >= 200 && i < 300);
    },
    // Returns true if the given variable is a String.
    _isString: function (o) {
      return (typeof (o) == 'string');
    },
    // Registers a configuration button action.
    // This has the elements:
    //   'action' - A function to call when the user clicks the configuration button.
    //   'title' - The text to show to the user, if multiple actions are defined.
    // The function triggerConfigAction() is called when the config button is clicked.
    _registerConfigAction: function (options) {
      _configActions[_configActions.length] = options;
    },
    // Retrieves a given value from local storage for the current page.
    // The internal storage key is made of the current page with the key appended.
    // Can be stored using  _storeLocal().  If the returned value is a string enclosed with brackets ('{}'),
    // then it is assumed to be JSON, which will be parsed into an object.
    // Optional: uri.  If not given, then uses the path (minus the object Id and last element).
    _retrieveLocal: function (key, uri) {
      uri = uri || ef._getURIPath();
      var fullKey = uri + '/' + key;
      var s = localStorage.getItem(fullKey);

      if (s != null) {
        if (s.substr(0, 1) == '{' && s.substr((s.length - 1), 1) == '}') {
          s = JSON.parse(s);
        }
      }
      return s;
    },
    // Fixes common issues with evaluating the Javascript for evaluating some script.
    // Remove <script></script> tags.
    _sanitizeJavascript: function (js) {
      js = js.replace(/<script>/g, "");
      js = js.replace(/<\/script>/g, "");
      return js;
    },
    // Sets the inner HTML to the given value on the element.
    _setInnerHTML: function (id, text) {
      var element = document.getElementById(id);
      element.innerHTML = text;
    },
    // Stores a given value in local storage for the current page.
    // The internal storage key is made of the current page with the key appended.
    // Can be retrieved using  _retrieveLocal().
    // If the value is a javascript object ( see _isPlainObject), then it will be converted to a JSON string.
    // Optional: uri.  If not given, then uses the path (minus the object Id and last element).
    _storeLocal: function (key, value, uri) {
      uri = uri || ef._getURIPath();
      var fullKey = uri + '/' + key;
      if (ef._isPlainObject(value)) {
        value = JSON.stringify(value);
      }
      localStorage.setItem(fullKey, value)
    },
    // Triggers the configuration button action.
    // This has the elements:
    //   'action' - A function to call when the user clicks the configuration button.
    //   'title' - The text to show to the user, if multiple actions are defined.
    _triggerConfigAction: function () {
      if (_configActions.length == 1) {
        _configActions[0].action();
      } else if (_configActions.length > 1) {
        ef.alert('Too many configuration actions for this page.');
      } else {
        ef.alert('No configuration actions for this page.');
      }
    }
  }
}();
var ef = _ef.eframe;  // Shorthand
var eframe = _ef.eframe;  // Simplified variable for access to eframe API.

