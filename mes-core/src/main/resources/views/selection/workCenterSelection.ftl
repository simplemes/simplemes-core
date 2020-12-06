<#--@formatter:off-->
<script>
<#assign panel = "${params._panel}"/>
<#assign variable = "${params._variable}"/>
<#if params.workCenter?has_content>
  <#assign workCenter = "${params.workCenter}"/>
<#else>
  <#assign workCenter><@efLookup key="change.label"/></#assign>
</#if>

${variable}.handleChangeWorkCenter = function() {
  var wc = document.getElementById("workCenter").innerText;
  var url = eframe.addArgToURI('/selection/changeWorkCenterDialog', 'workCenter', wc);

  ef.displayDialog({
    bodyURL: url,
    width: '55%',
    buttons: ['ok', 'cancel'],
    <#--noinspection JSUnusedLocalSymbols-->
    ok: function (dialogID, button) {
      var workCenter = $$('wcdChangeWorkCenter').getValue();
      document.getElementById("workCenter").innerText=workCenter;
      // Now, store this as a preference for future uses.
      var postData = {page: window.location.pathname,workCenter: workCenter};
      ef.post("/selection/workCenterChanged", postData);
      // Now, let the other dashboard activities know the work center changed.
      dashboard.sendEvent({type: 'WORK_CENTER_CHANGED',source: "/selection/workCenterSelection",  workCenter: workCenter});
    }
  });
}
${variable}.orderLSNChangeByUser = function(newValue) {
  //console.log('changed '+newValue);
  var list = [{order: newValue}];
  dashboard.sendEvent({type: 'ORDER_LSN_CHANGED',source: "/selection/workCenterSelection",  list: list});
}
${variable}.provideParameters = function() {
  var wc= document.getElementById("workCenter").innerText;
  var order= document.getElementById("order").value;
  //console.log(wc+order);
  return {
    workCenter: wc, order: order
  }
}
${variable}.handleEvent = function(event) {
  if (event.type == 'WORK_LIST_SELECTED') {
    var list = event.list;
    var order = list[0].order;
    if (list.length>1) {
      order = ef.lookup('multiplesSelected.label');
    }
    document.getElementById("order").value = order;
  }
}
${params._variable}.getState =  function() {
  return {order: $$('order').getValue()};
}
${params._variable}.restoreState =  function(state) {
  if (state && state.order) {
    $$('order').setValue(state.order);
  }
}


<@efForm id="wcSelection" dashboard="buttonHolder">
  <@efField field="LSN.lsn" id="order" label="orderLsn.label" onChange="${variable}.orderLSNChangeByUser(newValue)">
    <@efHTML spacer="before" width="20%">
    </@efHTML>
    <@efButton type='undo' id="undoButton" tooltip='undo.title' click='dashboard.undoAction();'/>
    <@efHTML spacer="before" width="20%">
    <#--noinspection JSUnresolvedVariable,UnterminatedStatementJS-->
    <b><span id="workCenterLabel"><@efLookup key="workCenter.label"/></span></b>
    <#--noinspection JSUnresolvedVariable,UnterminatedStatementJS-->
    <a href='javascript:${variable}.handleChangeWorkCenter()'><span id="workCenter">${workCenter}</span></a>
    </@efHTML>
  </@efField>
</@efForm>


<@efPreloadMessages codes="multiplesSelected.label"/>

</script>