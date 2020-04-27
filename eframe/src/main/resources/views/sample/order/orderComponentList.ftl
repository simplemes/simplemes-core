<#--@formatter:off-->
<script>
  ${params._variable}.buildData = function() {
    return [{order: 'ABC1', id:"12234"},{order: 'ABC2', id:"122346"}];
  }
  ${params._variable}.onSelect = function(rowData, listID) {
    //console.log(rowData);
    //console.log(listID);
    //ef.displayMessage("Selected '"+rowData.order+"' in list "+listID);
  }

  <@efForm id="componentListForm${params._panel}" dashboard="true">
    <@efList id="componentList${params._panel}" columns="sequence,component,qtyAssembled,qtyRequired"
             paddingX="5%" uri="/order/findComponents" copyParameters=true
             model="sample.pogo.FindComponentResponseDetail" onSelect="${params._variable}.onSelect(rowData,listID)"
             add@buttonIcon="fa-plus-square"
             add@buttonLabel="addComponent.label"
             add@buttonHandler="${params._variable}.add(rowData, listID)"
             add@buttonEnableColumn="canBeAssembled"
             remove@buttonIcon="fa-minus-square"
             remove@buttonLabel="remove.label"
             remove@buttonHandler="${params._variable}.remove(rowData, listID)"
             remove@buttonEnableColumn="canBeRemoved"
             />
  </@efForm>

  ${params._variable}.add = function(rowData) {
    console.log("adding: "+rowData.component);
  }
  ${params._variable}.remove = function(rowData) {
    console.log("removing: "+rowData.component);
  }
  ${params._variable}.handleEvent = function(event) {
    //console.log(event);
    if (event.type == 'ORDER_LSN_CHANGED') {
      tk.refreshList("componentList${params._panel}")
    }
  }
</script>
