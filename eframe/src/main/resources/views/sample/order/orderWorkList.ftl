<#--@formatter:off-->
<script>
  ${params._variable}.buildData = function() {
    return [{order: 'ABC1'},{order: 'ABC2'}];
  }
  ${params._variable}.onSelect = function(rowData, listID) {
    console.log(rowData);
    ef.displayMessage("Selected '"+rowData.order+"' in list "+listID);
  }

  <@efForm id="theOrderListForm${params._panel}" dashboard="true">
  <#if params.js??>
    <@efList id="theOrderList${params._panel}" columns="order,product,qtyInQueue,qtyInWork,workCenter"
             paddingX="5%" dataFunction="${params._variable}.buildData" copyParameters=true
             model="sample.pogo.FindWorkResponse" onSelect="${params._variable}.onSelect"/>
  <#else>
    <@efList id="theOrderList${params._panel}" columns="order,product,qtyInQueue,qtyInWork,workCenter"
             paddingX="5%" uri="/order/findWork" copyParameters=true
             model="sample.pogo.FindWorkResponse" onSelect="${params._variable}.onSelect"/>
  </#if>
  </@efForm>

  ${params._variable}.handleEvent = function(event) {
    console.log(event);
    //$$("theOrderListB").clearAll();
    //$$("theOrderListB").load("php/wpt");
  }
</script>
