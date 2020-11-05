<!DOCTYPE html>
<!--suppress JSUnusedLocalSymbols -->
<html lang="<@efLanguage/>">
<head>
  <title>${title!"Unknown"}</title>
  <meta charset="utf-8">
  <link rel="icon" id="favicon" type="image/x-icon" href="<@efAsset uri="/assets/favicon.ico"/>">
  <link rel="stylesheet" href="<@efAsset uri="/assets/eframe.css"/>" type="text/css">

  <!--<link rel="stylesheet" href="<@efAsset uri="/assets/webix.css"/>" type="text/css">-->
  <link rel="stylesheet" href="<@efAsset uri="/assets/skins/flat.css"/>" type="text/css">
  <!--<link rel="stylesheet" href="<@efAsset uri="/assets/skins/material.css"/>" type="text/css">-->
  <link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.0.10/css/all.css?v=6.1.0"
        integrity="sha384-+d0P83n9kaQMCwj8F4RJB66tzIwOKmrdb46+porD/OvrJ+37WqIM7UoBtwHO6Nlg" crossorigin="anonymous">
  <script src="<@efAsset uri="/assets/webix.min.js"/>" type="text/javascript"></script>
  <script src="<@efAsset uri="/assets/jsnlog.js"/>" type="text/javascript"></script>

  <script src="<@efAsset uri="/assets/eframe.js"/>" type="text/javascript"></script>
  <script src="<@efAsset uri="/assets/eframe_toolkit.js"/>" type="text/javascript"></script>
    <@efGUISetup/>
    ${head!""}
  <script defer src="/taskMenu" type="text/javascript"></script>
  <@efAddition assets=true/>
</head>
<body>
<form action="/logout" method="POST" id="logoutForm" name="logoutForm"></form>
<div id="_header">
  <script>
    function home() {
      window.location = "/";
    }

    webix.ui({
      container: '_header',
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
              tooltip: '<@efLookup key="configurePage.label"/>'
            },
            <#if _loggedIn>
            {
              view: "button", width: 40, type: "icon", icon: "fas fa-power-off",
              click: 'javascript:logoutForm.submit()', tooltip: '<@efLookup key="logout.label"/>',
              id: "logoutButton"
            },
            </#if>
            {
              view: "button", width: 40, type: "htmlbutton", click: 'home();', tooltip: '<@efLookup key="home.label"/>',
              css: "no-border",
              label: '<a href="/" class="toolbar-link" tabindex="-1"><span class="webix_icon fas fa-home" style="font-size: 20px;"></span></a>'
            }
          ]
        }]
    });
  </script>
</div>

<@efMessages/>
