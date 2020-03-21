<#--@formatter:off-->
<script>
  ${params._variable}.execute =  function() {
    // Find the current order/LSN.
    var params = dashboard.getCurrentProvidedParameters();
    var order = undefined;
    for (var i=0; i<params.length; i++) {
      var map = params[i];
      if (map.order) {
        order = map.order;
      }
    }
    if (order) {
      var startRequest = {barcode: order};
      dashboard.postActivity(startRequest,'/work/start','${params._panel}',{success: function(response) {
        var json = JSON.parse(response);
        var order = json[0].order;
        var qty = json[0].qty;
        var msg = ef.lookup('started.message',order,qty);
        ef.displayMessage(msg);
        var list = [{order: order}];
        var event = {type: 'ORDER_LSN_STATUS_CHANGED', source: '/work/startActivity', list: list};
        dashboard.sendEvent(event);
      }});
    } else {
      ef.displayMessage({error: ef.lookup('orderLSN.missing.message')});
    }
  }
  ${params._variable}.cache = true;
  <@efPreloadMessages codes="started.message,orderLSN.missing.message"/>
</script>