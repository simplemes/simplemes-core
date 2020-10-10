<#--noinspection ALL-->
<#--@formatter:off-->
<@efPreloadMessages codes="save.label,save.tooltip,cancel.label,cancel.tooltip,ok.label,
                           dashboard.label,
                           dashboard.editor.title,panel.label,
                           default.created.message,default.updated.message
                           "/>
<script>

  <@efForm id="addPanel" dashboard="true">
  <@efMenu id="dashboardEditorMenu">
      <@efMenuItem key="details" onClick="dashboardEditor.openDetailsDialog();"/>
      <@efMenu label="dashboardEditorMenu.splitter.label">
        <@efMenuItem key="addHorizontalSplitter" onClick="dashboardEditor.addSplitter(false);"/>
        <@efMenuItem key="addVerticalSplitter" onClick="dashboardEditor.addSplitter(true);"/>
      </@efMenu>
      <@efMenu label="dashboardEditorMenu.panel.label">
        <@efMenuItem key="removePanel" onClick="dashboardEditor.removePanel();"/>
        <@efMenuItem/>
        <@efMenuItem key="details" onClick="dashboardEditor.openPanelDetailsDialog();"/>
      </@efMenu>
      <@efMenu label="dashboardEditorMenu.button.label">
        <@efMenuItem key="addButtonBefore" onClick="dashboardEditor.addButtonBefore();"/>
        <@efMenuItem key="addButtonAfter" onClick="dashboardEditor.addButtonAfter();"/>
        <@efMenuItem/>
        <@efMenuItem key="renumberButtons" onClick="dashboardEditor.renumberSequences();"/>
        <@efMenuItem/>
        <@efMenuItem key="removeButton" onClick="dashboardEditor.removeButton();"/>
        <@efMenuItem/>
        <@efMenuItem key="details" onClick="dashboardEditor.openButtonDetailsDialog();"/>
      </@efMenu>
      <@efMenu key="more.menu">
        <@efMenuItem key="create.menu" onClick="dashboardEditor.createNew();"/>
        <@efMenuItem/>
        <@efMenuItem key="duplicate.menu" onClick="dashboardEditor.duplicate();"/>
        <@efMenuItem/>
        <@efMenuItem key="delete.menu" onClick="dashboardEditor.openDeleteDialog();"/>
      </@efMenu>
      <@efMenuItem label="save.menu" rightMenu="true" onClick="dashboardEditor.save();"/>
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
            click: "ef.closeDialog()", width: tk.pw("8em")
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

