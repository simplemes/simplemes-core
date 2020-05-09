<#--@formatter:off-->
<#--noinspection ALL-->
<script>
<#assign panel = "${params._panel}"/>
<#assign variable = "${params._variable}"/>

${variable}.provideParameters = function() {
  var order= document.getElementById("order").innerHTML;
  //console.log("provided order:"+order);
  return {
    order: order
  }
}
${variable}.provideScanParameters = function() {
  return ${variable}.provideParameters();
}
${variable}.handleEvent = function(event) {
  JL().trace(event);
  //console.log(event);
  if (event.type == 'ORDER_LSN_CHANGED') {
    ef._setInnerHTML("order",event.list[0].order);
    ef._setInnerHTML("orderStatus",${variable}.determineStatusToDisplayForOrder(event.list[0]));
  } else if (event.type == 'ORDER_LSN_STATUS_CHANGED') {
    ${variable}.updateOrderStatus(event.list);
  }
};

// Updates the displayed order status for the first order in the list.
// Queries the server for status.
${variable}.updateOrderStatus = function(list) {
  var order = list[0];
  var url = "/order/determineQtyStates/" + order.order ;
  ef.get(url, undefined, function (responseText) {
    ef._setInnerHTML("orderStatus",${variable}.determineStatusToDisplayForOrder(JSON.parse(responseText)));
  });
}

// Determines ths display string for the given status.
// Checks the qtyInQueue and qtyInWork for this check.
${variable}.determineStatusToDisplayForOrder = function(status) {
  var statusString = "...";
  if (status.qtyInWork > 0) {
    statusString = eframe.lookup('inWork.status.message', status.qtyInWork);
  } else if (status.qtyInQueue > 0) {
    statusString = eframe.lookup('inQueue.status.message', status.qtyInQueue);
  }
  return statusString;
}

${variable}.postScript = "mes_dashboard._initScanHandlers()";

<@efForm id="scanArea" dashboard="buttonHolder">
  <@efHTML spacer="before" height="1em">
    <div id="scan">
      <span class="scanHeader"><@efLookup key="scanDashboard.main.header"/>&nbsp;</span><span id="scanText"></span>
      <div class="orderBlock">
        <span id="order" class="orderDisplay">${params.order!""}</span>
          <button type="button" id="undoButton" class="undo-button-disabled" onclick="dashboard.undoAction();"
                  title="<@efLookup key="undo.title"/>" ></button>
        <br>
        <span id="orderStatus" class="orderStatusDisplay">...</span>
      </div>
      <div id="buttonLayout" style="text-align: center">
        <div id="DashboardButtons" style="padding: 8px 10px 4px">
        </div>
      </div>
    </div>
  </@efHTML>
</@efForm>


<@efPreloadMessages codes="inWork.status.message inQueue.status.message scanDashboard.couldNotFind.message"/>


</script>