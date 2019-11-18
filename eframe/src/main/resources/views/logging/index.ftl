<#assign title><@efTitle label="logging.title"/></#assign>
<#assign head>
  <link rel="stylesheet" href="<@efAsset uri="/assets/logging.css"/>" type="text/css">
</#assign>

<#include "../includes/header.ftl" />

<@efPreloadMessages codes="ok.label,cancel.label,addLogger.title,addLogger.tooltip,logger.label,removeLogger.tooltip,others.label"/>

<div id="loggerDiv"></div>
<!--suppress JSUnusedLocalSymbols, JSReferencingMutableVariableFromClosure -->
<script type="text/javascript" charset="utf-8">
  var nextID = 9999;   // For added logger, the ID to use.

  /**
   * Toggles the logger state for one level on one item.
   * @param id The element ID.
   * @param name The name of the logger level.
   */
  function toggleLoggerState(id, name) {
    var idOnly = id.substring(id.indexOf('-') + 1);

    var x = $$("logger").data.find(function (obj) {
      //console.log("comp "+obj.id +' to '+ idOnly +' = '+(obj.id == idOnly));
      return obj.id == idOnly;
    });

    var element = document.getElementById(id);
    var levelToSend;
    // Are we turning this level on or clearing it?
    if (element.classList.contains(name + "-on")) {
      levelToSend = 'clear';
    } else {
      levelToSend = name;
    }
    // Now, set all levels to off (or on for the selected one).
    var levels = ['error', 'warn', 'info', 'debug', 'trace'];
    for (var i = 0; i < levels.length; i++) {
      var level = levels[i];
      var on = false;
      var classList = document.getElementById(id).classList;
      if (level == name) {
        on = !(classList.contains(name + '-on'));
      }
      //console.log(level+ ' set to '+on);
      if (on) {
        classList.add(level + '-on');
      } else {
        classList.remove(level + '-on');
      }
    }

    //element.classList.toggle(name + "-on");
    if (x[0]) {
      var postData = {};
      postData.logger = x[0].title;
      postData.level = levelToSend;

      ef.post("/logging/setLoggingLevel", postData, function (responseText) {
        var newState = JSON.parse(responseText);
        updateState(x[0].title, newState);
        //console.log(newState);
        //console.log(x[0].title);
        // Sets the
      });
    }
  }

  /**
   * Updates the display state for a single row in the tree table.
   *
   * @param title The Row's title (class name).
   * @param state The state of the row.
   */
  function updateState(title, state) {
    // Find the existing data.
    var row = findRow(title);
    if (row) {
      row.effective = state.effective;
      row.error = state.error;
      row.warn = state.warn;
      row.info = state.info;
      row.debug = state.debug;
      row.trace = state.trace;
      $$("logger").refresh();
    }
  }

  /**
   * Finds the row in the table for the given title.
   *
   * @param title The Row's title (class name).
   */
  function findRow(title) {
    var x = $$("logger").data.find(function (obj) {
      return obj.title == title;
    });
    return x[0];
  }

  /**
   * Sends the current tree state to the preferences controller for the next time this page is displayed.
   *
   */
  function sendCurrentTreeState() {
    var expandedKeys = '';
    var openItems = $$("logger").getOpenItems();
    for (var i = 0; i < openItems.length; i++) {
      //console.log(openItems[i]);
      var openItem = openItems[i];
      var x = $$("logger").data.find(function (obj) {
        return obj.id == openItem;
      });
      if (x[0]) {
        //console.log(x[0]);
        if (expandedKeys.length > 0) {
          expandedKeys += ',';
        }
        expandedKeys += x[0].title;
      }
    }

    var postData = {};
    postData.event = "TreeStateChanged";
    postData.pageURI = window.location.pathname;
    postData.element = 'logger';
    postData.expandedKeys = expandedKeys;
    //console.log(postData);

    ef.post("/userPreference/guiStateChanged", postData);

  }

  /**
   * Generates a single logger state HTML for the display.
   * @param id The ID of the logger element.
   * @param name The logger level name.
   * @param state The current stated (on or off).
   * @param effective If true, then this is the effective value (from a parent logger).
   * @returns {string} The HTML.
   */
  function loggerState(id, name, state, effective) {
    if (state != undefined) {
      var effClass = "";
      if (effective == name) {
        effClass = "logger-effective";
      }
      var onClass = "";
      if (state == 'on') {
        onClass = name + '-on'
      }
      var htmlID = "id='" + name + "-" + id + "'";
      var onClick = "onClick='toggleLoggerState(\"" + name + "-" + id + "\",\"" + name + "\")'";
      return "<span " + htmlID + "class='logger-state " + onClass + " " + effClass + "' " + onClick + " >" + name + "</span>";
    } else {
      return "";
    }
  }

  /**
   * Generates logger state HTML for each of the support logger levels
   * @param obj The row in the tree data.
   * @param common The common logger functions.
   * @returns {string} The HTML.
   */
  function loggerStates(obj, common) {
    var effective = obj.effective;
    var id = obj.id;
    return loggerState(id, 'error', obj.error, effective) + loggerState(id, 'warn', obj.warn, effective) +
      loggerState(id, 'info', obj.info, effective) + loggerState(id, 'debug', obj.debug, effective) +
      loggerState(id, 'trace', obj.trace, effective);
  }

  /**
   * Called when the add 'other' logger level is clicked.
   * @param id The ID of the other element.
   */
  function loggerAdd(id) {
    ef.displayTextFieldDialog({
      title: "addLogger.title", value: 'org.simplemes.eframe', label: 'logger.label',
      textOk: function (value) {
        var postData = {logger: value};
        ef.post("/logging/addOtherLogger", postData, function (responseText) {
          var newLogger = JSON.parse(responseText);
          newLogger.title = value;
          newLogger.id = ($$('logger').data.length + 1) + "";
          newLogger.id = nextID + '';
          nextID++;
          var parent = findRow(ef.lookup('others.label'));
          $$('logger').add(newLogger, 0, parent.id);
        });
        //return true;
      }
    });
  }

  /**
   * Called when the user clicks the remove button of the other element.
   * @param id The ID of the element to remove.
   */
  function loggerRemove(id) {
    var x = $$("logger").data.find(function (obj) {
      return obj.id == id;
    });
    if (x[0]) {
      var postData = {logger: x[0].title};
      ef.post("/logging/removeOtherLogger", postData, function (responseText) {
        $$("logger").remove(id);
      });
    }
  }

  /**
   * Creates the main logger title entry.
   * @param obj The tree object row.
   * @param common The common tree features.
   * @returns {string} The HTML for the display.
   */
  function loggerTitle(obj, common) {
    var icons = "";
    var id = obj.id;
    var htmlID = 'id="title-' + id + '"';
    if (obj.add) {
      var tooltip1 = ' title="' + ef.lookup('addLogger.tooltip') + '"';
      var onClick1 = " onClick='loggerAdd(\"" + id + "\")'";
      icons = "<span id='addLoggerButton' class='webix_icon wxi-plus-circle logger-add' " + onClick1 + tooltip1 + "></span>";
    }
    if (obj.remove) {
      var removeHTMLID = ' id="remove-' + id + '"';
      var tooltip2 = ' title="' + ef.lookup('removeLogger.tooltip') + '"';
      var onClick2 = "onClick='loggerRemove(\"" + id + "\")'";
      icons += "<span class='webix_icon wxi-minus-circle  logger-remove'" + removeHTMLID + onClick2 + tooltip2 + "></span>";
    }

    return common.icon(obj, common) +
      common.folder(obj, common) + "<span " + htmlID + ">" + obj.title + "</span>" + icons;
  }

  var gird = webix.ui({
      container: "loggerDiv",
      rows: [
        {
          view: "treetable",
          id: 'logger',
          columns: [
            {
              id: "title", header: "Logger", width: tk.pw("55%"),
              template: loggerTitle
            },
            {id: "state", header: "State", width: tk.pw("35%"), template: loggerStates}
          ],
          autoheight: true,
          autowidth: true,
          activeTitle: true,
          data: ${treeJSON}
        }
      ]
    })
  ;
  $$("logger").attachEvent("onAfterOpen", function (id) {
    sendCurrentTreeState();
  });
  $$("logger").attachEvent("onAfterClose", function (id) {
    sendCurrentTreeState();
  });
  ef.loadDialogPreferences();

</script>

<#include "../includes/footer.ftl" />

