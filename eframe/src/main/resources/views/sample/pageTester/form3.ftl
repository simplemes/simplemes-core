<!DOCTYPE html>
<!--suppress JSUnusedLocalSymbols -->
<html lang="en_US">
<head>
  <title>Search - EFrame</title>
  <meta charset="utf-8">
  <link rel="icon" id="favicon" type="image/x-icon" href="/assets/favicon.ico">
  <link rel="stylesheet" href="/assets/eframe.css" type="text/css">

  <!--<link rel="stylesheet" href="/assets/webix-b55b478df694269d9dc5aabab909d851.css" type="text/css">-->
  <link rel="stylesheet" href="/assets/skins/flat-bb9e248b9deb4b00b4390018c7030292.css" type="text/css">
  <!--<link rel="stylesheet" href="/assets/skins/material-6e46748800700726fa45f5c5850cf5f8.css" type="text/css">-->
  <link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.0.10/css/all.css?v=6.1.0"
        integrity="sha384-+d0P83n9kaQMCwj8F4RJB66tzIwOKmrdb46+porD/OvrJ+37WqIM7UoBtwHO6Nlg" crossorigin="anonymous">
  <script src="/assets/webix.min-0d22b982070f705fe41e8d7228b74424.js" type="text/javascript"></script>
  <script src="/assets/jsnlog.js" type="text/javascript"></script>

  <script src="/assets/eframe.js" type="text/javascript"></script>
  <script src="/assets/eframe_toolkit.js" type="text/javascript"></script>
  <script src="/assets/i18n/en-63437a3f182142de48dc6ed51182c1df.js" type="text/javascript" charset="utf-8"></script>
  <script type="text/javascript">webix.i18n.setLocale("en-US");
    webix.i18n.fullDateFormat = "%n/%j/%y %g:%i:%s %A";
    webix.i18n.dateFormat = "%n/%j/%y";
    webix.i18n.parseFormat = "%Y-%m-%d %H:%i:%s";
    webix.i18n.setLocale();

    JL().setOptions({
      "level": 4000,
      "appenders": [JL.createAjaxAppender('ajaxAppender').setOptions({
        "url": "/logging/client?logger=client.search", "level": 4000
      }),
        JL.createConsoleAppender('consoleAppender')]
    });
  </script>
  <link rel="stylesheet" href="/assets/logging.css" type="text/css">

  <script defer src="/taskMenu" type="text/javascript"></script>
</head>
<body>
<form action="logout" method="POST" id="logoutForm" name="logoutForm"></form>
<div id="h">
  <script>
    function home() {
      window.location = "/";
    }

    webix.ui({
      container: 'h',
      type: "space", margin: 0, paddingY: -8, css: 'header-bar', id: "headerToolbar", rows: [
        {
          view: "toolbar",
          elements: [
            {
              view: "button", id: "_taskMenuButton", width: 40, type: "icon", icon: "fas fa-bars",
              click: 'tk._taskMenuToggle();', tooltip: 'i18n: Task Menu'
            },
            {view: "label", template: "<span></span>"},
            {
              view: "button", id: 'configButton', width: 40, type: "icon", icon: "fas fa-cogs",
              click: 'ef._triggerConfigAction();',
              tooltip: 'i18n: Enable Configuration Buttons'
            },
            {
              view: "button", width: 40, type: "icon", icon: "fas fa-power-off",
              click: 'javascript:logoutForm.submit()', tooltip: 'i18n: Logout',
              id: "logoutButton"
            },
            {
              view: "button", width: 40, type: "htmlbutton", click: 'home();', tooltip: 'i18n: old3', css: "no-border",
              label: '<a href="/" class="toolbar-link" tabindex="-1"><span class="webix_icon fas fa-home" style="font-size: 20px;"></span></a>'
            }
          ]
        }]
    });
  </script>
</div>

<div id="messages"></div>


<div id="editFieldContent"></div>
<script>
  var editFieldFormData = [
    {
      margin: 8,
      cols: [
        {
          view: "label", id: "fieldNameLabel", label: "*Field Name",
          width: tk.pw(ef.getPageOption('labelWidth', '20%')), align: "right"
        },
        {
          view: "text", id: "fieldName", name: "fieldName", value: "", inputWidth: tk.pw("22em"), width: tk.pw("22em"),
          attributes: {maxlength: 30, id: "fieldName"}, required: true
        }, {}

      ]
    }

  ]; // form

  webix.ui({
    container: 'editFieldContent',
    type: "space", margin: 0, padding: 2, cols: [
      {
        align: "center", body: {
          rows: [

            {view: "form", id: 'editField', scroll: false, width: tk.pw('90%'), elements: editFieldFormData}
          ]
        }
      }
    ]
  });

</script>
<div>
  Search not configured in application.yml. See <a href="http://docs.simplemes.org/latest/eframe/guide.html#searching">Searching</a>
</div>
green

</body>
</html>

