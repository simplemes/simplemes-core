<#--noinspection ALL-->
<#--@formatter:off-->
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

<script>

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


  <@efForm id="addPanel" dashboard="true">
      <@efMenu id="configMenu">
        <@efMenu id="fields" label="definitionEditorMenu.customFields">
          <@efMenuItem id="addField" key="definitionEditorMenu.addCustomField" onClick="efd._editorFieldOpenAddDialog()"/>
          <@efMenuItem id="editField" key="definitionEditorMenu.editCustomField" onClick="efd._editorFieldOpenEditDialog()"/>
          <@efMenuItem/>
          <@efMenuItem id="deleteField" key="definitionEditorMenu.deleteCustomField" onClick="efd._editorFieldDeleteConfirm()"/>
        </@efMenu>
        <@efMenu id="panels" label="definitionEditorMenu.panels">
          <@efMenuItem id="addPanel" key="definitionEditorMenu.addPanel" onClick="efd._editorPanelOpenAddDialog()"/>
          <@efMenuItem id="editPanel" key="definitionEditorMenu.editPanel" onClick="efd._editorPanelOpenEditDialog()"/>
        </@efMenu>
      </@efMenu>
      ,{height: tk.ph("0.2em")},
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
  </@efForm>

  ${params._variable}.postScript = "efd._editorSetupConfigDialogHandlers()";

</script>
