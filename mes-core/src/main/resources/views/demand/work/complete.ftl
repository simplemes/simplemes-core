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
      var completeRequest = {barcode: order};
      dashboard.postActivity(completeRequest,'/work/complete','${params._panel}',{success: function(response) {
        var json = JSON.parse(response);
        var order = json[0].order;
        var qty = json[0].qty;
        var done = json[0].done;
        var msg = ef.lookup('completed.message',order,qty);
        if (done) {
          msg = ef.lookup('completedToDone.message',order,qty);
        }
        ef.displayMessage(msg);
        var list = [{order: order}];
        var event = {type: 'ORDER_LSN_STATUS_CHANGED', source: '/work/completeActivity', list: list};
        dashboard.sendEvent(event);
      }});
    } else {
      ef.displayMessage({error: ef.lookup('orderLSN.missing.message')});
    }
  }
  ${params._variable}.cache = true;
  <@efPreloadMessages codes="completed.message,completedToDone.message,orderLSN.missing.message"/>
</script>