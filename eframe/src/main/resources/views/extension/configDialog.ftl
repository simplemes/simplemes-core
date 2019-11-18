<script>

  <@efPreloadMessages codes="save.label,save.tooltip,cancel.label,cancel.tooltip,ok.label,
                           definitionEditor.title,definitionEditor.drag.label,
                           panels.label,customFields.label,customField.label,
                           delete.confirm.message,delete.confirm.title,
                           definitionEditorMenu.addPanel.label,definitionEditorMenu.addPanel.tooltip,
                           definitionEditorMenu.editPanel.label,definitionEditorMenu.editPanel.tooltip,
                           definitionEditorMenu.deleteCustomField.label,definitionEditorMenu.deleteCustomField.tooltip,
                           definitionEditorMenu.editCustomField.label,definitionEditorMenu.editCustomField.tooltip
                           definitionEditorMenu.addCustomField.label,definitionEditorMenu.addCustomField.tooltip,
                           error.132.message,error.135.message"/>

  var available = ${availableFields};
  var configured = ${configuredFields};

  var addRemove = {
    gravity: 10, padding: 5, type: 'space',
    cols: [
      {
        view: "list",
        id: "available",
        template: efd._editorFieldTemplate,
        type: {
          height: tk.ph("0.9em")
        },
        select: true, drag: true,
        data: available
      },
      {
        view: "list",
        id: "configured",
        template: efd._editorFieldTemplate,
        type: {
          height: tk.ph("0.9em")
        },
        select: true, drag: true,
        data: configured
      }
    ]
  };


  var maxSubMenuWidth = ef.lookup('definitionEditorMenu.addPanel.label').length;

  ${params._variable}.display = {
    type: "clean", paddingX: 6,
    rows: [
      {height: tk.ph("0.2em")},
      {
        view: "toolbar", id: "showToolbar", paddingY: -2, paddingX: 6,
        elements: [
          {
            view: "menu", id: "custom", css: 'toolbar-with-submenu', openAction: "click", type: {subsign: true},
            submenuConfig: {
              width: tk.pw(maxSubMenuWidth + 'em'),
              tooltip: function (item) {
                return item.tooltip || "??";
              }
            },
            data: [
              {
                id: "fields", value: ef.lookup("customFields.label"),
                submenu: [
                  {
                    id: "addField",
                    value: ef.lookup('definitionEditorMenu.addCustomField.label'),
                    tooltip: ef.lookup('definitionEditorMenu.addCustomField.tooltip')
                  },
                  {
                    id: "editField",
                    value: ef.lookup('definitionEditorMenu.editCustomField.label'),
                    tooltip: ef.lookup('definitionEditorMenu.editCustomField.tooltip')
                  },
                  {$template: "Separator"},
                  {
                    id: "deleteField",
                    value: ef.lookup('definitionEditorMenu.deleteCustomField.label'),
                    tooltip: ef.lookup('definitionEditorMenu.deleteCustomField.tooltip')
                  }
                ]
              }
            ],
            on: {
              onMenuItemClick: function (id) {
                if (id == "addField") {
                  efd._editorFieldOpenAddDialog();
                } else if (id == "editField") {
                  efd._editorFieldOpenEditDialog();
                } else if (id == "deleteField") {
                  efd._editorFieldDeleteConfirm();
                }
              }
            }
          },
          {
            view: "menu", id: "panels", css: 'toolbar-with-submenu', openAction: "click", type: {subsign: true},
            submenuConfig: {
              width: tk.pw(maxSubMenuWidth + 'em'),
              tooltip: function (item) {
                return item.tooltip || "??";
              }
            },
            data: [
              {
                id: "panels", value: ef.lookup("panels.label"), autowidth: true,
                submenu: [
                  {
                    id: "addPanel",
                    value: ef.lookup('definitionEditorMenu.addPanel.label'),
                    tooltip: ef.lookup('definitionEditorMenu.addPanel.tooltip')
                  },
                  {
                    id: "editPanel", value: ef.lookup('definitionEditorMenu.editPanel.label'),
                    tooltip: ef.lookup('definitionEditorMenu.editPanel.tooltip')
                  }
                ]
              }
            ],
            on: {
              onMenuItemClick: function (id) {
                if (id == "addPanel") {
                  efd._editorPanelOpenAddDialog();
                } else if (id == "editPanel") {
                  efd._editorPanelOpenEditDialog();
                }
              }
            }
          }, {}

        ]
      },
      {height: tk.ph("0.2em")},
      addRemove,
      {id: 'instructions', template: ef.lookup("definitionEditor.drag.label")},
      {
        cols: [
          {},
          {
            view: "button", id: "save", label: ef.lookup("save.label"), tooltip: ef.lookup("save.tooltip"),
            click: "efd._editorSave", width: tk.pw("8em"), type: "iconButton", icon: 'fas fa-check'
          },
          {
            view: "button", id: "cancel", label: ef.lookup("cancel.label"), tooltip: ef.lookup("cancel.tooltip"),
            click: "ef.closeDialog()", width: tk.pw("8em")
          },
          {}
        ]
      },
      {}
    ]


  };

  ${params._variable}.postScript = "efd._editorSetupConfigDialogHandlers()";

</script>
