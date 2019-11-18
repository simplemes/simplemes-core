<#assign title>Dashboard Sandbox</#assign>
<#assign head>
  <script src="<@efAsset uri="/assets/dashboard.js"/>" type="text/javascript"></script>
</#assign>

<#include "../../includes/header.ftl" />


<div id="w"></div>
<script>
  var a = {};
  var __dash;
  var __dashA;

  function start() {
    console.log('starting...');
    //var text = '{"view":"datepicker", "label":"Date"}';
    //var json = webix.DataDriver.json.toObject(text);
    // function in dynamic .js works:  webix.exec('var w=300;  a.x = function() {console.log("x()")}; _json = {view: "button", value: "List", type: "form", click: "a.x();", width: w*2}');
    // works for simple percent cases: webix.exec(' _json = {view: "button", value: "List", type: "form", click: "console.log(\'clicked\');", width: tk.pw("40%")}');

    dashboard.load({uri: '/sample/dashboard/page?view=sample/dashboard/wcSelection', panel: '_A'});
    /*
        var uri = '/sample/dashboard/page?view=sample/dashboard/wcSelection';
        uri = ef.addArgToURI(uri,'panel','_a');
        ef.get(uri,{},function(s){
          //console.log(s);
          var res = eval(s);
          console.log(__dashA);
          webix.ui(res, $$('PanelB'));
          console.log(a);
        });
    */

    //var stuff = eval('__dash = {view: "button", value: "List2", type: "form", click: "console.log(\'clicked\');", width: tk.pw("30%")}');
    //console.log(__dash);

    // Works
    //var stuff = eval('__dash = {view: "button", value: "List", type: "form", click: "console.log(\'clicked\');", width: tk.pw("30%")}');

    //webix.ui(stuff, $$('PanelB'))
    /*
        webix.ui([
          {view:"datepicker", label:"Date"},
          {view:"colorpicker", label:"Color"},
          {view:"slider"}
        ], $$('dashboardLayout'),$$('PanelB'));
    */
  }

  var form3 = [
    {
      view: "text", value: 'M1001', id: "vOrder", name: "vOrder", required: false, label: "Order",
      labelAlign: 'right', labelWidth: 200, inputWidth: 350, attributes: {maxlength: 10, id: "order"}
    },
    {
      margin: 5, cols: [
        {view: "button", value: "Start", type: "form", click: "start();", id: "start"}
      ]
    }
  ];

  function r(pos) {
    console.log(pos);
  }

  webix.ui({
    container: 'w',
    type: "space", margin: 4, id: "dashboardLayout", rows: [
      {view: "form", id: "PanelA", scroll: false, width: 400, elements: form3},
      {view: "resizer", id: "resizerA", attributes: {id: "_internalID", onclick: r}},
      {
        view: "template", id: "PanelB", height: 100, template: "Default template with some text inside",
        on: {
          onViewResize: function (arg) {
            console.log(this);
            console.log("Change!" + this.config.id);
            console.log($$("PanelB"));
            console.log($$("PanelB").$height);
          }
        }
      }

    ]
  });
  /*
    var $dash=$$("w");
    var $resizer = $$("resizerA");
    console.log($resizer);
    $$("resizerA").$view.ondragend = function(pos){
      console.log(pos);
      //... some code here ...
    };
  */

</script>

<#include "../../includes/footer.ftl" />

