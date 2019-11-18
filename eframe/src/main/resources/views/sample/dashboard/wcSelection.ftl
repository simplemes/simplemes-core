<script>
  <#assign panel = "${params._panel}"/>
  <#assign variable = "${params._variable}"/>

  // Testbed for dashboard layout work
  /*
${variable}.display = {
    view: 'form', type: 'clean', margin: 0,
    rows: [
      { 
        cols: [{
          view: "text", value: 'M1007', id: "order", name: "order", required: false, label: "Order/LSN",
          labelAlign: 'right', labelWidth: tk.pw("25%"), inputWidth: tk.pw("40%"),
          attributes: {maxlength: 30, id: "order"}
        }
          ,{view: "template", type: 'clean',width: tk.pw("8em"),template: '<a href="/">(No Work Center)</a>'   }
          ,{view: "template", type: 'clean',width: tk.pw("1.5em"), height: tk.ph("1.5em"), template: '<button type="button" class="undo-button-disabled" onclick="ef.alert(\'Alert\')" title="XYZ"/>'   }
        ]
      },
      {
        view: "form", id: "ButtonsA", type: "clean", borderless: true, elements: [
          {view: "template", id: "ButtonsContentA", template: "-"}
        ]
      }
    ]
  };
*/
  <@efForm id="logFailure" dashboard='buttonHolder'>
  <@efField field="order" id="order" label="Order/LSN" value="M1008" width=20 labelWidth='35%'>
  <@efButton type='undo' id="undoButton" tooltip='undo.title' click='dashboard.undoAction();'/>
  </@efField>
  </@efForm>


</script>
