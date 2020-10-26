<#--noinspection ALL-->
<#--@formatter:off-->
<@efPreloadMessages codes="dashboard.editor.detailsDialog.title,cancel.label,cancel.tooltip,ok.label,
                           "/>
<script>

  <@efForm id="detailsDialog" dashboard="true">
    <@efField field="DashboardConfig.dashboard" required=true/>
    <@efField field="DashboardConfig.category" />
    <@efField field="DashboardConfig.title" />
    <@efField field="DashboardConfig.defaultConfig" />
  </@efForm>

</script>

