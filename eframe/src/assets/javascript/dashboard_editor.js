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
  var firstPanelIndex = -1;     // The first panel displayed in the GUI.
  var selectedElement;        // The currently selected element.
  var selectedStateClass = 'jqx-fill-state-pressed';  // The CSS class to use for selected elements.
  var jsonSplitterClass = 'org.simplemes.eframe.web.dashboard.DashboardPanelSplitter';  // The class name for a splitter, as needed for the JSON interface.
  var jsonPanelClass = 'org.simplemes.eframe.web.dashboard.DashboardPanel';  // The class name for a splitter, as needed for the JSON interface.
  var defaultCategory;        // The default category for a new dashboard.

  var DIV_MESSAGES = 'editorMessages';     // The ID of the div to display messages in.
  var editorDialog;           // The editor's dialog.
  var detailsDialog;          // The details dialog, if seen.
  var buttonDetailsDialog;    // The button details dialog, if seen.
  var panelDetailsDialog;     // The panel details dialog, if seen.
  var unsavedChanges = false;   // Set to true if the editor has any unsaved changes.

  //noinspection JSUnusedGlobalSymbols
  return {
    // Adds a new button (before or after selected button)
    addButton: function (after) {
      var selectedButtonIndex = -1;
      var newIndex = 0;
      var newSequence = 10;
      if (dashboardEditor.isButton(selectedElement)) {
        selectedButtonIndex = parseInt(selectedElement.attr('button-index'));
      } else {
        if (dashboardButtons.length > 0) {
          // Place before or after all buttons.
          selectedButtonIndex = after ? dashboardButtons.length - 1 : 0
        }
      }

      if (selectedButtonIndex >= 0) {
        newIndex = after ? selectedButtonIndex + 1 : selectedButtonIndex;
        newSequence = dashboardButtons[selectedButtonIndex].pages[0].sequence + (after ? 1 : -1);
      }
      //console.log('Adding '+selectedButtonIndex);
      dashboardButtons.splice(newIndex, 0, {});
      dashboardButtons[newIndex].label = 'label' + newSequence;
      // Add a page to the list of child pages.
      var pages = [];
      dashboardButtons[newIndex].pages = pages;
      pages[0] = {};
      pages[0].url = 'change';
      pages[0].panel = 'A';
      pages[0].sequence = newSequence;

      // Push out to the persistence model.
      dashboardConfig.buttons = dashboardEditor.convertButtonsFromHierarchy(dashboardButtons);
      // Clear selection and re-display the dashboard.
      dashboardEditor.display(dashboardConfig);
      dashboardEditor.setUnsavedChanges(true);
    },
    // Adds a new button after the selected button
    addButtonAfter: function () {
      dashboardEditor.addButton(true);
    },
    // Adds a new button before the selected button
    addButtonBefore: function () {
      dashboardEditor.addButton(false);
    },
    // Adds the context menus to each button.
    addButtonContextMenu: function () {
      var menus;
      menus = [{
        label: eframe.lookup('dashboardEditorMenu.addButtonBefore.label'),
        select: dashboardEditor.addButtonBefore
      },
        {
          label: eframe.lookup('dashboardEditorMenu.addButtonAfter.label'),
          select: dashboardEditor.addButtonAfter
        },
        {label: ""},
        {
          label: eframe.lookup('dashboardEditorMenu.removeButton.label'),
          select: dashboardEditor.removeButton
        },
        {label: ""},
        {
          label: eframe.lookup('dashboardEditorMenu.details.label'),
          select: dashboardEditor.openButtonDetailsDialog
        }];
      var buttonContextMenu;
      buttonContextMenu = {clickHandler: dashboardEditor.contextClickedButton, menus: menus};

      // Build a context menu handler for each button in the panel.
      eframe.defineContextMenu($("#editorDashboardButtons").find("a"), buttonContextMenu);
    },
    // Adds a Horizontal splitter to the selected panel.
    addHSplitter: function () {
      dashboardEditor.addSplitter(false);
    },
    // Adds a Vertical splitter to the selected panel.
    addVSplitter: function () {
      dashboardEditor.addSplitter(true);
    },
    addSplitter: function (vertical) {
      // The new splitter will replace the selected panel and the panel will become the first child of the new splitter.
      // New splitter takes the place of the selected panel.
      // Selected splitter is first child of new splitter.
      dashboardEditor.clearMessages();
      if (!selectedElement || !dashboardEditor.isPanel(selectedElement)) {
        dashboard.displayMessage({warn: eframe.lookup('error.114.message'), divID: DIV_MESSAGES});
        return;
      }

      var panels = dashboardConfig.dashboardPanels;
      var panelIndex = panels.length;
      var newPanelName = this.determineNextPanelName();
      var selectedPanelIndex = parseInt(selectedElement.attr('panel-index'));
      var newSplitterIndex = parseInt(selectedElement.attr('panel-index'));

      // Update the model.
      // Move the selected panel to the end of the panel array so we can put the splitter
      // in its old place.
      var oldParentIndex = panels[newSplitterIndex].parentPanelIndex;
      //console.log("oldParentIndex = " + oldParentIndex);
      var selectPanelIndex = panels.length;
      panels[selectPanelIndex] = {};
      panels[selectPanelIndex] = panels[newSplitterIndex];
      panels[selectPanelIndex].panelIndex = selectPanelIndex;
      // Now, make sure the selected panel has the splitter as it's new parent.
      panels[selectPanelIndex].parentPanelIndex = newSplitterIndex;


      // Fill in the new splitter details, using the original parent.
      panels[newSplitterIndex] = {};
      panels[newSplitterIndex].class = jsonSplitterClass;
      panels[newSplitterIndex].panelIndex = newSplitterIndex;
      panels[newSplitterIndex].panel = undefined;
      panels[newSplitterIndex].vertical = vertical;
      panels[newSplitterIndex].parentPanelIndex = oldParentIndex;
      //console.log("set parent for [" + newSplitterIndex + "] to " + oldParentIndex);
      //console.log("oldParentIndex = " + oldParentIndex);

      // Now, add the new panel at the end.
      var newPanelIndex = panels.length;
      panels[newPanelIndex] = {};
      panels[newPanelIndex].class = jsonPanelClass;
      panels[newPanelIndex].panelIndex = newPanelIndex;
      panels[newPanelIndex].parentPanelIndex = newSplitterIndex;
      panels[newPanelIndex].panel = newPanelName;

      this.logModel(dashboardConfig);

      // Clear selection and re-display the dashboard.
      dashboardEditor.display(dashboardConfig);
      dashboardEditor.setUnsavedChanges(true);
    },
    // Builds the HTML needed to display the panels defined for this dashboard.
    // Returns a string with the HTML.
    buildDashboardContent: function () {
      //console.log(dashboardConfig);
      var content;
      if (dashboardConfig.dashboardPanels.length > 1) {
        var vertical = dashboardConfig.splitterPanels[0].vertical;
        content = {
          type: "space", margin: 4, id: "EditorContent", width: tk.pw("100%"), height: tk.ph("70%")
        };
        if (vertical) {
          content.cols = dashboardEditor.buildSplitterContent(dashboardConfig.splitterPanels[0]);
        } else {
          content.rows = dashboardEditor.buildSplitterContent(dashboardConfig.splitterPanels[0]);
        }
      } else {
        var panel = dashboardConfig.dashboardPanels[0];
        content = {
          type: "space", margin: 4, id: "EditorContent", width: tk.pw("100%"), height: tk.ph("60%"), rows: [
            dashboardEditor.buildPanelContent(panel, {includeButtons: true})
          ]
        };
      }

      return content;

    },
    // Builds the JS needed to add a context menu to the a new panel.
    buildPanelContextMenu: function (panelID) {
      var js = 'eframe.defineContextMenu("' + panelID + '",{clickHandler: dashboardEditor.contextClickedPanel, menus: [';
      js += ' {label: "' + eframe.lookup('dashboardEditorMenu.addHorizontalSplitter.label') + '",';
      js += '  select: dashboardEditor.addHSplitter}\n';
      js += ',{label: "' + eframe.lookup('dashboardEditorMenu.addVerticalSplitter.label') + '",';
      js += '  select: dashboardEditor.addVSplitter}\n';
      js += ',{label: ""}\n';
      js += ',{label: "' + eframe.lookup('dashboardEditorMenu.removePanel.label') + '",';
      js += '  select: dashboardEditor.removePanel}\n';
      js += ',{label: ""}\n';
      js += ',{label: "' + eframe.lookup('dashboardEditorMenu.details.label') + '",';
      js += '  select: dashboardEditor.openPanelDetailsDialog}\n';
      js += ']});';
      return js;
    },
    // Builds the Javascript objects needed for a single splitter.  This includes the nested splitters if needed.
    // Returns a A Javascript Array of object for the splitter.
    buildSplitterContent: function (splitter) {
      var content = [];

      var panelsInSplitter = dashboardEditor.findPanelsInSplitter(splitter);
      var panel0 = panelsInSplitter[0];
      var panel1 = panelsInSplitter[1];

      content[content.length] = dashboardEditor.buildPanelContent(panel0, {
        includeButtons: true,
        resizerName: "EditorResizer0",
        firstPanel: true
      });
      content[content.length] = {view: "resizer", id: "EditorResizer0"};
      content[content.length] = dashboardEditor.buildPanelContent(panel1, {includeButtons: false});
      // TODO: Support recursive.

      return content;
    },
    // Builds the Javascript objects needed for a panel content.
    // Options supported: includeButtons, resizerName, firstPanel.
    // Returns a A Javascript Array of object for the panel.
    buildPanelContent: function (panel, options) {
      var includeButtons = options.includeButtons;
      var resizerName = options.resizerName;
      var firstPanel = options.firstPanel;
      var panelName = panel.panel;
      var templateText = ef.lookup("panel.label") + " " + panelName + ":&nbsp;&nbsp;&nbsp;" + panel.defaultURL;

      var content = {
        view: "form", id: "EditorPanel" + panelName, type: "clean", margin: 2, paddingX: 2, paddingY: 2,
        borderless: true,
        elements: [
          {view: "template", id: "EditorContent" + panelName, template: templateText},
        ]
      };
      if (firstPanel) {
        // Add a button holder for first panel only.
        content.elements[content.elements.length] = {
          view: "form", id: "ButtonsEditor" + panelName, type: "clean", borderless: true,
          elements: [{view: "template", id: "ButtonsContentEditor" + panelName, template: " "}]
        };
      }

      if (resizerName) {
        content.on = {
          onViewResize: function () {
            dashboardEditor.handleSplitterResized("EditorPanel" + panelName, panelName, resizerName, false);
          }
        };
      }
      if (panel.defaultSize) {
        content.height = tk.ph(panel.defaultSize + "%");
      }

      return content;
    },
    // Checks for changes to a simple field and returns the value for use in an assignment.
    // e.g. button.label=dashboardEditor.checkForChanges(button.label,$('#label').val());
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
          buttons: [
            {
              label: eframe.lookup('menu.save.label'), hotKey: 's',
              click: function (e) {
                dashboardEditor.save(function () {
                  done();
                });
              }
            },
            {
              label: eframe.lookup('menu.do.not.save.label'), hotKey: 'd',
              click: function (e) {
                done();
              }
            },
            {label: 'Cancel'}
          ]
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
    clickedButton: function (jqElement, event) {
      dashboardEditor.toggleSelection(jqElement);
      //console.log(event);
      if (event) {
        event.stopPropagation();
      }
    },
    // Toggles the panel selection.
    clickedPanel: function (jqElement, event) {
      dashboardEditor.toggleSelection(jqElement);
      if (event) {
        event.stopPropagation();
      }
    },
    closeButtonDetailsDialog: function (ok) {
      if (ok) {
        var selectedButtonIndex = parseInt(selectedElement.attr('button-index'));
        var button = dashboardButtons[selectedButtonIndex];
        button.label = dashboardEditor.checkForChanges(button.label, $('#label').val());
        button.title = dashboardEditor.checkForChanges(button.title, $('#title').val());
        button.style = dashboardEditor.checkForChanges(button.style, $('#style').val());
        button.buttonID = dashboardEditor.checkForChanges(button.buttonID, $('#buttonID').val());

        var p = toolkit.getGridData('buttonsList');

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
        dashboardConfig.buttons = dashboardEditor.convertButtonsFromHierarchy(dashboardButtons);
      }

      eframe.closeDialog(buttonDetailsDialog);
      dashboardEditor.display(dashboardConfig);
    },
    closeDetailsDialog: function (ok) {
      if (ok) {
        dashboardConfig.dashboard = dashboardEditor.checkForChanges(dashboardConfig.dashboard, $('#dashboard').val());
        dashboardConfig.category = dashboardEditor.checkForChanges(dashboardConfig.category, $('#category').val());
        dashboardConfig.title = dashboardEditor.checkForChanges(dashboardConfig.title, $('#title').val());
        dashboardConfig.defaultConfig = dashboardEditor.checkForChanges(dashboardConfig.defaultConfig, $('#defaultConfig').prop('checked'));
        dashboardEditor.updateDialogTitle();
      }
      eframe.closeDialog(detailsDialog);
    },
    closePanelDetailsDialog: function (ok) {
      if (ok) {
        var selectedPanelIndex = parseInt(selectedElement.attr('panel-index'));
        var panel = dashboardConfig.dashboardPanels[selectedPanelIndex];
        panel.panel = dashboardEditor.checkForChanges(panel.panel, $('#panel').val());
        panel.defaultURL = dashboardEditor.checkForChanges(panel.defaultURL, $('#defaultURL').val());
        dashboardConfig.buttons = dashboardEditor.convertButtonsFromHierarchy(dashboardButtons);
      }

      eframe.closeDialog(panelDetailsDialog);
      dashboardEditor.display(dashboardConfig);
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
        b[buttonIndex].style = button.style;
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
          b[buttonIndex].title = b[buttonIndex - 1].title;
          b[buttonIndex].style = b[buttonIndex - 1].style;
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
          b[buttonIndex].style = button.style;
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
        dashboard.displayMessage({error: s, divID: DIV_MESSAGES});
        return;
      }
      // Make sure old child records won't cause trouble on the update.
      dashboardConfig._mode = "childClear";
      // Send save request to server.
      var uri = '/dashboardConfig/crud/' + dashboardConfig.id;
      $.ajax({
        url: uri,
        type: 'delete'
      }).done(function (response, status, xhr) {
        //default.deleted.message=Deleted {0} (id={1})
        var msg = eframe.lookup('default.deleted.message', [dashboardConfig.dashboard, dashboardConfig.id]);
        dashboard.displayMessage({info: msg, divID: DIV_MESSAGES});
        dashboardEditor.clearDashboard();
      }).error(function (xhr, status, statusText) {
        var msg = 'Delete failed:' + uri + ' with status ' + statusText + ' (' + xhr.status + ') ' + xhr.responseText;
        eframe._criticalError(msg, DIV_MESSAGES);
        //console.log(xhr);
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
      firstPanelIndex = -1;
      var cfg = dashboardConfig = configIn;
      this.logModel(dashboardConfig);

      var content = this.buildDashboardContent();

      var parentViewName = 'EditorPanel';
      var contentViewName = 'EditorContent';
      $$(parentViewName).removeView(contentViewName);
      $$(parentViewName).addView({view: 'form', type: "clean", borderless: true, id: contentViewName, margin: 0, rows: [content]}, 0);
      dashboard._addButtonsIfNeededInternal('A', dashboardConfig.buttons, 'Editor');

      /*
            var script='dashboardEditor.clickedButton($(this), event)';
            var doubleClickScript='dashboardEditor.doubleClickedButton($(this), event)';
            dashboardButtons=dashboardEditor.convertButtonsToHierarchy(configIn.buttons);
            dashboard._addButtonsIfNeeded(jqDashboardEditorContent, dashboardButtons, script, doubleClickScript);
      */

      selectedElement = null;
      // Must wait for the JS in the new HTML to finish initializing the buttons.
      //setTimeout("dashboardEditor.addButtonContextMenu()", 500);
    },
    // Handles button double clicks by selecting the button, then opening the details dialog.
    doubleClickedButton: function ($element, event) {
      //event.stopPropagation();
      if (!$element.is(selectedElement)) {
        // Use normal button click logic to select this button.
        dashboardEditor.clickedButton($element, event);
        dashboardEditor.openButtonDetailsDialog();
      }
    },
    // Handles panel double clicks by selecting the panel, then opening the details dialog.
    doubleClickedPanel: function ($element, event) {
      //event.stopPropagation();
      if (!$element.is(selectedElement)) {
        // Use normal panel click logic to select this panel.
        dashboardEditor.clickedPanel($element, event);
        dashboardEditor.openPanelDetailsDialog();
      }
    },
    // Creates a new unsaved entry using the current config.
    duplicate: function () {
      // No need to check for unsaved changes since those are preserved in the copy.
      // Clear the IDs in the current definition
      dashboardConfig.id = undefined;
      for (var i = 0; i < dashboardConfig.dashboardPanels.length; i++) {
        dashboardConfig.dashboardPanels[i].id = undefined;
      }
      for (i = 0; i < dashboardConfig.buttons.length; i++) {
        dashboardConfig.buttons[i].id = undefined;
      }

      // Make a new title
      dashboardConfig.dashboard = 'COPY ' + dashboardConfig.dashboard;
      dashboardEditor.updateDialogTitle();
      dashboardEditor.setUnsavedChanges(true);
    },
    // Finds the two panels in the given splitter for the current dashboardConfig.
    findPanelsInSplitter: function (splitter) {
      var res = [];
      for (var i = 0; i < dashboardConfig.dashboardPanels.length; i++) {
        if (dashboardConfig.dashboardPanels[i].parentPanelIndex == splitter.panelIndex) {
          res[res.length] = dashboardConfig.dashboardPanels[i];
        }
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
        console.log(dashboardConfig);
      }
    },
    // Determines if the jquery element is a dashboard button.
    isButton: function ($element) {
      return ($element != null && $element.length && $element[0].nodeName == 'A');
    },
    // Determines if the jquery element is a dashboard panel.
    isPanel: function ($element) {
      return ($element[0].nodeName == 'DIV');
    },
    // Determines if the given panel is a splitter.
    isSplitter: function (panel) {
      return (panel.class == jsonSplitterClass);
    },
    // Loads the dashboard config.
    load: function (dashboardName) {
      if (dashboardName != undefined) {
        // Read the Config into local memory.
        var uri = '/dashboardConfig/crud/' + dashboardName;
        ef.get(uri, {}, function (responseText) {
          var data = JSON.parse(responseText);
          dashboardButtons = dashboardEditor.convertButtonsToHierarchy(data.buttons);
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
    loadButtonDetailDialogValues: function () {
      var selectedButtonIndex = parseInt(selectedElement.attr('button-index'));
      var button = dashboardButtons[selectedButtonIndex];
      //$('#sequence').val(button.sequence);
      $('#label').val(button.label);
      $('#title').val(button.title);
      $('#style').val(button.style);
      $('#buttonID').val(button.buttonID);

      for (var i = 0; i < button.pages.length; i++) {
        var value;
        value = {};
        value.sequence = button.pages[i].sequence;
        value.url = button.pages[i].url;
        value.panel = button.pages[i].panel;
        //grid.dataSource.add(value);
        eframe.addGridRow('buttonsList', value);
      }
      // Must delay focus since the dialog may not be fully visible here.
      window.setTimeout("$('#label').focus()", 200);
    },
    // Loads the dashboard config into the details dialog fields.
    loadDetailDialogValues: function () {
      $('#dashboard').val(dashboardConfig.dashboard);
      $('#category').val(dashboardConfig.category);
      $('#title').val(dashboardConfig.title);
      $('#defaultConfig').prop('checked', dashboardConfig.defaultConfig);
      //$('#dashboard').focus();
      // Must delay focus since the dialog may not be fully visible here.
      window.setTimeout("$('#dashboard').focus()", 200);
    },
    loadPanelDetailDialogValues: function () {
      var selectedPanelIndex = parseInt(selectedElement.attr('panel-index'));
      var panel = dashboardConfig.dashboardPanels[selectedPanelIndex];
      $('#panel').val(panel.panel);
      $('#defaultURL').val(panel.defaultURL);

      // Must delay focus since the dialog may not be fully visible here.
      window.setTimeout("$('#panel').focus()", 200);
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

      console.log(padding + "Splitter " + splitter.panelIndex + ' parent=' + splitter.parentPanelIndex);
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
        title: 'dashboardEditor.title',
        width: '95%',
        height: '98%',
        messageArea: true,
        buttons: [],
        beforeClose: function (dialogID, action) {
          dashboardEditor.checkForUnsavedChanges(function () {
            eframe.closeDialog(dialogID)
          }, event);
          return false;
        }
      });
    },
    // Opens the buttons details dialog
    openButtonDetailsDialog: function () {
      if (!selectedElement || !dashboardEditor.isButton(selectedElement)) {
        dashboard.displayMessage({warn: eframe.lookup('error.118.message'), divID: DIV_MESSAGES});
        return;
      }
      //console.log(msg);
      var url = '/dashboardConfig/buttonDetailsDialog';

      var dlgMap;
      dlgMap = {
        contentsURL: url,
        name: 'DashboardEditorButtonsDialog',
        width: '80%',
        height: '80%',
        displayed: function () {
          dashboardEditor.loadButtonDetailDialogValues();
        }
      };
      buttonDetailsDialog = eframe.displayDialog(dlgMap);
    },
    // Opens the delete dialog
    openDeleteDialog: function () {
      /*
       delete.confirm.cancel.label=Cancel
       delete.confirm.delete.label=Delete
       delete.confirm.dialog.title=Confirm Delete
       delete.confirm.message=Ok to delete {0} {1}?
       */

      eframe._confirmDialog(eframe.lookup('delete.confirm.dialog.title'),
        eframe.lookup('delete.confirm.message', eframe.lookup('dashboard.label'), dashboardConfig.dashboard),
        eframe.lookup('delete.confirm.delete.label'),
        eframe.lookup('delete.confirm.cancel.label'),
        dashboardEditor.deleteDashboard);
    },
    // Opens the details dialog
    openDetailsDialog: function () {
      //console.log(msg);
      var url = '/dashboardConfig/detailsDialog';

      var dlgMap;
      dlgMap = {
        contentsURL: url,
        name: 'DashboardEditorDetailsDialog',
        width: '80%',
        height: '80%',
        displayed: function () {
          dashboardEditor.loadDetailDialogValues();
        }
      };
      detailsDialog = eframe.displayDialog(dlgMap);
    },
    openPanelDetailsDialog: function () {
      if (!selectedElement || !dashboardEditor.isPanel(selectedElement)) {
        dashboard.displayMessage({warn: eframe.lookup('error.114.message'), divID: DIV_MESSAGES});
        return;
      }

      var url = '/dashboardConfig/panelDetailsDialog';

      var dlgMap;
      dlgMap = {
        contentsURL: url,
        name: 'DashboardEditorPanelDialog',
        width: '80%',
        height: '80%',
        displayed: function () {
          dashboardEditor.loadPanelDetailDialogValues();
        }
      };
      panelDetailsDialog = eframe.displayDialog(dlgMap);
    },
    // Removes the selected button.
    removeButton: function () {
      dashboardEditor.clearMessages();
      if (!selectedElement || !dashboardEditor.isButton(selectedElement)) {
        dashboard.displayMessage({warn: eframe.lookup('error.118.message'), divID: DIV_MESSAGES});
        return;
      }
      var selectedButtonIndex = parseInt(selectedElement.attr('button-index'));
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
    // Removes the selected panel.
    removePanel: function () {
      dashboardEditor.clearMessages();
      if (!selectedElement || !dashboardEditor.isPanel(selectedElement)) {
        dashboard.displayMessage({warn: eframe.lookup('error.114.message'), divID: DIV_MESSAGES});
        return;
      }
      if (dashboardConfig.dashboardPanels.length <= 1) {
        //error.116.message=Cannot delete last panel.
        dashboard.displayMessage({warn: eframe.lookup('error.116.message'), divID: DIV_MESSAGES});
        return;
      }
      var selectedPanelIndex = parseInt(selectedElement.attr('panel-index'));
      dashboardEditor.removePanelByIndex(selectedPanelIndex);

      //console.log(selectedPanelIndex);
      //console.log(dashboardConfig);
      dashboardEditor.logModel(dashboardConfig);

      // Now, rebuild the editor display completely.
      dashboardEditor.display(dashboardConfig);
      dashboardEditor.setUnsavedChanges(true);
    },
    // Removes the selected panel, recursively deleting the parent if it only has one child.
    removePanelByIndex: function (panelIndex) {
      var i;
      var panels = dashboardConfig.dashboardPanels;
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

      // Keep the JSON model up to date with the display model.
      dashboardConfig.buttons = dashboardEditor.convertButtonsFromHierarchy(dashboardButtons);
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
        dashboard.displayMessage({error: s, divID: DIV_MESSAGES});
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
          callBack();
        } else {
          // No callback, so assume the dialog needs to be refreshed.
          var msg = eframe.lookup(create ? 'default.created.message' : 'default.updated.message',
            [eframe.lookup('dashboard.label'), dashboardConfig.dashboard]);
          dashboard.displayMessage({info: msg, divID: DIV_MESSAGES});
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
        var create = (dashboardConfig.uuid == undefined);
        var msg = eframe.lookup(create ? 'default.created.message' : 'default.updated.message',
          [eframe.lookup('dashboard.label'), dashboardConfig.dashboard]);
        ef.displayMessage({info: msg});
      });
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
    toggleSelection: function ($element) {
      //console.log($element);
      if ($element.is(selectedElement)) {
        // Toggle selection on current element.
        toolkit._removeClass(selectedElement, selectedStateClass);
        selectedElement = null;
      } else {
        // New element selected, so de-select current.
        if (selectedElement) {
          toolkit._removeClass(selectedElement, selectedStateClass);
        }
        // Save current selection
        selectedElement = $element;
        toolkit._addClass(selectedElement, selectedStateClass);
      }
    },
    // Sets the dialog title to reflect the current dashboard.
    updateDialogTitle: function () {
      var title = unsavedChanges ? '*' : '';
      title += dashboardConfig.dashboard + ' - ' + eframe.lookup('dashboard.editor.title');
      tk._setTopDialogTitle(title);
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
    // Registers the configuration action needed for this definition page.
    _registerConfigAction: function () {
      ef._registerConfigAction({action: dashboardEditor.openDashboardEditor, title: 'Open Dashboard Editor'});
    }


  }
}();
dashboardEditor = efe.dashboardEditor;  // Simplified variable for access to dashboard API.




