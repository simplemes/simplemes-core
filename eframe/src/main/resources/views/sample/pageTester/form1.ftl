<#assign title>Form1</#assign>
<#include "../../includes/header.ftl" />
<h1>Form1 Test</h1>
Basic use of form with various inputs.
<br>
<div id="w"></div>
<script>
  function clicked() {
    //console.log(webix.send('bad', $$('theForm').getValues()));
  }

  var form3 = [
    {
      cols: [
        {view: "label", label: "Order *", autoheight: false, align: "right", width: 200},
        {view: "label", label: "M1001", autoheight: false, align: "left", gravity: 6}
      ]
    },
    {
      cols: [
        //{ align:"left", body: {view: "template",template:"Order" ,autoheight:true}},
        {view: "label", label: "Order *", autoheight: false, align: "right", width: 200},
        {
          view: "text", value: 'M2001', id: "order2", inputWidth: 150, attributes: {maxlength: 4}
        }
      ]
    },
    {
      view: "text", value: 'US', id: "plant", name: "uName", required: true, label: "Plant", readonly: false,
      disabled: false,
      labelAlign: 'right', labelWidth: 200, inputWidth: 250, attributes: {maxlength: 4}
    },
    {
      view: "text", value: '${username}', id: "uName", name: "uName", required: true, label: "User Name",
      readonly: false, disabled: true,
      labelAlign: 'right', labelWidth: 200, inputWidth: 350, attributes: {maxlength: 10, id: "userName"}
    },
    {view: "text", type: 'password', value: '123pass', label: "Passwoooooord", labelAlign: 'right', labelWidth: 200},
    {
      margin: 5, cols: [
        {view: "button", value: "Login", type: "form", click: clicked},
        {view: "button", value: "Cancel"}
      ]
    }
  ];

  webix.ui({
    container: 'w',
    type: "space", margin: 30, cols: [
      {view: "form", id: 'theForm', scroll: false, width: 400, elements: form3}
    ]
  });
</script>
<#include "../../includes/footer.ftl" />
