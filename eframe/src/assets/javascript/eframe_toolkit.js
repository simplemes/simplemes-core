/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

// Define the eframe_toolkit 'module' for the enterprise framework API in case of name conflicts.
// Provides javascript features for use with the GUI toolkit.
// noinspection JSUnusedAssignment JSUnusedGlobalSymbols JSUnusedLocalSymbols JSUnresolvedFunction
var __dialogContentName = '_dialogContent';
var _dialogContent = {};      // Holds dialog content object from server-side page definitions.
// noinspection JSUnusedAssignment
var _ef_tk = _ef_tk || {};
_ef_tk.toolkit = function () {
  'use strict';
  var openAjaxRequests = 0;         // The current number of Ajax request currently open.
  var _dialogPreferences = {};      // Holds the pre-loaded the dialog preferences.
  var _taskMenuString;              // Holds the task menu definition (string form).
  var _taskMenuPopup;               // The popup window that holds the task menu.
  var _ignoreSelectionList = [];    // A list of IDs to ignore on the next selection event in a list.

  // noinspection
  // noinspection JSUnusedLocalSymbols JSUnusedGlobalSymbols JSUnresolvedFunction
  return {
    // Returns the current document loading state.  Will be '' when the document is loaded and Ajax is done.
    docState: function () {
      var docState = (document.readyState == 'complete');
      var ajaxState = (openAjaxRequests <= 0);
      if (ajaxState && docState) {
        return '';
      }
      // Not ready, so return the reason in a string format.
      var reason = '';
      if (!docState) {
        reason = 'state: ' + document.readyState + " ";
      }
      if (!ajaxState) {
        reason = 'ajax: ' + openAjaxRequests;
      }

      return reason;
    },
    findMaxGridValue: function (gridName, columnName) {
      // Find the highest numeric value in the given grid for the given column.
      // If not a numeric column, then returns 0.
      var data = $$(gridName).data;
      if (data.count == 0) {
        return 0;
      }
      var max = 0;
      data.each(function (obj) {
        var value = Number(obj[columnName]);
        if (value > max) {
          max = value;
        }
      });
      return max;
    },
    // Finds the given field and sets focus on it.  Will also select the field if it has a select() method
    focus: function (fieldName) {
      $$(fieldName).focus();
      // Try to select all if there is an input node (e.g. a text field).
      var node = $$(fieldName).getInputNode();
      if (node && ef._hasFunction(node, 'select')) {
        node.select();
      }
    },
    // Calculate the actual height in pixels for the current window height (String, number).
    // heightArg = The height.  Can be a string percent ("10%"), or a number.
    // Supports '3em' also.  Uses the font-size *2.
    ph: function (heightArg) {
      var res = 300;
      if (ef._isNumber(heightArg)) {
        return parseFloat(heightArg);
      }
      if (heightArg != undefined && heightArg.length > 0) {
        if (heightArg.indexOf('%') > 0) {
          var percent = parseInt(heightArg);
          var height = (window.innerHeight * percent) / 100;
          res = height.toFixed(0);
        } else if (heightArg.indexOf('em') > 0) {
          res = parseFloat(heightArg) * tk._getEMSize() * 2;
        } else {
          res = parseInt(heightArg);
        }
      }
      //console.log('_ph: ' + heightArg + ' = ' + res);

      return res;
    },
    // Calculate the actual width in pixels for the current window width (String, number).
    // widthArg = The width.  Can be a string percent ("10%"), or a number.
    // Supports '3em' also.  Uses the font-size * 1.
    pw: function (widthArg) {
      var res = 300;
      if (ef._isNumber(widthArg)) {
        return parseFloat(widthArg);
      }
      if (widthArg != undefined && widthArg.length > 0) {
        if (widthArg.indexOf('%') > 0) {
          var percent = parseFloat(widthArg);
          res = (window.innerWidth * percent) / 100;
        } else if (widthArg.indexOf('em') > 0) {
          res = parseFloat(widthArg) * tk._getEMSize();
        } else {
          res = parseInt(widthArg);
        }
      }

      return res;
    },
    // Refreshes the list, using the current query data and preserving the current selection (if possible).
    refreshList: function (listID, args) {
      let list = $$(listID);
      var url = tk._adjustURLArguments(list.config.url, args);
      var pager = list.getPager();
      if (pager) {
        // Need to add page/sort attributes to the URL manually since the original URL does not have them.
        // Otherwise, the request will be made twice.
        if (pager.data.page > 0) {
          var start = pager.data.page * pager.data.size + 1;
          url = ef.addArgToURI(url, 'start', start);
          url = ef.addArgToURI(url, 'count', pager.data.size);
        }
      }
      // Now, fix the current sort order.  This adds the toolkit-style sorting values since the user may have
      // changed the sort order without a refresh.
      var sortState = list.getState().sort;
      if (sortState) {
        var key = 'sort[' + sortState.id + ']';
        url = ef.addArgToURI(url, key, sortState.dir);
      }
      if (url) {
        //console.log('url:'+url);
        var rowData = list.getSelectedItem();
        //list.clearAll();
        list.load(url, function () {
          if (rowData) {
            var id = rowData.id;
            if (id) {
              var loadedRowData = tk._findListRowData(listID, id);
              if (loadedRowData) {
                tk._ignoreSelectionAdd(id);
                $$(listID).select(id);
              }
            }
          }
        });
      } else {
        console.log('refreshList(): No url defined for ' + listID);
      }
    },
    // *************************************
    // Internal Function
    // *************************************
    // Add the given row object1 to the given formData array, optionally after the given row element.
    _addRowToForm: function (array, object1, afterFieldName) {
      var loc = array.length;
      if (afterFieldName != undefined) {
        var found = tk._findFormRow(array, afterFieldName);
        if (found >= 0) {
          loc = found + 1;
        }
      }
      array.splice(loc, 0, object1);
    },
    // Adjust the given URL to use the given arguments instead of any present on the URL.  argsToAdd is an object (map).
    _adjustURLArguments: function (url, argsToAdd) {
      // First, find all arguments on the URL
      var mainList = url.split('?');
      var path = mainList[0];
      var argString = undefined;
      var args = {};
      if (mainList.length > 1) {
        argString = mainList[1];
        var argList = argString.split('&');
        for (var i = 0; i < argList.length; i++) {
          var oneArgList = argList[i].split('=');
          if (oneArgList.length === 2) {
            args[oneArgList[0]] = oneArgList[1];
          }
        }
      }

      // Now, overwrite/add any arguments from the map passed in
      for (var key in argsToAdd) {
        // skip loop if the property is from prototype
        if (!argsToAdd.hasOwnProperty(key)) {
          continue
        }
        args[key] = argsToAdd[key];
      }

      // Now, rebuild the URL with the (maybe) new parameters.
      var searchParams = "";
      for (var k in args) {
        if (searchParams.length > 0) {
          searchParams += "&";
        }
        searchParams += k + "=" + args[k];
      }
      url = path + "?" + searchParams;

      console.log(args);


      return url;
    },
    _alert: function (msg) {
      webix.message(msg);
    },
    // Builds a standard buttons.
    // Uses the options to find button handlers.
    _buildButton: function (buttonID, handler, dialogID, preCloseFunction) {
      // noinspection JSUnusedLocalSymbols
      return {
        view: "button",
        id: dialogID + '-' + buttonID,
        autowidth: true,
        value: ef.lookup(buttonID + ".label"),
        click: function (id, event) {
          var okToClose = preCloseFunction(dialogID, id);
          if (okToClose == false) {
            return;
          }
          if (handler) {
            // Strip the dialog prefix, if it is there.
            var prefix = dialogID + '-';
            if (id.startsWith(prefix)) {
              id = id.substring(prefix.length);
            }
            okToClose = handler(dialogID, id);
            if (okToClose == false) {
              return;
            }
          }
          $$(dialogID).close();

        }
      };
    },
    // Builds a standard row of centered buttons with spacers for the given list of core button IDs.
    // Uses the options to find button handlers.
    _buildButtonRow: function (buttons, options, dialogID, preCloseFunction) {
      var buttonRow = [{view: "spacer"}];

      for (var i = 0; i < buttons.length; i++) {
        var buttonID = buttons[i];
        var handler = options[buttonID];

        var button = tk._buildButton(buttonID, handler, dialogID, preCloseFunction);
        // Figure out the hot-key for this button.  First = Enter, last = ESC.
        if (buttons.length > 1) {
          // Multiple buttons, then the first button si the 'Enter' hot key
          if (i == 0) {
            button.hotkey = 'enter';
          }
        }
        if (buttons.length > 0) {
          // The last button is always an escape button.
          if (i == (buttons.length - 1)) {
            button.hotkey = 'escape';
          }
        }
        buttonRow[buttonRow.length] = button;
      }
      buttonRow[buttonRow.length] = [{view: "spacer"}];

      return buttonRow;
    },
    // Calculates the percentage height of window width for a given pixel width.
    _calcPercentHeight: function (height) {
      var res = (height / window.innerHeight) * 100.0;
      res = res.toFixed(2);
      return res;
    },
    // Calculates the percentage width of window width for a given pixel width.
    _calcPercentWidth: function (width) {
      var res = (width / window.innerWidth) * 100.0;
      res = res.toFixed(2);
      return res;
    },
    // Tracks opened Ajax requests
    _closeAjax: function () {
      openAjaxRequests--;
      //console.log('Close: '+url+openAjaxRequests+tk.activeAjax());
    },
    // Closes a dialog.  If dialogID is passed in uses it.  Otherwise, close the dialog on top.
    _closeDialog: function (dialogID) {
      if (dialogID == undefined) {
        var dialogs = tk._getOpenDialogs();
        if (dialogs.length < 1) {
          JL().warn('closeDialog(): No dialogs are open');
          return;
        }
        dialogID = dialogs[0];
      }
      $$(dialogID).close();
    },
    // Handles grid field column resize.
    _columnResized: function (id, url, column, newSize) {
      var postData = {};
      postData.event = "ColumnResized";
      postData.pageURI = url;
      postData.element = id;
      postData.column = column;
      postData.newSize = tk._calcPercentWidth(newSize);

      ef.post("/userPreference/guiStateChanged", postData);
    },
    // Handles grid sorting changed by user.
    _columnSorted: function (id, uri, column, direction, defaultSortField) {
      var postData = {};
      postData.event = "ListSorted";
      postData.pageURI = uri;
      postData.element = id;
      postData.defaultSortField = defaultSortField;
      if (column != null) {
        postData["sort"] = column;
        postData["order"] = direction;
      }

      ef.post("/userPreference/guiStateChanged", postData);
    },
    // Internal Toolkit version of displayDialog.  Displays a dialog with the given options.
    // See other types of simple, common dialogs.
    _displayDialog: function (options) {
      if (options.bodyURL) {
        // Must get the body from the server.
        tk._getPage(options.bodyURL, __dialogContentName, function (content) {
          options.body = content;
          options.bodyURL = undefined;
          options.postScript = window[__dialogContentName].postScript;
          tk._displayDialogSuccess(options);
        });

      } else {
        // Can show it immediately.
        tk._displayDialogSuccess(options);
      }
    },
    // Callback function for displaying a dialog.  Used by the AJAX Get success handler when a bodyURL is used.
    _displayDialogSuccess: function (options) {
      if (options.body == undefined && options.bodyURL == undefined) {
        var optionsString = 'unknown options';
        if (options) {
          optionsString = JSON.stringify(options);
        }
        ef._criticalError("eframe_toolkit._displayDialog(): Missing required field body/bodyURL.  Options = " + optionsString);
        return;
      }
      var body = options.body;
      if (typeof options.body === 'string') {
        body = {template: options.body};
      }
      // Determine the next dialog # needed
      var dialogID = 'dialog0';
      var topDialogID = tk._getTopDialogID();
      if (topDialogID) {
        var n = Number(topDialogID.substr(6));
        dialogID = 'dialog' + (n + 1);
      }

      var title = options.title || 'Dialog';
      title = ef.lookup(title);

      // Internal function to trigger the closing of the dialog.  
      var preCloseFunction = function (id, action) {
        //console.log('preClose: ' + id + action);
        var res = true;
        if (options.beforeClose) {
          res = options.beforeClose(dialogID, action);
        }
        return res;
      };
      // Now, add any buttons (if desired).
      var buttons = options.buttons;
      if (buttons == undefined) {
        buttons = ['ok'];
      } else if (buttons.length == 0) {
        buttons = undefined;
      }

      var rows = [];
      rows[0] = body;
      if (buttons != undefined) {
        rows[1] = {
          type: "clean",
          padding: 10,
          margin: 6,
          cols: tk._buildButtonRow(buttons, options, dialogID, preCloseFunction)
        };
      }

      var dialogBody = {
        type: "clean",
        view: "form",
        autoheight: false,
        rows: rows
      };

      // Calculate the default position, using the inputs if given.
      var width = tk.pw(options.width || "50%");
      var height = tk.ph(options.height || "35%");
      var left = tk.pw(options.left || (tk.pw("100%") - width) / 2);
      var top = tk.ph(options.top || (tk.ph("100%") - height) / 2);

      // And let the user preferences override that.
      var preferences = tk._getDialogPreferences();
      var dialogPref = preferences[dialogID];
      if (dialogPref) {
        width = tk.pw(dialogPref.width + "%");
        height = tk.ph(dialogPref.height + "%");
        left = tk.pw(dialogPref.left + "%");
        top = tk.ph(dialogPref.top + "%");
      }
      //console.log("(w,h) - (l,t) = (" + width + ',' + height + ") - (" + left + ',' + top + ") + screen = (" + tk.pw("100%") + ',' + tk.ph("100%") + ')');

      var lastButton = '';
      if (buttons != undefined && buttons.length > 0) {
        lastButton = dialogID + '-' + buttons[buttons.length - 1];
      }
      var focus = options.focus || lastButton;
      //console.log(focus);

      var dialog = webix.ui({
          view: "window",
          //autofit: false,
          id: dialogID,
          //buttons: ["Ok", "Cancel"],
          left: left, top: top, height: height, width: width,
          maxWidth: window.innerWidth, maxHeight: window.innerHeight,
          minWidth: 0, minHeight: 0,
          modal: true,
          move: true,
          resize: true,
          head: {
            view: "toolbar", margin: -4, cols: [
              {view: "label", label: title},
              {
                view: "icon", icon: "wxi-close", click: function () {
                  preCloseFunction(dialogID, 'cancel');
                  $$(dialogID).close();
                }
              }
            ]
          },
          on: {
            'onViewMoveEnd': function () {
              tk._updateDialogState(dialogID);
            },
            'onViewResize': function () {
              tk._updateDialogState(dialogID);
            },
            'onKeyPress': function (code) {
              // A backup ESCAPE handler for those dialogs with no std buttons.
              //alert(code);
              if (code == 27) {
                var res = preCloseFunction(dialogID, 'cancel');
                if (res == false) {
                  return;
                }
                $$(dialogID).close();
              }
            },
            'onShow': function () {
              if (focus) {
                webix.delay(function () {
                  tk.focus(focus);
                })
              }
            }
          },
          //position: "center",
          body: dialogBody
        }
      );
      dialog.show();

      // Now, add a temporary messages div for errors in the dialog.
      var topDialog = tk._getTopDialogBody();
      // Find the first child element
      var theDiv = document.createElement('DIV');
      theDiv.id = dialogID + "Messages";
      topDialog.insertBefore(theDiv, topDialog.children[0]);

      // Call any script to done after the dialog content is rendered
      if (options.postScript) {
        webix.delay(function () {
          if (ef._isString(options.postScript)) {
            eval(options.postScript);
          } else {
            options.postScript();
          }
        })
      }
      return dialogID;
    },
    // Displays a dialog with a single text field.  Calls the ok function when done, passing the text field value.
    // options contains: value, id, label, options from the displayDialog function.
    _displayTextFieldDialog: function (options) {
      var textFieldID = options.fieldID || 'dialogTextField';

      options.body = {
        cols: [
          {template: "", width: tk.pw('5%')},
          {
            view: "text", value: options.value, id: textFieldID, required: true, label: ef.lookup(options.label),
            labelAlign: 'right', inputWidth: tk.pw('40%')
          }
        ]
      };
      options.focus = textFieldID;
      options.buttons = options.buttons || ['ok', 'cancel'];
      // noinspection JSUnusedLocalSymbols
      options.ok = function (dialogID, id) {
        // grab the text value
        var value = $$(textFieldID).getValue();
        if (options.textOk) {
          // And let the caller know what it is.
          return options.textOk(value);
        }
      };
      tk._displayDialog(options);
    },
    // Finds the index in the given formData array that has the given field defined.  -1 if not found.
    _findFormRow: function (array, fieldName) {
      for (var i = 0; i < array.length; i++) {
        var row = array[i];
        if (row.cols != undefined) {
          if (row.cols.length > 1) {
            if (row.cols[1].id == fieldName) {
              return i;
            }
          }
        }
      }
      return -1;
    },
    _findListRowData: function (listID, id) {
      // Find the row data for the given ID from the given list/grid.
      var data = $$(listID).data;
      if (data.count == 0) {
        return undefined;
      }

      var res = undefined;
      data.each(function (obj) {
        //console.log(obj);
        if (obj.id == id) {
          //console.log(' found');
          res = obj;
        }
      });
      return res;
    },
    // Gets the list of open dialogs.  First is the dialog on top.
    _getOpenDialogs: function () {
      var elements = document.getElementsByClassName('webix_window');
      var dialogIDs = [];
      for (var i = 0; i < elements.length; i++) {
        if (elements[i].attributes['view_id'].value.substr(0, 6) == 'dialog') {
          var s = elements[i].attributes['view_id'].value;
          dialogIDs.push(Number(s.substr(6)));
        }
      }
      dialogIDs.reverse();
      var dialogs = [];
      for (i = 0; i < dialogIDs.length; i++) {
        dialogs[i] = 'dialog' + dialogIDs[i];
      }

      return dialogs;
    },
    // Gets the list of open dialogs.  First is the dialog on top.
    _getTopDialogID: function () {
      var list = tk._getOpenDialogs();
      if (list.length > 0) {
        return list[0];
      }
      return undefined;
    },
    // Gets window body div element for the top dialog.
    _getTopDialogBody: function () {
      var dialogID = tk._getTopDialogID();
      var windows = document.getElementsByClassName('webix_window');
      for (var i = 0; i < windows.length; i++) {
        if (windows[i].attributes['view_id'].value == dialogID) {
          return windows[i].getElementsByClassName('webix_win_body')[0];
        }
      }

      return undefined;
    },
    _gridAddRow: function (view, rowData) {
      var ed = view.getEditor();
      if (ed) {
        // Finish any editing.
        view.editStop();
      }

      var max = 0;
      view.eachRow(function (id) {
        if (parseInt(id) > max) {
          max = parseInt(id);
        }
      });
      var id = (max + 1).toString();
      rowData.id = id;
      view.add(rowData);
      view.select(id);
      view.showItem(id);
      tk._gridStartEditing(view);
    },
    _gridRemoveRow: function (view, rowData) {
      var ed = view.getEditor();
      if (ed) {
        // Finish any editing.
        view.editStop();
      }
      var currentId = view.getSelectedId();
      if (currentId) {
        // Try to select the row below, falling back to last row in the list
        var nextId = view.getNextId(currentId);
        view.remove(view.getSelectedId());
        if (nextId == undefined) {
          nextId = view.getLastId();
        }
        if (nextId) {
          view.select(nextId);
        }
      }
    },
    // Handles the Shift-Tab key for a table.  Used to allow tabbing out of a table consistently.
    _gridBackwardTabHandler: function (view) {
      return tk._gridTabHandler(view, -1);
    },
    // Handles the Tab key for a table.  Used to allow tabbing out of a table consistently.
    _gridForwardTabHandler: function (view) {
      return tk._gridTabHandler(view, 1);
    },
    _gridStartEditing: function (view) {
      var ed = view.getEditor();
      if (ed) {
        // Let the default key handler take take of it.
        return;
      }
      var rowId = view.getSelectedId();
      view.editCell(rowId, 0);
      // The keystroke is eaten.
      return false;
    },
    // Handles the Tab key for a table.  Used to allow tabbing out of a table consistently.
    // Direction=1 for forward, -1 for backward.
    _gridTabHandler: function (view, direction) {
      var ed = view.getEditor();
      if (ed) {
        var rowId = ed.row;
        var colId = ed.column;
        var column = view.config.columns[view.getColumnIndex(colId) + direction];
        if (direction == 1 && view.getNextId(rowId) == undefined && column == undefined) {
          view.editStop();
          return true;
        } else if (direction == -1 && view.getPrevId(rowId) == undefined && column == undefined) {
          view.editStop();
          return true;
        }
      } else {
        // No editor, so let some other handler handle it.
        return true;
      }
    },
    // Loads all of the dialog preferences for the current page and caches them for later use.
    _getDialogPreferences: function () {
      return _dialogPreferences;
    },
    // Returns the size of a generic em.
    _getEMSize: function () {
      return parseFloat(getComputedStyle(document.querySelector('body'))['font-size']);
    },
    // Gets page content from the server and executes the javascript to define the page content in the toolkit format (object).
    // url - The server page that provides the content.
    // variable - The name of the variable to load the content into (an object with a .display sub-element).
    // success - The success function to call (passed the content object).
    _getPage: function (url, variable, success) {
      if (url) {
        url = eframe.addArgToURI(url, '_variable', variable);

        // Now, add any extra params to the URI.
        window[variable] = {};  // Clear any previous values from the shared object.
        ef.get(url, {}, function (src) {
          var s = ef._sanitizeJavascript(src);
          try {
            JL().trace(url);
            JL().trace(s);
            var res = eval(s);
          } catch (e) {
            var msg = "Invalid Javascript for page. Error: '" + e.toString() + "' on " + url + '. (Set Trace log level for details).';
            ef.displayMessage({error: msg});
            JL().error(msg);
            JL().info(s);
          }
          var content = window[variable].display;
          //console.log(window[variable]);
          if (success) {
            success(content);
          }
        });
      }
    },
    // The global mouse click handler.  Mostly closes an open task menu.
    _globalClickHandler: function (event) {
      if ($$('_taskMenuButton').getNode().contains(event.target)) {
        // The click is in the menu button, so don't use it to close the menu.
        return true;
      }
      if (event.target.className.indexOf('webix_tree') >= 0) {
        // The click is on the tree open/close in the task menu popup itself, so don't use it to close the menu.
        return true;
      }
      if (_taskMenuPopup != undefined) {
        if (_taskMenuPopup.isVisible()) {
          _taskMenuPopup.hide();
        }
      }
      return true;
    },
    // Returns true if the selection of this element should be ignored.
    // Use _ignoreSelectionAdd(id) to add to the list when programmatically selecting in a list..
    _ignoreSelection: function (id) {
      for (var i = 0; i < _ignoreSelectionList.length; i++) {
        if (_ignoreSelectionList[i] == id) {
          _ignoreSelectionList.slice(i, 1);
          return true;
        }
      }
      return false;
    },
    // The next selection event for this ID will be ignored.  Used for programmatically selecting in a list.
    // Use _ignoreSelection(id) to check for entries in this list.
    _ignoreSelectionAdd: function (id) {
      for (var i = 0; i < _ignoreSelectionList.length; i++) {
        if (_ignoreSelectionList[i] == id) {
          return;
        }
      }
      _ignoreSelectionList.push(id);
    },
    // Loads all of the dialog preferences for the current page and caches them for later use.
    _loadDialogPreferences: function () {
      var uri = window.location.pathname;
      var args;
      args = {pageURI: uri, preferenceType: 'DialogPreference'};
      ef.get("/userPreference/findPreferences", args, function (responseText) {
        var data = JSON.parse(responseText);
        ef._extendObject(_dialogPreferences, data);
      });
    },
    // Tracks opened Ajax requests
    _openAjax: function () {
      openAjaxRequests++;
      //console.log('Open: '+url+openAjaxRequests+tk.activeAjax());
    },
    // Parses an ISO date string into a real date.
    // If the toolkitFlag==true, then the TZ for date/times will be dropped to
    // allow the webix GUI code to display the date/time correctly.
    _parseISODate: function (s, toolkitFlag) {
      // Convert +0000 to Z for IE parsing
      /*
            var loc = s.indexOf('+0000');
            if (loc > 0) {
              s = s.substring(0, s.length - 5);
            }
      */
      if (s.length == 10) {
        // Handle date only methods to avoid TZ offset issues with just dates.
        var fields = s.split('-');
        if (fields.length == 3) {
          var month = Number(fields[1]) - 1;
          return new Date(fields[0], month, fields[2]);
        }
      }
      var tLoc = s.indexOf('T');
      if (tLoc > 0 && toolkitFlag) {
        // This has a time portion, so try to drop the TZ portion to fake the toolkit into displaying the correct value.
        // Find the beginning of the TZ section (starts with -/+).
        var loc = s.indexOf('-', tLoc);
        if (loc < 0) {
          loc = s.indexOf('+', tLoc);
        }
        if (loc > 0) {
          s = s.substring(0, loc);
        }
        //console.log('  loc = '+loc+' tLoc = '+tLoc);
      }
      //console.log(s+' to '+new Date(s)+toolkitFlag);
      return new Date(s);
    },
    // A check box rendered for the grid - non-clickable.
    _readOnlyCheckbox: function (obj, common, value, config) {
      var checked = (value == config.checkValue) ? 'checked="true"' : '';
      return "<input disabled class='webix_table_checkbox' type='checkbox' " + checked + ">";
    },
    // Sets the string form of the task menu.
    _setTaskMenuString: function (s) {
      _taskMenuString = s;
      //console.log(JSON.parse(s));
    },
    // Sets the string form of the task menu.
    _splitterResizeHandler: function (id) {
      console.log(id);
    },
    // Handles state changes in the task menu tree.
    _taskMenuStateChanged: function (treeId) {
      var selectedId = $$(treeId).getSelectedId();
      var openItems = $$(treeId).getOpenItems();
      var state = {};
      state.selected = selectedId;
      state.openIds = [];
      for (var i = 0; i < openItems.length; i++) {
        state.openIds[state.openIds.length] = openItems[i];
      }
      ef._storeLocal(treeId, state, '/common');
    },
    _taskMenuToggle: function () {
      var treeId = "_taskMenuTree";
      if (_taskMenuPopup == undefined) {
        var theData = JSON.parse(_taskMenuString);
        //console.log(theData);
        var state = ef._retrieveLocal(treeId, '/common');
        _taskMenuPopup = webix.ui({
          view: "popup",
          id: "_taskMenuPopup",
          toFront: true,
          top: $$('_taskMenuButton').$height,
          left: $$('_taskMenuButton').$width,
          width: tk.pw("30%"),
          height: tk.ph("90%"),
          body: {
            view: "tree",
            id: treeId,
            select: true,
            on: {
              onAfterSelect: function () {
                tk._taskMenuStateChanged(treeId);
              },
              onAfterOpen: function () {
                tk._taskMenuStateChanged(treeId);
              },
              onAfterClose: function () {
                tk._taskMenuStateChanged(treeId);
              },
              onKeyPress: function (code, event) {
                if (event.key == " " || event.key == "Enter") {
                  var $$tree = $$(treeId);
                  var selectedId = $$tree.getSelectedId();
                  var item = $$tree.getItem(selectedId);
                  if (item != undefined) {
                    if (item.link != undefined) {
                      window.location = item.link;
                    } else if ($$tree.isBranch(selectedId)) {
                      // Toggle entry
                      if ($$tree.isBranchOpen(selectedId)) {
                        $$tree.close(selectedId);
                      } else {
                        $$tree.open(selectedId);
                      }
                    }
                  }
                }
              }
            },
            template: function (obj, common) {
              if (obj.link == undefined) {
                return common.icon(obj, common) + "<span>" + obj.value + "</span>"
              } else {
                return common.icon(obj, common) + "<a href='" + obj.link + "' id='_" + obj.id + "' class='tree-link'>&nbsp;" + obj.value + "&nbsp;</a>"
              }
            },
            data: theData
          }
        });
        if (state != null) {
          //console.log(state.selected);
          $$(treeId).select(state.selected);

          for (var i = 0; i < state.openIds.length; i++) {
            $$(treeId).open(state.openIds[i]);
          }
        }
      }
      if (_taskMenuPopup.isVisible()) {
        _taskMenuPopup.hide();
      } else {
        _taskMenuPopup.show();
        webix.UIManager.setFocus($$(treeId));
      }
    },

    // A handler to update the dialog state in the user preferences.
    _updateDialogState: function (dialogID) {
      var $dialog = $$(dialogID);
      var postData = {};
      postData.event = "DialogChanged";
      postData.pageURI = window.location.pathname;
      postData.element = dialogID;
      postData.width = tk._calcPercentWidth($dialog.$width);
      postData.height = tk._calcPercentHeight($dialog.$height);
      postData.left = tk._calcPercentWidth($dialog.getNode().offsetLeft);
      postData.top = tk._calcPercentHeight($dialog.getNode().offsetTop);

      JL().trace(postData);
      //console.log(postData);
      ef.post("/userPreference/guiStateChanged", postData);
    }
  }
}();
var tk = _ef_tk.toolkit;  // Shorthand
// noinspection JSUnusedGlobalSymbols
var toolkit = _ef_tk.toolkit;  // Simplified variable for access to eframe API.
// noinspection JSUnusedGlobalSymbols
var eframe_toolkit = _ef_tk.toolkit;  // Fallback for name conflicts

// Add a wrapper to monitor currently active AJAX requests.
(function () {
  var _oldOpen = XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.open = function (method, url, async, user, pass) {
    tk._openAjax(url);
    this.addEventListener("readystatechange", function () {
      if (this.readyState == 4) {
        tk._closeAjax(url);
      }
    }, false);
    // Always make async calls.  jsnlog.js seems to use synch calls.
    _oldOpen.call(this, method, url, true, user, pass);
  }
})();

// Attach the key handler to the whole page to trigger the menu.
document.onkeydown = function keyEventHandler(event) {
  if (event.key == "F9") {
    tk._taskMenuToggle();
    event.stopPropagation();
    return false;
  }
};

// Attach the some click handlers to the whole page to close the task menu when not needed.
document.onclick = tk._globalClickHandler;
document.onauxclick = tk._globalClickHandler;

// Extend any Webix components for custom behavior.

// DateOnly editor with optional input via keyboard.
webix.editors.keyboardEditDate = webix.extend({
  render: function () {
    var icon = "<span class='webix_icon wxi-calendar' style='position:absolute; cursor:pointer; top:8px; right:5px;'></span>";
    var node = webix.html.create("div", {
      "class": "webix_dt_editor"
    }, "<input type='text'>" + icon);

    node.childNodes[1].onclick = function () {
      var master = webix.UIManager.getFocus();
      var editor = master.getEditor();

      master.editStop(false);
      var config = master.getColumnConfig(editor.column);
      config.editor = "date";
      master.editCell(editor.row, editor.column);
      config.editor = "keyboardEditDate";
    };
    return node;
  },
  getInputNode: function () {
    return this.node.firstChild;
  },
  getValue: function () {
    var s = this.getInputNode(this.node).value;
    return webix.i18n.dateFormatDate(s);
  },
  setValue: function (value) {
    this.getInputNode(this.node).value = webix.i18n.dateFormatStr(value);
  }
}, webix.editors.text);

// Date editor with optional input via keyboard.
webix.editors.keyboardEditDateTime = webix.extend({
  render: function () {
    var icon = "<span class='webix_icon wxi-calendar' style='position:absolute; cursor:pointer; top:8px; right:5px;'></span>";
    var node = webix.html.create("div", {
      "class": "webix_dt_editor"
    }, "<input type='text'>" + icon);

    node.childNodes[1].onclick = function () {
      var master = webix.UIManager.getFocus();
      var editor = master.getEditor();

      master.editStop(false);
      var config = master.getColumnConfig(editor.column);
      config.editor = "date";
      master.editCell(editor.row, editor.column);
      config.editor = "keyboardEditDateTime";
    };
    return node;
  },
  getInputNode: function () {
    return this.node.firstChild;
  },
  getValue: function () {
    var s = this.getInputNode(this.node).value;
    return webix.i18n.fullDateFormatDate(s);
  },
  setValue: function (value) {
    this.getInputNode(this.node).value = webix.i18n.fullDateFormatStr(value);
  }
}, webix.editors.text);

// A custom webix combobox that supports multiple selection.
webix.protoUI({
  name: "multiComboEF",
  $init: function (config) {
    this.$view.className = "webix_view webix_control webix_el_combo";
    //console.log(config);
    this._options = config.options;
  },
  $setValue: function (value) {
    //console.log(value);
    //webix.ui.combo.prototype.$setValue.apply(this,arguments);
    var node = this.getInputNode();
    var a = value.split(',');
    var res = '';
    for (var i = 0; i < a.length; i++) {
      //console.log(a[i]);
      for (var j = 0; j < this._options.length; j++) {
        if (this._options[j].id == a[i]) {
          if (res.length > 0) {
            res += ",";
          }
          res += this._options[j].value;
          break;
        }
      }

    }
    node.value = res;
  },
  getValue: function (config) {
    // noinspection JSPotentiallyInvalidConstructorUsage
    var res = webix.ui.combo.prototype.getValue.apply(this, arguments);
    var node = this.getInputNode();
    if (node) {
      // Parse the text field's value to make a list of selected IDs
      res = '';
      var v = node.value;
      var a = v.split(',');
      for (var i = 0; i < a.length; i++) {
        //console.log(a[i]);
        for (var j = 0; j < this._options.length; j++) {
          if (this._options[j].value == a[i]) {
            if (res.length > 0) {
              res += ",";
            }
            res += this._options[j].id;
            break;
          }
        }
      }
      //console.log(v);
    }
    //console.log(res);
    return res;
  },
  setValue: function (id) {
    // Adds to the field, if not already in the displayed text.
    var node = this.getInputNode();
    if (node) {
      var res = node.value;
      for (var j = 0; j < this._options.length; j++) {
        if (this._options[j].id == id) {
          if (res.indexOf(this._options[j].value) < 0) {
            if (res.length > 0) {
              res += ",";
            }
            res += this._options[j].value;
            node.value = res;
          }
          break;
        }
      }

    }
    //console.log(node);
  },
  _options: []
}, webix.ui.combo);
