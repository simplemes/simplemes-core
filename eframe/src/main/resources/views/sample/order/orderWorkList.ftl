<#--@formatter:off-->
<script>
  <@efForm id="theOrderListForm${params._panel}" dashboard="true">
    <@efList id="theOrderList${params._panel}" columns="order,product,qtyInQueue,qtyInWork,workCenter"
             paddingX="5%" uri="/order/findWork" copyParameters=true model="sample.pogo.FindWorkResponse"/>
  </@efForm>

  ${params._variable}.handleEvent = function(event) {
    console.log(event);
    //$$("theOrderListB").clearAll();
    //$$("theOrderListB").load("php/wpt");
  }
</script>
