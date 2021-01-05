<#--@formatter:off-->
<script>
  <#assign panel = "${params._panel}"/>
  <#assign variable = "${params._variable}"/>
  <@efPreloadMessages codes="addComponent.title,removeComponent.title"/>

  <@efForm id="componentListForm${panel}" dashboard="true">
    <@efHTML id="XYZZY" width="90%" height="32">
      <img id="fullyAssembled" src="<@efAsset uri="/assets/assembled.png"/>" width="32" height="32" style="display: none;margin: auto; "/>
    </@efHTML>

    <@efList id="componentList${panel}" columns="sequence,componentAndTitle,assemblyDataAsString,qtyAndStateString"
             uri="/orderAssy/findComponentAssemblyState" model="org.simplemes.mes.assy.demand.OrderComponentState"
             pageSize="30" paddingX="5%" copyParameters=true
             add@buttonIcon="fa-plus-square"
             add@buttonLabel="addComponent.label"
             add@buttonHandler="${variable}.add(rowData, listID)"
             add@buttonEnableColumn="canBeAssembled"
             remove@buttonIcon="fa-minus-square"
             remove@buttonLabel="removeComponent.label"
             remove@buttonHandler="${variable}.remove(rowData, listID)"
             remove@buttonEnableColumn="canBeRemoved"
           />
  </@efForm>

  ${variable}.handleEvent = function(event) {
    if (event.type === 'ORDER_LSN_STATUS_CHANGED') {
      tk.refreshList('componentList${panel}',${variable}.newArgs,${variable}.loadFinished);
    }
    if (event.type === 'ORDER_LSN_CHANGED') {
      // Pass the new value and refresh the list
      var newArgs = {};
      if (event.list.length>0) {
        if (event.list[0].order) {
          newArgs.order = event.list[0].order;
        }
        if (event.list[0].lsn) {
          newArgs.lsn = event.list[0].lsn;
        }
      }
      ${variable}.newArgs = newArgs;
      tk.refreshList('componentList${panel}',newArgs,${variable}.loadFinished);
    } else if (event.type === 'ORDER_COMPONENT_STATUS_CHANGED') {
      // Just refresh the list.
      tk.refreshList('componentList${panel}',${variable}.newArgs,${variable}.loadFinished);
    } else if (event.type === 'DISPLAY_ASSEMBLE_DIALOG') {
      var data = {component: event.component, sequence: event.bomSequence,
        assemblyData: {flexType: event.assemblyData, uuid: event.assemblyDataUuid},
        firstAssemblyDataField: event.firstAssemblyDataField};
      ${variable}.add(data);
    }
  }
  ${variable}.loadFinished = function(listID, data, http_request) {
    if(data) {
      var json = data.json();
      ${variable}.updateFullyAssembled(json.fullyAssembled);
    }
  }

  ${variable}.updateFullyAssembled = function(fullyAssembled) {
    var element = document.getElementById('fullyAssembled');
    if (fullyAssembled) {
      element.style.display = 'block';
    } else {
      element.style.display = 'none';
    }
  }

  ${variable}.add = function(rowData) {
    var uri = '/orderAssy/assembleComponentDialog';
    uri = eframe.addArgToURI(uri, 'component', rowData.component);
    uri = eframe.addArgToURI(uri, 'bomSequence', rowData.sequence);
    var titleArgs = [rowData.component];
    var pList = dashboard.getCurrentProvidedParameters();
    for (var i=0; i< pList.length; i++ ) {
      var p = pList[i];
      if (p.order) {
        uri = eframe.addArgToURI(uri, 'order', p.order);
        titleArgs [1] = p.order;
      }
      if (p.lsn) {
        uri = eframe.addArgToURI(uri, 'lsn', p.lsn);
        titleArgs [1] = p.lsn;
      }
    }
    if(rowData.assemblyData) {
      uri = eframe.addArgToURI(uri, 'assemblyData', rowData.assemblyData.uuid);
      titleArgs[2] = rowData.assemblyData.flexType;
    }
    // Build the title
    var title = ef.lookup('addComponent.title',titleArgs);
    var focusField = '';
    if (rowData.firstAssemblyDataField) {
      focusField = rowData.firstAssemblyDataField;
    }

    //console.log(uri);
    ef.displayDialog({
      bodyURL: uri, title: title,
      width: '55%', height: '60%',
      buttons: ['assemble', 'cancel'],
      focus:focusField,
      assemble: function () {
        var values = {};
        if (rowData.assemblyData) {  // Make sure the assemblyData reference is first to allow proper parsing of the values later.
          values.assemblyData = rowData.assemblyData.uuid;
        }
        values.order = p.order;
        values.lsn = p.lsn;
        values.bomSequence = rowData.sequence;
        values = Object.assign(values, $$('assembleComponent').getValues());
        ef.postAjaxForm(values,'/orderAssy/addComponent',null,
          function(response) {
            var json = JSON.parse(response);
            // Force a refresh of the list
            var event = {
              type: 'ORDER_COMPONENT_STATUS_CHANGED',
              source: '/orderAssy/assemblyActivity',
              order: p.order,
              component: rowData.component,
              bomSequence: response.bomSequence
            };
            dashboard.sendEvent(event);
            dashboard.checkForUndoActions(json);
        });
      }
    });
  }

  ${variable}.remove = function(rowData) {
    var uri = '/orderAssy/removeComponentDialog';
    var titleArgs = [];
    var pList = dashboard.getCurrentProvidedParameters();
    for (var i=0; i< pList.length; i++ ) {
      var p = pList[i];
      if (p.order) {
        uri = eframe.addArgToURI(uri, 'order', p.order);
        titleArgs [0] = p.order;
      }
    }
    if(rowData.sequencesForRemoval) {
      var s = "";
      for (i=0; i<rowData.sequencesForRemoval.length; i++) {
        if (s.length>0) {
          s += ",";
        }
        s += rowData.sequencesForRemoval[i];
      }
      uri = eframe.addArgToURI(uri, 'sequences', s);
    }
    // Build the title
    var title = ef.lookup('removeComponent.title',titleArgs);

    //console.log(uri);
    ef.displayDialog({
      bodyURL: uri, title: title,
      width: '55%', height: '60%',
      buttons: ['remove', 'cancel'],
      remove: function () {
        // Find all selected components from the dialog.
        var sequencesToRemove = '';
        for (i=0; i<rowData.sequencesForRemoval.length; i++) {
          var j = i+1;
          var element = document.getElementById('removeComp'+j);
          if (element.checked) {
            if (sequencesToRemove.length>0) {
              sequencesToRemove += ",";
            }
            sequencesToRemove += rowData.sequencesForRemoval[i];
          }
        }

        var values = {};
        values.order = p.order;
        values.sequences = sequencesToRemove;
        console.log(values);

        ef.postAjaxForm(values,'/orderAssy/removeComponents',null,
          function(response) {
            var json = JSON.parse(response);
            // Force a refresh of the list
            var event = {
              type: 'ORDER_COMPONENT_STATUS_CHANGED',
              source: '/orderAssy/assemblyActivity',
              order: p.order,
              component: rowData.component,
              bomSequence: response.bomSequence
            };
            dashboard.sendEvent(event);
            dashboard.checkForUndoActions(json);
          });
      }
    });
  }

</script>
