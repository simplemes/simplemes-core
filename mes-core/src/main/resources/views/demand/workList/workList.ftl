<#--@formatter:off-->
<script>
  <#assign panel = "${params._panel}"/>
  <#assign variable = "${params._variable}"/>

  ${variable}.onSelect = function(rowData) {
    var list = [{order: rowData.order}];
    dashboard.sendEvent({type: 'WORK_LIST_SELECTED',source: "/workList/workListActivity",  list: list});
  }
  ${variable}.handleEvent = function(event) {
    if (event.type == 'ORDER_LSN_STATUS_CHANGED') {
      tk.refreshList('workList${panel}');
    }
  }

  <@efForm id="workListForm${panel}" dashboard="true">
    <@efList id="workList${panel}" columns="order,lsn,qtyInQueue,qtyInWork,workCenter"
             uri="/workList/findWork" model="org.simplemes.mes.demand.FindWorkResponse"
             paddingX="5%" copyParameters=true
             onSelect="${variable}.onSelect(rowData, listID)" />
  </@efForm>


</script>
