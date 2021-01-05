<#--@formatter:off-->
<script>
  <#assign panel = "${params._panel}"/>
  <#assign variable = "${params._variable}"/>

  ${variable}.provideParameters = function() {
    return {
      workCenter: 'WC237',
      ordr: $$('order').getValue(),
    }
  }
  ${variable}.changed = function(newValue) {
    dashboard.sendEvent({type: 'ORDER_LSN_CHANGED', newValue: newValue});
  }
  ${params._variable}.getState =  function() {
    return {order: $$('order').getValue()};
  }
  ${params._variable}.restoreState =  function(state) {
    if (state && state.order) {
      $$('order').setValue(state.order);
    }
  }
  // Testbed for dashboard layout work
  <@efForm id="logFailure" dashboard='buttonHolder'>
  <@efField field="order" id="order" label="Order/LSN" value="M1008" suggest="/order/suggestOrder?workCenter=WC1"
            width=20 labelWidth='35%' onChange="${variable}.changed(newValue)">
    <@efButton type='undo' id="undoButton" tooltip='undo.title' click='dashboard.undoAction();'/>
    <#--noinspection UnterminatedStatementJS-->
    <@efHTML><a onclick="tk.setSuggestURI('order','/order/suggestOrder?workCenter=WC1&ts='+new Date().getTime())">link</a>
    </@efHTML>
  </@efField>
  </@efForm>

  ${params._variable}.postScript = 'ef.focus("order")';

</script>
