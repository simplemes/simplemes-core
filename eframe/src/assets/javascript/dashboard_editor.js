/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

/*
 * Support functions for the client-side eframe dashboard editor API.
 * This is not a public API and should not be used by most applications.
 *
 */

// Define the 'module' for the enterprise framework dashboard editor API in case of name conflicts.
// Allows access to the dashboard editor methods as: 'dashboardEditor.openDashboardEditor()' or 'ef.dashboardEditor.openDashboardEditor()'
var efe = efe || {};
efe.dashboardEditor = function () {
  var dashboardConfig;        // The dashboard configuration object for the dashboard being edited.
  var dashboardButtons;       // The dashboard buttons (in hierarchical format). This corresponds to the dashboardConfig.buttons.
  var baseHTMLID = 'Editor';    // The prefix used for most HTML IDs.
  var jsOut = [];               // A buffer to hold the javascript needed by the dashboard to build the splitter in the GUI.
  var splitterIndex = 0;        // Global indexes needed for the recursive splitter method below.
  var firstPanel = true;        // If true, then the current panel is the first panel.
  var panelSizes = [];          // The size of each panel.  (can be height or width).
  var selectedElement;        // The currently selected element.
  var selectedStateClass = 'editor-selected-state';  // The CSS class to use for selected elements.
  var jsonSplitterClass = 'org.simplemes.eframe.web.dashboard.DashboardPanelSplitter';  // The class name for a splitter, as needed for the JSON interface.
  var jsonPanelClass = 'org.simplemes.eframe.web.dashboard.DashboardPanel';  // The class name for a splitter, as needed for the JSON interface.
  var defaultCategory;        // The default category for a new dashboard.

  var editorDialog;           // The editor's dialog.
  var detailsDialog;          // The details dialog, if seen.
  var buttonDetailsDialog;    // The button details dialog, if seen.
  var panelDetailsDialog;     // The panel details dialog, if seen.
  var panelForDetailsDialog;  // The panel name for the details dialog, if seen.
  var unsavedChanges = false;   // Set to true if the editor has any unsaved changes.
  var buttonForDetailsDialog;   // The buttonID currently display in the buttons dialog.

  var lastClickTime;          // The time for the last click.

  //noinspection JSUnusedGlobalSymbols
  return {
    // Adds a new button (before or after selected button)
    addButton: function (buttonID, after) {
      var selectedButtonIndex = -1;
      var newIndex = 0;
      var newSequence = 10;
      if (!buttonID) {
        // Not passed in, so use the selected element.
        if (dashboardEditor.isButton(selectedElement)) {
          buttonID = this.getButtonIDFromView(selectedElement);
        }
      }
      if (buttonID) {
        for (var i = 0; i < dashboardConfig.buttons.length; i++) {
          if (dashboardConfig.buttons[i].buttonID == buttonID) {
            selectedButtonIndex = i;
            break;
          }
        }
      }
      if (selectedButtonIndex < 0) {
        if (dashboardButtons.length > 0) {
          // Place before or after all buttons.
          selectedButtonIndex = after ? dashboardButtons.length - 1 : 0
        }
      }

      if (selectedButtonIndex >= 0) {
        newIndex = after ? selectedButtonIndex + 1 : selectedButtonIndex;
        newSequence = dashboardButtons[selectedButtonIndex].pages[0].sequence + (after ? 1 : -1);
      }
      //console.log('buttonID:'+buttonID+', selectedElement:'+selectedElement);
      console.log('Adding (' + after + ') newIndex:' + newIndex + ', seq:' + newSequence + " for selectedButtonIndex:" + selectedButtonIndex);
      dashboardButtons.splice(newIndex, 0, {});
      dashboardButtons[newIndex].label = 'label' + newSequence;
      dashboardButtons[newIndex].buttonID = 'button' + newSequence;
      // Add a page to the list of child pages.
      var pages = [];
      dashboardButtons[newIndex].pages = pages;
      pages[0] = {};
      pages[0].url = 'change';
      pages[0].panel = 'A';
      pages[0].sequence = newSequence;
      console.log(dashboardButtons);

      // Push out to the persistence model.
      dashboardConfig.buttons = dashboardEditor.convertButtonsFromHierarchy(dashboardButtons);
      // Clear selection and re-display the dashboard.
      dashboardEditor.display(dashboardConfig);
      dashboardEditor.setUnsavedChanges(true);
    },
    // Adds a new button after the selected button
    addButtonAfter: function () {
      dashboardEditor.addButton(undefined, true);
    },
    // Adds a new button before the selected button
    addButtonBefore: function () {
      dashboardEditor.addButton(undefined, false);
    },
    // Adds the context menus to each button.
    addButtonContextMenus: function () {
      if (!dashboardConfig.buttons) {
        return;
      }
      for (var i = 0; i < dashboardButtons.length; i++) {
        var buttonName = dashboardButtons[i].buttonID;
        //console.log("buttonName: " + buttonName);

        webix.ui({
          view: "contextmenu", id: 'cm' + buttonName,
          width: tk.pw('15em'),
          master: $$(buttonName + 'Editor').$view,
          data: [
            {id: "addButtonBeforeContext", button: buttonName, value: ef.lookup("dashboardEditorMenu.addButtonBefore.label")},
            {id: "addButtonAfterContext", button: buttonName, value: ef.lookup("dashboardEditorMenu.addButtonAfter.label")},
            {$template: "Separator"},
            {id: "removeButtonContext", button: buttonName, value: ef.lookup("dashboardEditorMenu.removeButton.label")},
            {$template: "Separator"},
            {id: "buttonDetailsContext", button: buttonName, value: ef.lookup("dashboardEditorMenu.details.label")},
          ],
          on: {
            onItemClick: function (id) {
              var buttonID = this.getItem(id).button;
              switch (id) {
                case 'addButtonBeforeContext' :
                  dashboardEditor.addButton(buttonID, false)
                  break;
                case 'addButtonAfterContext' :
                  dashboardEditor.addButton(buttonID, true)
                  break;
                case 'removeButtonContext' :
                  dashboardEditor.removeButton(buttonID)
                  break;
                case 'buttonDetailsContext' :
                  dashboardEditor.openButtonDetailsDialog(buttonID)
                  break;
              }
            }
          }
        });
      }
    },
    // Adds a click handler for the main dialog.
    addClickHandler: function () {
      webix.event($$(editorDialog).$view, "click", function (e) {
        dashboardEditor.clickHandler(e);
      });
    },
    // Builds and displays the JS needed to add a context menus for the panels.
    addPanelContextMenus: function () {
      for (var i = 0; i < dashboardConfig.dashboardPanels.length; i++) {
        var panelName = dashboardConfig.dashboardPanels[i].panel;
        webix.ui({
          view: "contextmenu", id: 'cm' + panelName,
          width: tk.pw('15em'),
          master: $$("EditorPanel" + panelName).$view,
          data: [
            {id: "addHorizontalSplitter", panel: panelName, value: ef.lookup("dashboardEditorMenu.addHorizontalSplitter.label")},
            {id: "addVerticalSplitter", panel: panelName, value: ef.lookup("dashboardEditorMenu.addVerticalSplitter.label")},
            {$template: "Separator"},
            {id: "removePanelContext", panel: panelName, value: ef.lookup("dashboardEditorMenu.removePanel.label")},
            {$template: "Separator"},
            {id: "panelDetailsContext", panel: panelName, value: ef.lookup("dashboardEditorMenu.details.label")},
          ],
          on: {
            onItemClick: function (id, x) {
              var eventPanel = this.getItem(id).panel;
              //console.log('EventPanel: ' + eventPanel + ", find() = " + dashboardEditor.findPanelByID(eventPanel));
              switch (id) {
                case 'addHorizontalSplitter' :
                  dashboardEditor.addHSplitter(eventPanel)
                  break;
                case 'addVerticalSplitter' :
                  dashboardEditor.addVSplitter(eventPanel)
                  break;
                case 'removePanelContext' :
                  dashboardEditor.removePanelByName(eventPanel)
                  break;
                case 'panelDetailsContext' :
                  dashboardEditor.openPanelDetailsDialog(eventPanel)
                  break;
              }
            }
          }
        });
      }
    },
    // Adds a Horizontal splitter to the selected panel.
    addHSplitter: function (eventPanel) {
      dashboardEditor.addSplitter(eventPanel, false);
    },
    // Adds a Vertical splitter to the selected panel.
    addVSplitter: function (eventPanel) {
      dashboardEditor.addSplitter(eventPanel, true);
    },
    addSplitterFromMenu: function (vertical) {
      ef.clearMessages();
      var eventPanel = dashboardEditor.findPanelByID(selectedElement);
      this.addSplitter(eventPanel, vertical);
    },
    addSplitter: function (eventPanel, vertical) {
      ef.clearMessages();
      if (!eventPanel) {
        ef.displayMessage({warn: eframe.lookup('error.114.message')});
        return;
      }

      // We will use a combined splitter/panel array to make sure the 'index' (sequence) values are unique and
      // assigned correctly.  The insert/remove logic is simpler with a single array to maintain since the array
      // indexes are only valid for the combined array.
      // NOTE: The 'panelIndex' values are array indices for this combined array only.

      var panels = dashboardEditor.combinePanelsAndSplitters(dashboardConfig.splitterPanels, dashboardConfig.dashboardPanels);

      // The new splitter will replace the selected panel and the panel will become the first child of the new splitter.
      // New splitter takes the place of the selected panel.
      // Move the selected panel to end so it is the first child of new splitter.

      var panelIndex = panels.length;
      var newPanelName = this.determineNextPanelName();
      var selectedPanelIndex = dashboardEditor.findPanelIndex(eventPanel);
      var newSplitterIndex = dashboardEditor.findPanelIndex(eventPanel);

      // Update the model.
      // Move the selected panel to the end of the panel array so we can put the splitter
      // in its old place.
      var oldParentIndex = panels[selectedPanelIndex].parentPanelIndex;
      //console.log("oldParentIndex = " + oldParentIndex);
      var movedPanelIndex = panels.length;
      panels[movedPanelIndex] = {};
      panels[movedPanelIndex] = panels[selectedPanelIndex];
      panels[movedPanelIndex].panelIndex = movedPanelIndex;
      // Now, make sure the selected panel has the splitter as it's new parent.
      panels[movedPanelIndex].parentPanelIndex = newSplitterIndex;

      // Fill in the new splitter details, using the original parent.
      panels[newSplitterIndex] = {};
      panels[newSplitterIndex].panelIndex = newSplitterIndex;
      panels[newSplitterIndex].panel = undefined;
      panels[newSplitterIndex].vertical = vertical;
      panels[newSplitterIndex].parentPanelIndex = oldParentIndex;
      //console.log("set parent for [" + newSplitterIndex + "] to " + oldParentIndex);
      //console.log("oldParentIndex = " + oldParentIndex);

      // Now, add the new panel at the end.
      var newPanelIndex = panels.length;
      panels[newPanelIndex] = {};
      panels[newPanelIndex].panelIndex = newPanelIndex;
      panels[newPanelIndex].parentPanelIndex = newSplitterIndex;
      panels[newPanelIndex].panel = newPanelName;

      // Now, separate out the combined array again for saving in the model.
      // NOTE: The 'panelIndex' values are array indices for this combined array only.
      dashboardEditor.separatePanelsAndSplitters(panels, dashboardConfig);

      // Clear selection and re-display the dashboard.
      dashboardEditor.display(dashboardConfig);
      dashboardEditor.setUnsavedChanges(true);
    },
    // Builds the Javascript object for the content of the dashboard editor panels.
    buildDashboardContent: function () {
      //console.log(dashboardConfig);
      var content;
      if (dashboardConfig.dashboardPanels.length > 1) {
        content = dashboardEditor.buildPanelOrSplitterContent(dashboardConfig.splitterPanels[0])
      } else {
        var panel = dashboardConfig.dashboardPanels[0];
        content = {
          type: "space", margin: 0, id: "EditorPanelContent", width: tk.pw("100%"), height: tk.ph("60%"), rows: [
            dashboardEditor.buildPanelContent(panel, {})
          ]
        };
      }

      return content;
    },
    // Builds the Javascript objects needed for a member of the dashboard.  Can be a splitter or panel.
    // Returns a A Javascript Array of object for the member.
    buildPanelOrSplitterContent: function (splitterOrPanel, options) {
      if (splitterOrPanel.panel) {
        return this.buildPanelContent(splitterOrPanel, options);
      } else {
        var vertical = splitterOrPanel.vertical;
        var id = "EditorContainer" + splitterOrPanel.panelIndex;
        var content = {
          type: "space", margin: 4, id: id, width: tk.pw("100%"),/* height: tk.ph("70%")*/  // This margin controls the color of the splitter handle.
        };
        if (vertical) {
          content.cols = dashboardEditor.buildSplitterContent(splitterOrPanel);
        } else {
          content.rows = dashboardEditor.buildSplitterContent(splitterOrPanel);
        }

        return content;
      }
    },

    // Builds the Javascript objects needed for a single splitter.  This includes the nested splitters if needed.
    // Returns a Javascript Array of object for the splitter.
    buildSplitterContent: function (splitter) {
      var content = [];

      var panelsInSplitter = dashboardEditor.findChildrenForSplitter(splitter);
      var panel0 = panelsInSplitter[0];
      var panel1 = panelsInSplitter[1];

      var resizerName = "EditorResizer" + splitter.panelIndex;
      content[content.length] = dashboardEditor.buildPanelOrSplitterContent(panel0, {resizerName: resizerName});
      content[content.length] = {view: "resizer", id: resizerName};
      content[content.length] = dashboardEditor.buildPanelOrSplitterContent(panel1, {});

      return content;
    },
    // Builds the Javascript objects needed for a panel content.
    // Options supported: resizerName.
    // Returns a Javascript object for the panel.
    buildPanelContent: function (panel, options) {
      var resizerName = options.resizerName;
      var panelName = panel.panel;
      var text = panel.defaultURL ? panel.defaultURL : '';
      var templateText = ef.lookup("panel.label") + " " + panelName + ":&nbsp;&nbsp;&nbsp;" + text;

      var content = {
        view: "form", id: "EditorPanel" + panelName, type: "clean", margin: 0, paddingX: 0, paddingY: 0,
        borderless: true,
        elements: [
          {view: "template", id: "EditorContent" + panelName, template: templateText},
        ]
      };
      if (firstPanel) {
        // Add a button holder for first panel only.
        // NOTE: This is hard-coded as panel 'A', but it can be in any panel.  It just needs to be in one panel.
        content.elements[content.elements.length] = {
          view: "form", id: "ButtonsEditorA", type: "clean", borderless: true,
          elements: [{view: "template", id: "ButtonsContentEditorA", template: " "}]
        };
      }

      if (resizerName) {
        content.on = {
          onViewResize: function () {
            dashboardEditor.handleSplitterResized("EditorPanel" + panelName, panelName, resizerName, false);
          }
        };
      }

      if (panelSizes[panelName] && panelSizes[panelName].height) {
        content.height = tk.ph(panelSizes[panelName].height + "%");
      }
      if (panelSizes[panelName] && panelSizes[panelName].width) {
        content.width = tk.pw(panelSizes[panelName].width + "%");
      }

      firstPanel = false;

      return content;
    },
    // Calculates the panel sizes for each panel.
    calculatePanelSizes: function (config) {
      // TODO: handle width and defaultSize
      var heightAvailable = 65.0;
      if (config.dashboardPanels.length == 1) {
        return [55.0];
      }

      var sizes = [];
      if (config.dashboardPanels) {
        for (var i = 0; i < config.dashboardPanels.length; i++) {
          sizes[config.dashboardPanels[i].panel] = {height: heightAvailable / config.dashboardPanels.length};
        }
      }
      return sizes;
    },
    // Cancels the editor dialog. Checks for unsaved changes.
    cancel: function () {
      dashboardEditor.checkForUnsavedChanges(function () {
        eframe.closeDialog(editorDialog)
      });
    },
    // Checks for changes to a simple field and returns the value for use in an assignment.
    // e.g. button.label=dashboardEditor.checkForChanges(button.label,$$('panel').getInputNode().value));
    checkForChanges: function (originalValue, newValue) {
      if (originalValue == undefined) {
        originalValue = '';
      }
      if (originalValue != newValue) {
        //console.log("Changed from '" + originalValue + "' to '" + newValue + "'");
        dashboardEditor.setUnsavedChanges(true);
      }
      return newValue;
    },
    // Checks for unsaved changes for the given event(default prevented).
    // done=Function to call if any option other than cancel is selected (required).
    // title=The question dialog title.
    checkForUnsavedChanges: function (done, event, title) {
      if (unsavedChanges) {
        if (!title) {
          title = eframe.lookup('unsavedChanges.title');
        }
        if (event) {
          event.preventDefault();
        }
        eframe.displayQuestionDialog({
          title: title,
          // unsavedChanges.message=Do you want to save changes to {0}?
          question: eframe.lookup('unsavedChanges.message', dashboardConfig.dashboard),
          buttons: ['save', 'noSave', 'cancel'],
          save: function (dialogID, action) {
            dashboardEditor.save(function () {
              done();
              dashboardEditor.refreshDashboard(false);
            });
          },
          noSave: function (dialogID, action) {
            done();
          }
        });
      } else {
        done();
      }
    },
    // Clears the editor's current dashboard and sets the config to a default dashboard.
    // If defaultCfg is provided, then use it for the new dashboard's defaultConfig setting
    clearDashboard: function (defaultCfg) {
      var newDefaultConfig = dashboardConfig ? dashboardConfig.defaultConfig : true;
      if (defaultCfg != undefined) {
        newDefaultConfig = defaultCfg;
      }
      var onePanel;
      onePanel = {panel: 'A', defaultURL: '', class: jsonPanelClass};
      dashboardConfig = {dashboard: 'NEW', title: '', defaultConfig: newDefaultConfig, category: defaultCategory};
      dashboardConfig.dashboardPanels = [onePanel];
      dashboardConfig.buttons = [];
      dashboardEditor.display(dashboardConfig);
      dashboardEditor.setUnsavedChanges(false);
      dashboardEditor.updateDialogTitle();
    },
    // Clears the editor's message area.
    clearMessages: function () {
      ef.clearMessages();
    },
    // Toggles the button selection.
    clickedButton: function (event, id) {
      dashboardEditor.toggleSelection(id);
    },
    // Gets the button from the button view name.
    getButtonIDFromView: function (viewName) {
      var index = viewName.indexOf('Editor');
      if (index > 0) {
        return viewName.substr(0, index);
      }
    },
    // Gets the panel object from the dashboardConfig for the given panelName (e.g. 'A').
    getPanel: function (panelName) {
      for (var i = 0; i < dashboardConfig.dashboardPanels.length; i++) {
        if (dashboardConfig.dashboardPanels[i].panel == panelName) {
          return dashboardConfig.dashboardPanels[i];
        }
      }
      return undefined;
    },
    // Finds the panel or button ID for the given mouse event.
    findPanelOrButtonForClick: function (event) {
      return this.findPanelOrButtonForClickParent(event.target);
    },
    // Recursively finds the parent Pane or Button element.
    findPanelOrButtonForClickParent: function (target) {
      var parent = target.parentElement;
      if (parent) {
        var attr = parent.attributes['view_id'];
        if (attr) {
          var view_id = attr.value;
          if (view_id.startsWith('EditorPanel')) {
            return view_id;
          }
          if (view_id.endsWith('Editor') && parent.classList.contains('webix_el_button')) {
            return view_id;
          }
        }
        // Stop looking once we hit the dialog parent.
        attr = parent.attributes['role'];
        if (attr) {
          if (attr.value == 'dialog') {
            return undefined;
          }
        }

        // Try next parent
        return this.findPanelOrButtonForClickParent(parent);
      }
      return undefined;
    },
    // Handles the click on the dialog.
    clickHandler: function (event) {
      var doubleClick = false;
      var clickTime = new Date().getTime();
      if (lastClickTime) {
        // See if this is a double click.
        if ((clickTime - lastClickTime) < 300) {
          doubleClick = true;
        }
      }

      lastClickTime = clickTime;
      var panelOrButton = this.findPanelOrButtonForClick(event);
      if (panelOrButton) {
        event.stopPropagation();
        if (panelOrButton.startsWith('EditorPanel')) {
          if (doubleClick) {
            this.doubleClickedPanel(event, panelOrButton);
          } else {
            this.clickedPanel(event, panelOrButton);
          }
        } else {
          if (doubleClick) {
            this.doubleClickedButton(event, panelOrButton);
          } else {
            this.clickedButton(event, panelOrButton);
          }

        }
      }
    },
    // Toggles the panel selection.
    clickedPanel: function (event, id) {
      dashboardEditor.toggleSelection(id);
    },
    closeButtonDetailsDialog: function (ok) {
      if (ok) {
        var button = this.findButtonByID(buttonForDetailsDialog);
        if (!button) {
          return;
        }

        button.buttonID = dashboardEditor.checkForChanges(button.buttonID, $$('buttonID').getInputNode().value);
        button.label = dashboardEditor.checkForChanges(button.label, $$('label').getInputNode().value);
        button.title = dashboardEditor.checkForChanges(button.title, $$('title').getInputNode().value);
        button.css = dashboardEditor.checkForChanges(button.css, $$('css').getInputNode().value);
        var s = $$('size').getInputNode().value;
        var n = parseFloat(s);
        if (!Number.isNaN(n)) {
          button.size = dashboardEditor.checkForChanges(button.size, n);
        }

        var p = toolkit.getGridData('buttons');

        // Check for changes in the button activities.
        var origPages = button.pages;
        if (origPages.length != p.length) {
          dashboardEditor.setUnsavedChanges(true);
        } else {
          // Same number of pages, so check each one.
          for (var j = 0; j < p.length; j++) {
            dashboardEditor.checkForChanges(origPages[j].sequence, p[j].sequence);
            dashboardEditor.checkForChanges(origPages[j].url, p[j].url);
            dashboardEditor.checkForChanges(origPages[j].panel, p[j].panel);
          }
        }

        button.pages = [];
        for (var i = 0; i < p.length; i++) {
          var page;
          page = {};
          page.sequence = p[i].sequence;
          page.url = p[i].url;
          page.panel = p[i].panel;
          button.pages[button.pages.length] = page;
        }
        // Re-sort the buttons on sequence in case the user changed the sequence.
        dashboardButtons.sort(function (a, b) {
          return a.pages[0].sequence - b.pages[0].sequence
        });
        console.log(dashboardButtons);
        dashboardConfig.buttons = dashboardEditor.convertButtonsFromHierarchy(dashboardButtons);
        console.log(dashboardConfig.buttons);
      }

      // Re-display the dashboard.
      dashboardEditor.updateDialogTitle();
      dashboardEditor.display(dashboardConfig);
    },
    closeDetailsDialog: function (ok) {
      if (ok) {
        dashboardConfig.dashboard = dashboardEditor.checkForChanges(dashboardConfig.dashboard, $$('dashboard').getInputNode().value);
        dashboardConfig.category = dashboardEditor.checkForChanges(dashboardConfig.category, $$('category').getInputNode().value);
        dashboardConfig.title = dashboardEditor.checkForChanges(dashboardConfig.title, $$('title').getInputNode().value);
        dashboardConfig.defaultConfig = dashboardEditor.checkForChanges(dashboardConfig.defaultConfig, $$('defaultConfig').getValue() == '1');
        dashboardEditor.updateDialogTitle();
      }
    },
    closePanelDetailsDialog: function (ok) {
      if (ok) {
        var panel = this.getPanel(panelForDetailsDialog);
        panel.panel = dashboardEditor.checkForChanges(panel.panel, $$('panel').getInputNode().value);
        panel.defaultURL = dashboardEditor.checkForChanges(panel.defaultURL, $$('defaultURL').getInputNode().value);

        // Re-display the dashboard.
        dashboardEditor.updateDialogTitle();
        dashboardEditor.display(dashboardConfig);
      }
    },
    // Combines the dashboardConfig's panels and splitters into one array.
    // Used to insert/remove panels with unique index values.
    combinePanelsAndSplitters: function (splitters, panels) {
      var res = [];
      if (splitters) {
        for (var i = 0; i < splitters.length; i++) {
          res[splitters[i].panelIndex] = splitters[i];
        }
      }
      for (i = 0; i < panels.length; i++) {
        res[panels[i].panelIndex] = panels[i];
      }

      return res;
    },
    // The user right-clicked the button, so we want to make sure it is selected.
    contextClickedButton: function ($element, event) {
      if ($element.is(selectedElement)) {
        // Already selected, so do nothing.
        return;
      }
      // Use normal panel click logic to select this panel.
      dashboardEditor.clickedButton($element, event);
    },
    // The user right-clicked the panel, so we want to make sure it is selected.
    contextClickedPanel: function ($element, event) {
      if ($element.is(selectedElement)) {
        // Already selected, so do nothing.
        return;
      }
      // Use normal panel click logic to select this panel.
      dashboardEditor.clickedPanel($element, event);
    },
    // Converts the DashboardConfig button hierarchy a the flattened list suitable for saving.
    // This flattened format corresponds to the persistent DashboardButton format.
    convertButtonsFromHierarchy: function (buttons) {
      var b = [];
      if (buttons == undefined) {
        return undefined;
      }
      // Flatten the hierarchical model suitable for saving.
      for (var bi in buttons) {
        var button = buttons[bi];
        var buttonIndex = b.length;
        b[buttonIndex] = {};
        b[buttonIndex].label = button.label;
        b[buttonIndex].title = button.title;
        b[buttonIndex].css = button.css;
        b[buttonIndex].size = button.size;
        b[buttonIndex].buttonID = button.buttonID;
        b[buttonIndex].sequence = button.pages[0].sequence;
        // Use the first page for this initial DashboardButton row.
        b[buttonIndex].url = button.pages[0].url;
        b[buttonIndex].panel = button.pages[0].panel;

        // Now, add the the other pages (if any).
        for (var i = 1; i < button.pages.length; i++) {
          buttonIndex++;
          b[buttonIndex] = {};
          b[buttonIndex].label = b[buttonIndex - 1].label;
          b[buttonIndex].buttonID = b[buttonIndex - 1].buttonID;
          b[buttonIndex].title = b[buttonIndex - 1].title;
          b[buttonIndex].css = b[buttonIndex - 1].css;
          b[buttonIndex].size = b[buttonIndex - 1].size;
          b[buttonIndex].sequence = button.pages[i].sequence;
          b[buttonIndex].url = button.pages[i].url;
          b[buttonIndex].panel = button.pages[i].panel;
        }
      }
      //console.log(dashboardButtons);
      return b;
    },
    // Creates a new unsaved entry.
    createNew: function () {
      dashboardEditor.checkForUnsavedChanges(function () {
        dashboardEditor.clearDashboard(false)
      });
    },
    // Converts the DashboardConfig button list to an internal hierarchy for display in the dashboard.
    // This flattened format corresponds to the persistent DashboardButton format.
    convertButtonsToHierarchy: function (buttons) {
      var b = [];
      if (buttons == undefined) {
        return b;
      }
      // Combine all button rows with the same label into one entry with a sub-list of pages to display as a child array.
      for (var bi in buttons) {
        var button = buttons[bi];
        var buttonIndex = -1;
        for (var i = 0; i < b.length; i++) {
          if (b[i].label == button.label) {
            buttonIndex = i;
          }
        }
        // noinspection DuplicatedCode
        if (buttonIndex < 0) {
          // Not found, so add to end.
          buttonIndex = b.length;
          b[buttonIndex] = {};
          b[buttonIndex].label = button.label;
          b[buttonIndex].title = button.title;
          b[buttonIndex].css = button.css;
          b[buttonIndex].size = button.size;
          b[buttonIndex].buttonID = button.buttonID;
          b[buttonIndex].sequence = button.sequence;
          b[buttonIndex].pages = [];
        }
        // Add the page to the list of child pages.
        var pages = b[buttonIndex].pages;
        var pageIndex = pages.length;
        pages[pageIndex] = {};
        pages[pageIndex].url = button.url;
        pages[pageIndex].panel = button.panel;
        pages[pageIndex].sequence = button.sequence;
      }
      return b;
    },
    // Deletes the dashboard from the DB and clears the current editor.
    deleteDashboard: function () {
      var s = dashboardEditor.validate();
      //console.log(s);
      if (s) {
        ef.displayMessage({error: s});
        return;
      }
      // Make sure old child records won't cause trouble on the update.
      dashboardConfig._mode = "childClear";
      // Send save request to server.
      var uri = '/dashboardConfig/crud/' + dashboardConfig.uuid;
      ef.ajax('DELETE', uri, {}, function (response, status, xhr) {
        //default.deleted.message=Deleted {0} (id={1})
        var msg = eframe.lookup('default.deleted.message', [dashboardConfig.dashboard, dashboardConfig.uuid]);
        ef.displayMessage({info: msg});
        dashboardEditor.clearDashboard();
      });
    },
    // Determines the next available panel(name) based on the current panels
    determineNextPanelName: function () {
      var nextCode = 65;
      var i;
      for (i = 0; i < dashboardConfig.dashboardPanels.length; i++) {
        var panelName = dashboardConfig.dashboardPanels[i].panel;
        if (panelName && panelName.length == 1) {
          var code = panelName.charCodeAt(0) + 1;
          if (code > nextCode) {
            nextCode = code;
          }
        }
      }
      return String.fromCharCode(nextCode);
    },
    display: function (configIn) {
      firstPanel = true;
      panelSizes = this.calculatePanelSizes(configIn);

      var cfg = dashboardConfig = configIn;
      this.logModel(dashboardConfig);

      var content = this.buildDashboardContent();

      var parentViewName = 'EditorPanel';
      var contentViewName = 'EditorContent';
      $$(parentViewName).removeView($$(contentViewName));
      $$(parentViewName).addView({view: 'form', type: "clean", borderless: true, id: contentViewName, margin: 0, rows: [content]}, 0);

      // sort on button sequence
      if (dashboardConfig.buttons) {
        dashboardConfig.buttons.sort(function (a, b) {
          return a.sequence - b.sequence
        });
      }
      dashboardButtons = dashboardEditor.convertButtonsToHierarchy(dashboardConfig.buttons);
      dashboard._addButtonsIfNeededInternal('A', dashboardButtons, 'Editor', true);
      dashboardEditor.addPanelContextMenus();
      dashboardEditor.addButtonContextMenus();

      selectedElement = undefined;
    },
    // Handles button double clicks by selecting the button, then opening the details dialog.
    doubleClickedButton: function (event, id) {
      var buttonID = this.getButtonIDFromView(id);
      dashboardEditor.openButtonDetailsDialog(buttonID);
    },
    // Handles panel double clicks by selecting the panel, then opening the details dialog.
    doubleClickedPanel: function (event, id) {
      var eventPanel = this.findPanelByID(id);
      dashboardEditor.openPanelDetailsDialog(eventPanel)
    },
    // Creates a new unsaved entry using the current config.
    duplicate: function () {
      // No need to check for unsaved changes since those are preserved in the copy.
      // Clear the IDs in the current definition to force saving of the copy.
      dashboardConfig.uuid = undefined;
      for (var i = 0; i < dashboardConfig.dashboardPanels.length; i++) {
        dashboardConfig.dashboardPanels[i].uuid = undefined;
      }
      for (i = 0; i < dashboardConfig.splitterPanels.length; i++) {
        dashboardConfig.splitterPanels[i].uuid = undefined;
      }
      for (i = 0; i < dashboardConfig.buttons.length; i++) {
        dashboardConfig.buttons[i].uuid = undefined;
      }

      // Make a new title
      dashboardConfig.dashboard = 'COPY ' + dashboardConfig.dashboard;
      dashboardConfig.title = 'COPY ' + dashboardConfig.title;
      dashboardEditor.updateDialogTitle();
      dashboardEditor.setUnsavedChanges(true);
    },
    // Finds the panel name for the given button ID.
    findButtonByID: function (buttonID) {
      var button;
      for (var i = 0; i < dashboardButtons.length; i++) {
        if (dashboardButtons[i].buttonID == buttonID) {
          return dashboardButtons[i];
        }
      }
      return undefined;
    },
    // Finds the panel element for the given panel index.
    findPanelByIndex: function (index) {
      for (var i = 0; i < dashboardConfig.dashboardPanels.length; i++) {
        if (dashboardConfig.dashboardPanels[i].panelIndex == index) {
          return dashboardConfig.dashboardPanels[i];
        }
      }
      return undefined;
    },
    // Finds the index for the given panel name.
    findPanelIndex: function (panelName) {
      for (var i = 0; i < dashboardConfig.dashboardPanels.length; i++) {
        if (dashboardConfig.dashboardPanels[i].panel == panelName) {
          return dashboardConfig.dashboardPanels[i].panelIndex;
        }
      }
      return undefined;
    },
    // Finds the panel name for the given view ID (e.g. 'EditorPanelA' returns 'A").
    findPanelByID: function (viewID) {
      if (viewID && viewID.startsWith('EditorPanel')) {
        return viewID.substr(11, viewID.length);
      }
      return undefined;
    },
    // Finds the index for the given view ID (e.g. 'EditorPanelA').
    findPanelIndexByID: function (viewID) {
      if (viewID && viewID.startsWith('EditorPanel')) {
        var panelName = viewID.substr(11, viewID.length);
        return this.findPanelIndex(panelName);
      }
      return undefined;
    },
    // Finds the two panels/splitters in the given splitter for the current dashboardConfig.
    findChildrenForSplitter: function (splitter) {
      var res = [];
      for (var i = 0; i < dashboardConfig.dashboardPanels.length; i++) {
        if (dashboardConfig.dashboardPanels[i].parentPanelIndex == splitter.panelIndex) {
          res[res.length] = dashboardConfig.dashboardPanels[i];
        }
      }
      for (i = 0; i < dashboardConfig.splitterPanels.length; i++) {
        if (dashboardConfig.splitterPanels[i].parentPanelIndex == splitter.panelIndex) {
          res[res.length] = dashboardConfig.splitterPanels[i];
        }
      }

      // Return in the correct order
      if (res.length == 2) {
        res.sort(function (a, b) {
          return a.panelIndex - b.panelIndex;
        });
      }

      return res;
    },
    // Handle resize of the splitters so it can be saved as the default size for the panel.
    handleSplitterResized: function (element, panel, resizer, vertical) {
      var value;
      var maximum;
      if (vertical) {
        value = $$("EditorPanel" + panel).$width;
        maximum = window.innerWidth;
      } else {
        value = $$("EditorPanel" + panel).$height;
        maximum = window.innerHeight;
      }
      if (value > 0 && maximum > 0) {
        var percent = value / maximum * 100.0;
        percent = percent.toFixed(2);
        //console.log(element + "(" + resizer + "):" + panel + " size: " + percent + "(" + value + " pixels)" + " vertical:" + vertical);

        for (var i = 0; i < dashboardConfig.dashboardPanels.length; i++) {
          if (dashboardConfig.dashboardPanels[i].panel == panel) {
            dashboardEditor.checkForChanges(dashboardConfig.dashboardPanels[i].defaultSize, percent);
            dashboardConfig.dashboardPanels[i].defaultSize = percent;
          }
        }
      }
    },
    // Determines if the toolkit ID is a dashboard button.
    isButton: function (viewID) {
      return tk._getViewType(viewID) == 'button';
    },
    // Determines if the toolkit ID is a dashboard panel.
    isPanel: function (viewID) {
      return tk._getViewType(viewID) == 'form';
    },
    // Loads the dashboard config.
    load: function (dashboardName) {
      if (dashboardName != undefined) {
        // Read the Config into local memory.
        var uri = '/dashboardConfig/crud/' + dashboardName;
        ef.get(uri, {}, function (responseText) {
          var data = JSON.parse(responseText);
          dashboardEditor.display(data);
          dashboardEditor.setUnsavedChanges(false);
          dashboardEditor.updateDialogTitle();
        });
      } else {
        // No existing dashboard, so create an empty to edit...
        dashboardEditor.clearDashboard();
      }
    },
    // Loads the button into the details dialog fields.
    loadButtonDetailsDialogValues: function (buttonID) {
      var button = this.findButtonByID(buttonID);
      if (!button) {
        return;
      }

      $$('buttonID').getInputNode().value = button.buttonID ? button.buttonID : '';
      $$('label').getInputNode().value = button.label ? button.label : '';
      $$('title').getInputNode().value = button.title ? button.title : '';
      $$('css').getInputNode().value = button.css ? button.css : '';
      $$('size').getInputNode().value = button.size ? button.size : '';

      for (i = 0; i < button.pages.length; i++) {
        var value;
        value = {};
        value.sequence = button.pages[i].sequence;
        value.url = button.pages[i].url;
        value.panel = button.pages[i].panel;
        //grid.dataSource.add(value);
        tk._gridAddRow($$("buttons"), value, true)
      }

      tk.focus('buttonID');
    },
    // Loads the dashboard config into the details dialog fields.
    loadDetailDialogValues: function () {
      $$('dashboard').getInputNode().value = dashboardConfig.dashboard;
      $$('category').getInputNode().value = dashboardConfig.category;
      $$('title').getInputNode().value = dashboardConfig.title;
      $$('defaultConfig').setValue(dashboardConfig.defaultConfig);

      tk.focus('dashboard');
    },
    // Loads the given panel's values into the panel detals dialog.
    loadPanelDetailsDialogValues: function (panelName) {
      var panel = this.getPanel(panelName);
      if (panel) {
        $$('panel').getInputNode().value = panel.panel;
        $$('defaultURL').getInputNode().value = panel.defaultURL;

        tk.focus('panel');
      }
    },
    // Logs the current model.
    logModel: function (cfg) {
      if (cfg) {
        if (cfg.dashboardPanels.length > 1) {
          this.logSplitterFromModelRecursive(cfg, cfg.splitterPanels[0], 0);
        } else {
          this.logPanelFromModel(cfg.dashboardPanels[0], '');
        }
      } else {
        JL().error('undefined configuration');
      }
    },
    // Logs the given splitter at a given indention depth. Recursive version.
    logPanelFromModel: function (panel, padding) {
      console.log(padding + "Panel " + panel.panelIndex + ' panel=' + panel.panel + ' parent=' + panel.parentPanelIndex);
    },
    // Logs the given splitter at a given indention depth. Recursive version.
    logSplitterFromModelRecursive: function (cfg, splitter, depth) {
      var padding = '';
      var i;
      for (i = 0; i < depth; i++) {
        padding += '  ';
      }

      var direction = splitter.vertical ? 'V' : 'H';
      console.log(padding + direction + " Splitter " + splitter.panelIndex + ' parent=' + splitter.parentPanelIndex);
      for (i = 0; i < cfg.dashboardPanels.length; i++) {
        var panel1 = cfg.dashboardPanels[i];
        if (panel1.parentPanelIndex == splitter.panelIndex) {
          this.logPanelFromModel(panel1, padding + '  ');
        }
      }

      // Now, log any child splitters
      for (i = 0; i < cfg.splitterPanels.length; i++) {
        var splitter2 = cfg.splitterPanels[i];
        if (splitter2.parentPanelIndex == splitter.panelIndex && depth < 20) {
          // Log the nested splitter.
          this.logSplitterFromModelRecursive(cfg, splitter2, depth + 1);
        }
      }

    },
    // Pops-up the dashboard editor page.
    openDashboardEditor: function () {
      var url = eframe.addArgToURI('/dashboardConfig/editor', 'dashboard', dashboard.currentDashboard);
      if (dashboard.currentDashboard == undefined) {
        // New dashboard, so go into new dashboard mode
        url = eframe.addArgToURI(url, 'mode', 'new');
      }
      if (dashboard.currentCategory != undefined) {
        // Pass the category for no dashboard and after delete scenarios.
        url = eframe.addArgToURI(url, 'category', dashboard.currentCategory);
      }
      unsavedChanges = false;
      editorDialog = ef.displayDialog({
        bodyURL: url,
        title: 'dashboard.editor.title',
        width: '95%',
        height: '98%',
        messageArea: true,
        buttons: [],
        postScript: 'dashboardEditor.addClickHandler()',
        beforeClose: function (dialogID, action) {
          // noinspection JSDeprecatedSymbols
          dashboardEditor.checkForUnsavedChanges(function () {
            eframe.closeDialog(dialogID)
          }, event);
          return false;
        }
      });


    },
    // Opens the buttons details dialog
    openButtonDetailsDialog: function (buttonID) {
      if (!buttonID) {
        if (!selectedElement || !dashboardEditor.isButton(selectedElement)) {
          ef.displayMessage({warn: eframe.lookup('error.118.message')});
          return;
        }
        buttonID = this.getButtonIDFromView(selectedElement);
      }
      buttonForDetailsDialog = buttonID;

      var url = '/dashboardConfig/buttonDetailsDialog';

      buttonDetailsDialog = ef.displayDialog({
        bodyURL: url,
        title: 'dashboard.editor.buttonDetailsDialog.title',
        width: '80%',
        height: '90%',
        messageArea: true,
        buttons: ['ok', 'cancel'],
        postScript: "dashboardEditor.loadButtonDetailsDialogValues('" + buttonID + "')",
        ok: function (dialogID, button) {
          dashboardEditor.closeButtonDetailsDialog(true);
          return true;
        }
      });


    },
    // Opens the delete dialog
    openDeleteDialog: function () {
      ef.displayQuestionDialog({
        title: eframe.lookup('delete.confirm.title'),
        question: eframe.lookup('delete.confirm.message', eframe.lookup('dashboard.label'), dashboardConfig.dashboard),
        buttons: ['delete', 'cancel'],
        delete: function () {
          ef.closeDialog(editorDialog);
          dashboardEditor.deleteDashboard();
          dashboardEditor.refreshDashboard(true);
        }
      });
    },
    // Opens the details dialog
    openDetailsDialog: function () {
      var url = '/dashboardConfig/detailsDialog';

      detailsDialog = ef.displayDialog({
        bodyURL: url,
        title: 'dashboard.editor.detailsDialog.title',
        width: '80%',
        height: '80%',
        messageArea: true,
        buttons: ['ok', 'cancel'],
        postScript: "dashboardEditor.loadDetailDialogValues()",
        ok: function (dialogID, button) {
          dashboardEditor.closeDetailsDialog(true);
          return true;
        }
      });

    },
    openPanelDetailsDialog: function (panelName) {
      if (!panelName) {
        if (!selectedElement || !dashboardEditor.isPanel(selectedElement)) {
          ef.displayMessage({warn: eframe.lookup('error.114.message')});
          return;
        }
        panelName = this.findPanelByID(selectedElement);
      }
      panelForDetailsDialog = panelName;

      var url = '/dashboardConfig/panelDetailsDialog';

      panelDetailsDialog = ef.displayDialog({
        bodyURL: url,
        title: 'dashboard.editor.panelDetailsDialog.title',
        width: '80%',
        height: '80%',
        messageArea: true,
        buttons: ['ok', 'cancel'],
        postScript: "dashboardEditor.loadPanelDetailsDialogValues('" + panelName + "')",
        ok: function (dialogID, button) {
          dashboardEditor.closePanelDetailsDialog(true);
          return true;
        }
      });
    },
    // Refreshes the dashboard with an info message.  If deleteFlag is true, then use the delete message.
    refreshDashboard: function (deleteFlag) {
      var msg;
      if (deleteFlag) {
        msg = eframe.lookup('default.deleted.message', [eframe.lookup('dashboard.label'), dashboardConfig.dashboard]);
      } else {
        var create = (dashboardConfig.uuid == undefined);
        msg = eframe.lookup(create ? 'default.created.message' : 'default.updated.message',
          [eframe.lookup('dashboard.label'), dashboardConfig.dashboard]);
      }
      window.location = ef.addArgToURI(window.location.href, '_info', msg);
    },
    // Removes the given button.
    removeButton: function (buttonID) {
      ef.clearMessages();
      var selectedButtonIndex = -1;

      if (!buttonID) {
        // Not passed in, so use the selected element.
        if (dashboardEditor.isButton(selectedElement)) {
          buttonID = this.getButtonIDFromView(selectedElement);
        }
      }
      if (buttonID) {
        for (var i = 0; i < dashboardConfig.buttons.length; i++) {
          if (dashboardConfig.buttons[i].buttonID == buttonID) {
            selectedButtonIndex = i;
            break;
          }
        }
      }

      if (selectedButtonIndex < 0) {
        ef.displayMessage({warn: eframe.lookup('error.118.message')});
        return;
      }
      dashboardEditor.removeButtonByIndex(selectedButtonIndex);

      // Now, rebuild the editor display completely.
      dashboardEditor.display(dashboardConfig);
      dashboardEditor.setUnsavedChanges(true);
    },
    // Removes the selected button.
    removeButtonByIndex: function (buttonIndex) {
      //console.log('Removing Button ' + buttonIndex);
      //dashboardButtons.splice(newIndex, 0, {});
      dashboardButtons.splice(buttonIndex, 1);
      dashboardConfig.buttons = dashboardEditor.convertButtonsFromHierarchy(dashboardButtons);
    },
    // Removes the currently selected panel.
    removePanel: function () {
      ef.clearMessages();
      //console.log(selectedElement);
      /*
            if (!eventPanel) {
              ef.displayMessage({warn: eframe.lookup('error.114.message')});
              return;
            }
      */
      if (!selectedElement || !dashboardEditor.isPanel(selectedElement)) {
        ef.displayMessage({warn: eframe.lookup('error.114.message')});
        return;
      }
      var selectedPanelIndex = dashboardEditor.findPanelIndexByID(selectedElement);
      //console.log('selectedPanelIndex: ' + selectedPanelIndex);
      dashboardEditor.removePanelByIndex(selectedPanelIndex);
    },
    // Removes the selected panel, recursively deleting the parent if it only has one child.
    removePanelByIndex: function (panelIndex) {
      if (dashboardConfig.dashboardPanels.length <= 1) {
        //error.116.message=Cannot delete last panel.
        ef.displayMessage({warn: eframe.lookup('error.116.message')});
        return;
      }

      // We will use a combined splitter/panel array to make sure the 'index' (sequence) values are unique and
      // assigned correctly.  The insert/remove logic is simpler with a single array to maintain since the array
      // indexes are only valid for the combined array.
      // NOTE: The 'panelIndex' values are array indices for this combined array only.

      var panels = dashboardEditor.combinePanelsAndSplitters(dashboardConfig.splitterPanels, dashboardConfig.dashboardPanels);
      var i;
      var oldParentPanelIndex = panels[panelIndex].parentPanelIndex;
      var newParentPanelIndex = panels[oldParentPanelIndex].parentPanelIndex;

      // Remove the panel and its direct splitter parent.
      panels.splice(panelIndex, 1);
      panels.splice(oldParentPanelIndex, 1);
      // Fix all panel's indexes and their parent panel to match the new layout.
      for (i = 0; i < panels.length; i++) {
        var adjustment = panels[i].panelIndex - i;
        //console.log('Panel ' + i + ' Adjustment ' + adjustment);
        if (adjustment != 0) {
          panels[i].panelIndex = i;
          if (panels[i].parentPanelIndex == oldParentPanelIndex) {
            // This panel was under the splitter we removed, so we need to move it to the grand parent splitter.
            panels[i].parentPanelIndex = newParentPanelIndex;
          } else {
            // Not a direct child of the splitter removed, so just adjust for the array movement only.
            panels[i].parentPanelIndex -= adjustment;
          }
          if (panels[i].parentPanelIndex < 0) {
            panels[i].parentPanelIndex = -1;
          }
        }
      }

      // Now, separate out the combined array again for saving in the model.
      // NOTE: The 'panelIndex' values are array indices for this combined array only.
      dashboardEditor.separatePanelsAndSplitters(panels, dashboardConfig);

      // Now, rebuild the editor display completely.
      dashboardEditor.display(dashboardConfig);
      dashboardEditor.setUnsavedChanges(true);
    },
    // Removes the given panel (by panel name).
    removePanelByName: function (panelName) {
      //console.log('Removing: ' + panelName + ', index: ' + this.findPanelIndex(panelName));
      this.removePanelByIndex(this.findPanelIndex(panelName));
    },
    // Renumbers the sequences for all buttons.
    renumberSequences: function () {
      var current = 10;
      for (var b in dashboardButtons) {
        // Just assign values to the pages.
        for (var p in dashboardButtons[b].pages) {
          dashboardButtons[b].pages[p].sequence = current;
          current += 10;
        }
      }

      // Push out to the persistence model.
      dashboardConfig.buttons = dashboardEditor.convertButtonsFromHierarchy(dashboardButtons);
      // Clear selection and re-display the dashboard.
      dashboardEditor.display(dashboardConfig);
      dashboardEditor.setUnsavedChanges(true);
    },
    // Saves the current dashboard config (if possible).
    // Displays a result message when done.  Optional callBack function when save is successful.
    save: function (callBack) {
      if (callBack == undefined) {
        dashboardEditor.clearMessages();
      }
      console.log('Saving ' + dashboardConfig.dashboard);
      console.log(dashboardConfig);
      var s = dashboardEditor.validate();
      //console.log(s);
      if (s) {
        ef.displayMessage({error: s});
        return;
      }
      var create = (dashboardConfig.uuid == undefined);
      var subPage = create ? '' : '/' + dashboardConfig.uuid;   // The URL depends on the mode
      // Send save request to server.
      var uri = '/dashboardConfig/crud' + subPage;
      //console.log(dashboardConfig);

      var method = create ? 'POST' : 'PUT';

      ef.ajax(method, uri, dashboardConfig, function (response, status, xhr) {
        dashboardEditor.setUnsavedChanges(false);
        if (callBack) {
          // Assumes the callBack will close the dialog
          //console.log(callBack);
          callBack();
        } else {
          // No callback, so assume the dialog needs to be refreshed.
          var msg = eframe.lookup(create ? 'default.created.message' : 'default.updated.message',
            [eframe.lookup('dashboard.label'), dashboardConfig.dashboard]);
          ef.displayMessage({info: msg});
          // Finally, re-load the mode so we can have proper object IDs for a save.
          dashboardEditor.load(dashboardConfig.dashboard);
        }
      });
    },
    // Saves the current dashboard config (if possible) and closes the dialog.
    // Displays a result message when done.  Optional callBack function when save is successful.
    saveAndClose: function () {
      dashboardEditor.save(function () {
        ef.closeDialog(editorDialog);
        dashboardEditor.refreshDashboard(false);
      });
    },
    // Separates the single array of panels/splitters into separate dashboardConfig's panels and splitters arrays.
    // Used to insert/remove panels with unique index values.
    separatePanelsAndSplitters: function (combinedArray, config) {
      var splitters = [];
      var panels = [];

      for (var i = 0; i < combinedArray.length; i++) {
        if (combinedArray[i].vertical == undefined) {
          panels[panels.length] = combinedArray[i];
        } else {
          splitters[splitters.length] = combinedArray[i];
        }
      }
      // Quick integrity check.
      for (i = 0; i < combinedArray.length; i++) {
        if (combinedArray[i].panelIndex != i) {
          webix.alert("Internal Issue: Element " + i + " does not have correct panelIndex " + combinedArray[i].panelIndex + ".  Array: " + JSON.stringify(combinedArray));
          break;
        }
      }
      config.dashboardPanels = panels;
      config.splitterPanels = splitters;
    },
    setDefaultCategory: function (defaultCat) {
      defaultCategory = defaultCat;
    },
    // Set the unsaved changes flag and the visual indicator for the user.
    setUnsavedChanges: function (unsaved) {
      unsavedChanges = unsaved;
      var title = tk._getTopDialogTitle();
      if (unsaved) {
        // Set the indicator, if needed.
        if (title.substring(0, 1) != '*') {
          tk._setTopDialogTitle('*' + title);
        }
      } else {
        // Clear the indicator, if needed.
        if (title.substring(0, 1) == '*') {
          tk._setTopDialogTitle(title.substring(1));
        }
      }

    },
    // Toggles the selection state of the given element.
    // De-selects the current selection, if any.
    toggleSelection: function (id) {
      var $element = $$(id);
      //console.log($element);
      if (id == selectedElement) {
        // Toggle selection on current element.
        $$(id).getNode().classList.remove(selectedStateClass);
        selectedElement = undefined;
      } else {
        // New element selected, so de-select current.
        if (selectedElement) {
          $$(selectedElement).getNode().classList.remove(selectedStateClass);
        }
        // Save current selection
        selectedElement = id;
        $$(selectedElement).getNode().classList.add(selectedStateClass);
      }
    },
    // Sets the dialog title to reflect the current dashboard.
    updateDialogTitle: function () {
      var title = unsavedChanges ? '*' : '';
      title += dashboardConfig.dashboard + ' - ' + eframe.lookup('dashboard.editor.title');
      tk._setDialogTitle(editorDialog, title);
    },
    // Performs client-side validates of the current dashboard and returns an error.
    // Most validations are server-side, but some must be performed here.
    validate: function () {
      // Check for duplicates sequence.
      // This must be done on the client since sequence is a key field.  Will overwrite some buttons.
      var buttons = dashboardConfig.buttons;
      if (buttons) {
        for (var i = 0; i < buttons.length; i++) {
          var sequence = buttons[i].sequence;
          if (sequence == undefined || sequence == '') {
            //error.120.message=Button {0} requires a sequence number
            return eframe.lookup('error.120.message', buttons[i].label);
          }
          // Compare to all others.
          for (var j = 0; j < buttons.length; j++) {
            if (i != j) {
              if (sequence == buttons[j].sequence) {
                //error.119.message=Duplicate sequence number {0} used on {1} and {2}
                return eframe.lookup('error.119.message', sequence, buttons[i].label, buttons[j].label);
              }
            }
          }
        }
      }
      return undefined;
    },
    // Simple click handler for use with std dashboard.js button.
    _clickButton: function (id) {
    },
    // Registers the configuration action needed for this definition page.
    _registerConfigAction: function () {
      ef._registerConfigAction({action: dashboardEditor.openDashboardEditor, title: 'Open Dashboard Editor'});
    }


  }
}();
dashboardEditor = efe.dashboardEditor;  // Simplified variable for access to dashboard API.



