<#--noinspection ALL-->
<#--@formatter:off-->
<@efPreloadMessages codes="save.label,noSave.label,save.tooltip,cancel.label,cancel.tooltip,ok.label,
                           saveConfirm.label, cancelConfirm.label,
                           dashboard.label,
                           dashboard.editor.title,panel.label,
                           delete.confirm.message,delete.confirm.title, delete.label, default.deleted.message,
                           unsavedChanges.title,unsavedChanges.message,
                           default.created.message,default.updated.message,
                           dashboardEditorMenu.addHorizontalSplitter.label,
                           dashboardEditorMenu.addVerticalSplitter.label,
                           dashboardEditorMenu.removePanel.label,
                           dashboardEditorMenu.addButtonBefore.label,
                           dashboardEditorMenu.addButtonAfter.label,
                           dashboardEditorMenu.removeButton.label,
                           dashboardEditorMenu.details.label,

                           error.114.message,
                           error.116.message,
                           error.118.message,
                           error.119.message,
                           error.120.message,
                           "/>
<script>

  <@efForm id="addPanel" dashboard="true">
  <@efMenu id="dashboardEditorMenu">
      <@efMenuItem key="details" onClick="dashboardEditor.openDetailsDialog();"/>
      <@efMenu id="splitters" label="dashboardEditorMenu.splitter.label">
        <@efMenuItem id="addHorizontalSplitter" key="addHorizontalSplitter" onClick="dashboardEditor.addSplitterFromMenu(false);"/>
        <@efMenuItem key="addVerticalSplitter" onClick="dashboardEditor.addSplitterFromMenu(true);"/>
      </@efMenu>
      <@efMenu id="panels" label="dashboardEditorMenu.panel.label">
        <@efMenuItem key="removePanel" onClick="dashboardEditor.removePanel();"/>
        <@efMenuItem/>
        <@efMenuItem key="panelDetails" label="details.label" onClick="dashboardEditor.openPanelDetailsDialog();"/>
      </@efMenu>
      <@efMenu id="buttons" label="dashboardEditorMenu.button.label">
        <@efMenuItem key="addButtonBefore" onClick="dashboardEditor.addButtonBefore();"/>
        <@efMenuItem key="addButtonAfter" onClick="dashboardEditor.addButtonAfter();"/>
        <@efMenuItem/>
        <@efMenuItem key="renumberButtons" onClick="dashboardEditor.renumberSequences();"/>
        <@efMenuItem/>
        <@efMenuItem key="removeButton" onClick="dashboardEditor.removeButton();"/>
        <@efMenuItem/>
        <@efMenuItem key="buttonDetails" label="details.label" onClick="dashboardEditor.openButtonDetailsDialog();"/>
      </@efMenu>
      <@efMenu id="more.menu" label="more.menu.label">
        <@efMenuItem key="create.menu" onClick="dashboardEditor.createNew();"/>
        <@efMenuItem/>
        <@efMenuItem key="duplicate.menu" onClick="dashboardEditor.duplicate();"/>
        <@efMenuItem/>
        <@efMenuItem key="delete.menu" onClick="dashboardEditor.openDeleteDialog();"/>
      </@efMenu>
      <@efMenuItem id="save" label="save.menu" rightMenu="true" onClick="dashboardEditor.save();"/>
      </@efMenu>

      {height: tk.ph("0.2em")},
      { id: "EditorPanel",
        rows: [
          {id: 'EditorContent', template: '<@efLookup key="loading.label"/>'}
        ]
      },
      {
        cols: [
          {},
          {
            view: "button", id: "save", label: ef.lookup("save.label"), tooltip: ef.lookup("save.tooltip"),
            click: "dashboardEditor.saveAndClose()", width: tk.pw("8em"), type: "iconButton", icon: 'fas fa-check'
          },
          {
            view: "button", id: "cancel", label: ef.lookup("cancel.label"), tooltip: ef.lookup("cancel.tooltip"),
            click: "dashboardEditor.cancel()", width: tk.pw("8em")
          },
          {}
        ]
      },
      {},
  </@efForm>

  var dashboardToLoad="${params.dashboard}";
  //TODO: Handle create if ("${params.mode!'new'}" == 'new') {
  //  dashboardToLoad=undefined;
  //}
  /* Use the original category for the empty config. */
  dashboardEditor.setDefaultCategory("${params.category!''}");
  dashboardEditor.load(dashboardToLoad);

</script>

