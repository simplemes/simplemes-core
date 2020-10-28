<#--noinspection ALL-->
<#--@formatter:off-->
<@efPreloadMessages codes="dashboard.editor.buttonDetailsDialog.title,cancel.label,cancel.tooltip,ok.label,
                           "/>
<script>

  <@efForm id="buttonDetailsDialog" dashboard="true">
    <@efField field="DashboardButton.buttonID" required=true/>
    <@efField field="DashboardButton.label" required=true/>
    <@efField field="DashboardButton.title" />
    <@efField field="DashboardButton.css" />
    <@efField field="DashboardButton.size" />
    <@efField field="DashboardConfig.buttons"
              columns="sequence,panel,url" addRowPrefix="_dialogContent."
              sequence@default="tk.findMaxGridValue(gridName, 'sequence')+10"/>
  </@efForm>

</script>

