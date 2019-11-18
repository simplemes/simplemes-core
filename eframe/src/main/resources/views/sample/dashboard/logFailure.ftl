<#--@formatter:off-->
<script>
  <#assign panel = "${params._panel}"/>
  <#assign variable = "${params._variable}"/>

  // TODO: Default focus mechanism?  Script here or some logic in efField (focus=true)?
  <@efForm id="logFailure" dashboard=true>
    <@efField field="rma" value="RMA1001" width=20/>
    <@efField field="product" value="CM3056857"/>
    <@efButtonGroup>
      <@efButton label="Log Failure" click="dashboard.sendEvent({type: 'ABC',otherField: 'XYZZY'});dashboard.postActivity('logFailure','/sample/dashboard/echo','${panel}');"/>
      <@efButton label="cancel.label" click="dashboard.finished('${panel}')"/>
    </@efButtonGroup>
  </@efForm>
</script>