<#--@formatter:off-->
<script>
  ${params._variable}.buildData = function() {
    return [{order: 'ABC1', id:"12234"},{order: 'ABC2', id:"122346"}];
  }
  ${params._variable}.onSelect = function(rowData, listID) {
    //console.log(rowData);
    //console.log(listID);
    ef.displayMessage("Selected '"+rowData.order+"' in list "+listID);
  }

  <@efForm id="theOrderListForm${params._panel}" dashboard="true">
  <#if params.js??>
    <@efList id="theOrderList${params._panel}" columns="order,product,qtyInQueue,qtyInWork,workCenter"
             paddingX="5%" dataFunction="${params._variable}.buildData" copyParameters=true
             model="sample.pogo.FindWorkResponse" onSelect="${params._variable}.onSelect(rowData,listID)"/>
  <#else>
    <@efList id="theOrderList${params._panel}" columns="order,product,qtyInQueue,qtyInWork,workCenter"
             paddingX="5%" uri="/order/findWork" copyParameters=true
             model="sample.pogo.FindWorkResponse" onSelect="${params._variable}.onSelect(rowData,listID)"/>
  </#if>
  </@efForm>

  ${params._variable}.handleEvent = function(event) {
    //console.log(event);
    if (event.type == 'ORDER_LSN_CHANGED') {
      tk.refreshList("theOrderList${params._panel}")
    }
  }
</script>
