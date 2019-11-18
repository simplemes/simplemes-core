// Define the eframe_definition 'module' for the enterprise framework API.
// This provides functions for definition pages.
// It provides common routines for things like GUI Customization, deleting records, etc.
// noinspection JSUnusedAssignment JSUnusedGlobalSymbols
//
// All functions are internal functions.

// noinspection JSUnusedAssignment
var _efd = _efd || {};
_efd.eframe_definition = function () {
  'use strict';
  var inlineGridNames = [];    // A list of view IDs to be added to the submit.
  var AVAILABLE_LIST_ID = 'available';   // The ID of the available list of fields in the config editor dialog.
  var CONFIGURED_LIST_ID = 'configured'; // The ID of the configured list of fields in the config editor dialog.

  // noinspection
  // noinspection JSUnusedLocalSymbols
  // noinspection JSUnusedGlobalSymbols
  return {
    // Adds the rows from any inline grids 
    _addInlineGridRowsForSubmission: function (values) {
      for (var i = 0; i < inlineGridNames.length; i++) {
        var gridName = inlineGridNames[i];
        $$(gridName).editStop();
        var data = $$(gridName).data;
        if (data.count == 0) {
          return 0;
        }
        var row = 0;
        data.each(function (obj) {
          var name = gridName + '[' + row + '].';
          // Now, put each value from the data row into its own child record.
          for (var fieldName in obj) {
            var submitParameterName = name + fieldName;
            var s;
            if (obj[fieldName] instanceof Date) {
              s = webix.i18n.parseFormatStr(obj[fieldName]);
            } else {
              s = obj[fieldName];
            }
            values[submitParameterName] = s;
          }
          row++;
        });
      }
    },
    // Checks for definition page URL messages to display.  These are params such as _error or _msg.
    _checkURLMessages: function () {
      try {
        var url = new URL(window.location);
        var _info = url.searchParams.get("_info");
        if (_info) {
          ef.displayMessage(_info);
        }
        var _error = url.searchParams.get("_error");
        if (_error) {
          ef.displayMessage({error: _error});
        }
      } catch (e) {
        // Ignore for IE.  This means the messages won't be display.
      }
    },
    // Confirms the delete of a record.  Used in conjunction with the standard definition toolbar for show pages.
    _confirmDelete: function (uri, id, domainName, recordShortString) {
      ef.displayQuestionDialog({
        title: 'delete.confirm.title',
        question: ef.lookup('delete.confirm.message', domainName, recordShortString),
        ok: function () {
          // Build a form to submit.
          var values = {id: id};
          webix.send(uri, values);
        }
      });
    },
    // Submits the save request for the create action.  Marshals the form values and submits them.
    _createSave: function (formName, uri) {
      var values = $$(formName).getValues();
      //console.log(JSON.stringify(values));
      efd._addInlineGridRowsForSubmission(values);
      //console.log(JSON.stringify(values));
      webix.send(uri, values);
    },
    // Handles the cancel dialog button on the field edit/add dialogs.
    _editorFieldCancel: function (obj) {
      ef.closeDialog();
    },
    // Confirms that the user wants to delete the custom field.
    _editorFieldDeleteConfirm: function (obj) {
      // Check configured list first.
      var list = $$(CONFIGURED_LIST_ID);
      var item = list.getSelectedItem();
      var fieldName;
      if (item) {
        fieldName = item.name;
      } else {
        // Check the available list next
        list = $$(AVAILABLE_LIST_ID);
        item = list.getSelectedItem();
        if (item) {
          fieldName = item.name;
        }
      }
      if (fieldName == undefined) {
        //error.135.message=Please select a custom field.
        efd._editorDisplayDialogMessage({warning: ef.lookup('error.135.message')});
        return;
      }
      if (!item.custom || efd._isPanel(fieldName)) {
        //error.135.message=Please select a custom field.
        efd._editorDisplayDialogMessage({warning: ef.lookup('error.135.message')});
        return;
      }

      var divID = tk._getTopDialogID() + 'Messages';


      // if not found, display message.
      ef.displayQuestionDialog({
        title: 'delete.confirm.title',
        question: ef.lookup('delete.confirm.message', ef.lookup('customField.label'), fieldName),
        ok: function () {
          ef.post('/extension/deleteField', JSON.stringify({id: item.recordID}), function (responseText) {
            if (responseText) {
              var res = JSON.parse(responseText);
              if (ef._isErrorMessage(res)) {
                // Display the error in the dialog itself.
                res.divID = divID;
                ef.clearMessages(res.divID);
                ef.displayMessage(res);
              }
            } else {
              // Delete worked, so remove the field from the list.
              console.log(list.data);
              console.log('deleting...' + item.id);
              list.remove(item.id);
            }
          }, {divID: divID});
        }
      });

    },
    // Opens the add panel dialog.
    _editorFieldOpenAddDialog: function () {
      var bodyURL = ef.addArgToURI('/extension/editFieldDialog', 'domainURL', window.location.pathname);
      ef.displayDialog({
        bodyURL: bodyURL,
        title: 'definitionEditor.addField.title',
        width: '90%',
        height: '75%',
        buttons: []
      });
    },
    // Opens the edit panel dialog.
    _editorFieldOpenEditDialog: function (fieldName) {
      if (fieldName == undefined) {
        // Check configured list first.
        var list = $$(CONFIGURED_LIST_ID);
        var item = list.getSelectedItem();
        if (item) {
          fieldName = item.name;
        } else {
          // Check the available list next
          list = $$(AVAILABLE_LIST_ID);
          item = list.getSelectedItem();
          if (item) {
            fieldName = item.name;
          }
        }
        if (fieldName == undefined) {
          //error.135.message=Please select a custom field to edit.
          efd._editorDisplayDialogMessage({warning: ef.lookup('error.135.message')});
          return;
        }
      }
      if (fieldName.substr(0, 6) == 'group:') {
        //error.135.message=Please select a custom field to edit.
        efd._editorDisplayDialogMessage({warning: ef.lookup('error.135.message')});
        return;
      }
      var srcItem = efd._editorFindItemForField(fieldName);
      if (srcItem) {
        if (!srcItem.custom) {
          //error.135.message=Please select a custom field to edit.
          efd._editorDisplayDialogMessage({warning: ef.lookup('error.135.message')});
          return;
        }
      }


      var bodyURL = ef.addArgToURI('/extension/editFieldDialog', 'domainURL', window.location.pathname);
      bodyURL = ef.addArgToURI(bodyURL, 'id', srcItem.recordID);
      ef.displayDialog({
        bodyURL: bodyURL,
        title: 'definitionEditor.editField.title',
        width: '90%',
        height: '75%',
        buttons: []
      });
    },
    // Handles the save dialog button on the field edit/add dialogs.
    _editorFieldSave: function (obj) {
      var divID = tk._getTopDialogID() + 'Messages';
      var data = {domainURL: window.location.pathname};
      var id = $$('editField').getValues().id;
      ef.clearMessages(divID);
      ef.postAjaxForm('editField', '/extension/saveField', data, function (response) {
        var map = JSON.parse(response);
        var list;
        var item;
        if (id.length > 0) {
          // Update in list.
          list = efd._editorFindListForFieldID(id);
          if (list) {
            list.data.each(function (obj) {
              if (obj.recordID == id) item = obj
            });
          }
          if (item) {
            list.updateItem(item.id, map);
          }
        } else {
          list = $$(AVAILABLE_LIST_ID);
          list.add(map);
        }
        ef.closeDialog();
      }, {divID: divID})
    },
    // Provides the display HTML for a field row in the add/remove main editor dialog page.
    _editorFieldTemplate: function (obj) {
      var text = obj.label;
      var icon = 'textField.png';
      if (obj.type) {
        icon = obj.type + ".png";
      }
      var css = "definition-add-remove";
      if (obj.custom) {
        css += '-custom';
      }
      var id = obj.name + "ListItem";
      id = id.replace(/:/g, "-");
      return "<span id='" + id + "' class='" + css + "'>" + text + "</span><img src='/assets/" + icon + "' style='width:100px;height:24px;'>"
    },
    // Finds the data item from the list for the given field name.
    _editorFindItemForField: function (fieldName) {
      // Check configured list first.
      var list = efd._editorFindListForField(fieldName);
      if (list) {
        var item = undefined;
        list.data.each(function (obj) {
          if (obj.name == fieldName) item = obj
        });
        return item;
      }
      return undefined;
    },
    // Finds the list name the given field is in.
    _editorFindListForField: function (fieldName) {
      // Check configured list first.
      var list = $$(CONFIGURED_LIST_ID);
      var item = undefined;
      list.data.each(function (obj) {
        if (obj.name == fieldName) item = obj
      });
      if (item) {
        return list;
      } else {
        // Try the other list.
        list = $$(AVAILABLE_LIST_ID);
        list.data.each(function (obj) {
          if (obj.name == fieldName) item = obj;
        });
        if (item) {
          return list;
        }
      }
      return undefined;
    },
    // Finds the list name the given record ID is in.
    _editorFindListForFieldID: function (recordID) {
      // Check configured list first.
      var list = $$(CONFIGURED_LIST_ID);
      var item = undefined;
      list.data.each(function (obj) {
        if (obj.recordID == recordID) item = obj
      });
      if (item) {
        return list;
      } else {
        // Try the other list.
        list = $$(AVAILABLE_LIST_ID);
        list.data.each(function (obj) {
          if (obj.recordID == recordID) item = obj;
        });
        if (item) {
          return list;
        }
      }
      return undefined;
    },
    // The double-click handler for either list.
    _editorListDoubleClickHandle: function (listName, id) {
      var item = $$(listName).getItem(id);
      if (item) {
        var name = item.name;
        if (name) {
          if (name.substr(0, 6) == 'group:') {
            efd._editorPanelOpenEditDialog(name);
          } else {
            // Open for custom field editor
            if (item.custom) {
              efd._editorFieldOpenEditDialog(name);
            }
          }
        }
      }
    },
    // Displays a std message in the current dialog.
    // If msg is a string, then displays as error.
    _editorDisplayDialogMessage: function (msg) {
      var options;
      // Handle various inputs and convert to a map (plain object).
      if (ef._isString(msg)) {
        options = {error: msg};
      } else {
        // Assume an object, so use as-is
        options = msg;
      }
      var divID = tk._getTopDialogID() + 'Messages';
      ef.clearMessages(divID);
      options.divID = divID;
      ef.displayMessage(options);
    },
    // Opens the main editor config dialog.
    _editorOpenConfigDialog: function () {
      var url = window.location.pathname;
      ef.displayDialog({
        bodyURL: '/extension/configDialog?domainURL=' + url,
        title: 'definitionEditor.title',
        width: '70%',
        height: '90%',
        buttons: []

      });
    },
    // Sets up various click/double-click/context handlers for the lists.
    _editorSetupConfigDialogHandlers: function () {
      $$(AVAILABLE_LIST_ID).attachEvent("onItemDblClick", function (id, e, node) {
        efd._editorListDoubleClickHandle(AVAILABLE_LIST_ID, id);
      });
      $$(CONFIGURED_LIST_ID).attachEvent("onItemDblClick", function (id, e, node) {
        efd._editorListDoubleClickHandle(CONFIGURED_LIST_ID, id);
      });
    },
    // Opens the add panel dialog.
    _editorPanelOpenAddDialog: function () {
      ef.displayDialog({
        bodyURL: '/extension/dialog?dialog=extension/addPanelDialog',
        title: 'definitionEditor.addPanel.title',
        width: '50%',
        height: '40%',
        buttons: ['ok', 'cancel'],
        ok: function (dialogID, button) {
          var panel = $$('panel').getValue();
          panel = panel.replace(/ /g, '');
          if (panel.length > 0) {
            var fieldName = 'group:' + panel;
            // Make sure the custom panel does not already exist.
            var checkList = efd._editorFindListForField(fieldName);
            if (checkList) {
              // Duplicate panel name.
              efd._editorDisplayDialogMessage(ef.lookup('error.133.message', panel));
              return false;
            }
            var list = $$(AVAILABLE_LIST_ID);
            var panelData = {name: fieldName, label: panel, custom: true, type: 'tabbedPanels'};
            list.add(panelData);
          } else {
            // Missing panel name.
            efd._editorDisplayDialogMessage(ef.lookup('error.1.message', ef.lookup('panel.label')));
            return false;
          }
        }
      });
    },
    // Opens the edit panel dialog.
    _editorPanelOpenEditDialog: function (panel) {
      // find current item selected
      if (panel == undefined) {
        // Check configured list first.
        var list = $$(CONFIGURED_LIST_ID);
        var item = list.getSelectedItem();
        if (item) {
          panel = item.name;
        } else {
          // Check the available list next
          list = $$(AVAILABLE_LIST_ID);
          item = list.getSelectedItem();
          if (item) {
            panel = item.name;
          }
        }
        if (panel == undefined) {
          //error.132.message=Please select a custom panel to edit.
          efd._editorDisplayDialogMessage({warning: ef.lookup('error.132.message')});
          return;
        }
      }
      if (panel.substr(0, 6) != 'group:') {
        //error.132.message=Please select a custom panel to edit.
        efd._editorDisplayDialogMessage({warning: ef.lookup('error.132.message')});
        return;
      }
      var srcItem = efd._editorFindItemForField(panel);
      if (srcItem) {
        if (!srcItem.custom) {
          //error.132.message=Please select a custom panel to edit.
          efd._editorDisplayDialogMessage({warning: ef.lookup('error.132.message')});
          return;
        }
      }

      if (panel.length > 6 && panel.substr(0, 6) == 'group:') {
        panel = panel.substr(6);
      }

      var bodyURL = '/extension/dialog?dialog=extension/editPanelDialog';
      bodyURL = ef.addArgToURI(bodyURL, 'panel', panel);
      ef.displayDialog({
        bodyURL: bodyURL,
        title: 'definitionEditor.editPanel.title',
        width: '50%',
        height: '40%',
        buttons: ['ok', 'cancel'],
        ok: function (dialogID, button) {
          // Update the panel name/label.
          var originalPanel = $$("editPanel").getValues().originalPanel;
          var originalFieldName = 'group:' + originalPanel;
          var panel = $$('panel').getValue();
          panel = panel.replace(/ /g, '');
          if (panel.length > 0) {
            var newName = 'group:' + panel;
            var item = efd._editorFindItemForField(originalFieldName);
            var list = efd._editorFindListForField(originalFieldName);
            if (item && originalPanel != panel) {
              // See if the new panel name is used anywhere in the lists.
              var count = 0;
              $$(CONFIGURED_LIST_ID).data.each(function (obj) {
                if (obj.name == newName) count++
              });
              $$(AVAILABLE_LIST_ID).data.each(function (obj) {
                if (obj.name == newName) count++
              });
              if (count > 0) {
                // Duplicate panel name.
                //error.133.message=Panel {0} already exists in the configured list.
                efd._editorDisplayDialogMessage(ef.lookup('error.133.message', panel));
                return false;
              }
              item.name = newName;
              item.label = panel;
              list.updateItem(item.id, item);
            }
          } else {
            // Missing panel name.
            var msg = ef.lookup('error.1.message', ef.lookup('panel.label'));
            ef.displayMessage({error: msg, divID: dialogID + 'Messages'});
            return false;
          }
        }
      });
    },
    // Saves the changes to the definition editor display list.
    _editorSave: function () {
      var fields = [];
      var list = $$(CONFIGURED_LIST_ID).data;
      //console.log(list);
      if (list.each) {
        list.each(function (obj) {
          fields.push(obj.name);
        });
      }

      var body = {domainURL: window.location.pathname, fields: fields};
      var jsonString = JSON.stringify(body);

      ef.post('/extension/saveFieldOrder', jsonString, function (responseText) {
        var res = JSON.parse(responseText);
        if (ef._isErrorMessage(res)) {
          // Display the error in the dialog itself.
          res.divID = tk._getTopDialogID() + 'Messages';
          ef.clearMessages(res.divID);
          ef.displayMessage(res);
        } else {
          ef.closeDialog();
          if (ef._isMessage(res)) {
            ef.displayMessage(res);
          }
          // Force a refresh a little while later.
          setTimeout('window.location=window.location;', 1000);
        }
      });

    },
    // Submits the save request for the edit action.  Marshals the form values and submits them.
    _editSave: function (formName, uri, recordID) {
      var values = $$(formName).getValues();
      // Add the record ID to the data to send.
      values.id = recordID;
      efd._addInlineGridRowsForSubmission(values);
      //console.log(values);
      webix.send(uri, values);
    },
    // Determines if the given field is a panel (starts with 'group:').
    _isPanel: function (name) {
      if (name != undefined) {
        if (name.length > 6 && name.indexOf('group:') == 0) {
          return true;
        }
      }
      return false;
    },
    // Registers the configuration action needed for this definition page.
    _registerConfigAction: function () {
      var p = window.location.pathname;
      if (p.indexOf('/show/') > 0 || p.indexOf('/create') > 0 || p.indexOf('/edit/') > 0) {
        // Config dialog only works on show/create/edit pages for now.
        ef._registerConfigAction({action: efd._editorOpenConfigDialog, title: 'Open Configuration Editor for Custom Fields'});
      }
    },
    // Registers an inline grid as a possible part of the form to be submitted.
    _registerInlineGridName: function (gridName) {
      inlineGridNames[inlineGridNames.length] = gridName;
    }
  }
}();
var efd = _efd.eframe_definition;  // Shorthand
var eframe_definition = _ef.eframe_definition;  // Simplified variable for access to eframe API.

