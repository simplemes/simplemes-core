<#--@formatter:off-->
<script>
  <#assign panel = "${params._panel}"/>
  <#assign variable = "${params._variable}"/>

  ${variable}.provideParameters = function() {
    console.log('called');
    return {
      workCenter: 'WC237',
    }
  }
  ${variable}.changed = function(newValue) {
    dashboard.sendEvent({type: 'ORDER_LSN_CHANGED', newValue: newValue});
  }
  // Testbed for dashboard layout work
  <@efForm id="logFailure" dashboard='buttonHolder'>
  <@efField field="order" id="order" label="Order/LSN" value="M1008"
            width=20 labelWidth='35%' onChange="${variable}.changed(newValue)">
    <@efButton type='undo' id="undoButton" tooltip='undo.title' click='dashboard.undoAction();'/>
    <#--noinspection UnterminatedStatementJS-->
    <@efHTML><a href='./'>link</a>
    </@efHTML>
  </@efField>
  </@efForm>


</script>
