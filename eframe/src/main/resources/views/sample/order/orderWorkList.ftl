<#--@formatter:off-->
<script>
  <@efForm id="theOrderListForm${params._panel}" dashboard="true">
    <@efList id="theOrderList${params._panel}" columns="order,product,qtyInQueue,qtyInWork"
             paddingX="5%" uri="/order/findWork" model="sample.pogo.FindWorkResponse"/>
  </@efForm>
</script>
