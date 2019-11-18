<#assign title>Form2</#assign>
<#include "../../includes/header.ftl" />
<!--suppress JSUnusedLocalSymbols -->
<div id="w"></div>
<!--suppress JSUnusedGlobalSymbols -->
<script>
  function clicked() {
    var values = $$('theForm').getValues();
    console.log(values);

  }


  var form3 = [
    {
      margin: 6,
      scroll: "y",
      cols: [
        {view: "label", label: "Order *", align: "right", width: 300},
        {view: "text", value: 'M2001', id: "order2Key", inputWidth: 150, attributes: {maxlength: 4}}
      ]
    },
    {
      view: "tabview", id: 'theTabView', paddingX: 10,
      tabbar: {
        tabMargin: 6,
        on: {
          onChange: function (x) {
            ef._storeLocal('theTabView', x);
          }
        },
        bottomPadding: -10
      },
      multiview: {
        fitBiggest: true
      },
      cells: [
        {
          header: 'Main',
          body: {
            id: 'mainBody',
            rows: [
              {
                margin: 6,
                cols: [
                  {view: "label", label: "Title", align: "right", width: 200},
                  {view: "text", value: 'm2001', id: "title", inputWidth: 350, attributes: {maxlength: 80}}
                ]
              },
              {
                margin: 6,
                cols: [
                  {view: "label", label: "Quantity To Build*", align: "right", width: 200},
                  {view: "text", value: '12.2', id: "qtyToBuild", inputWidth: 150, attributes: {maxlength: 4}}
                ]
              },
              {
                margin: 6,
                cols: [
                  {view: "label", label: "Products*", autoheight: false, align: "right", width: 200},
                  {
                    view: "datatable",
                    id: 'theTable',
                    //autoConfig:true,
                    height: 150,
                    css: "webix_header_border",
                    headerRowHeight: tk.pw('2em'),
                    resizeColumn: {size: 6, headerOnly: true},
                    dragColumn: true,
                    //width: 300,
                    editable: true,
                    select: 'row',
                    //autowidth: true,
                    gravity: 6,
                    columns: [
                      {id: "rank", editor: "text", header: {text: "#"}, css: "rank"},
                      {id: "title", editor: "text", header: {text: "Title"}, adjust: "data"},
                      {id: "year", editor: "text", header: {text: "Year"}},
                      {
                        id: "details", editor: "text", header: {text: "details"}, template: function (obj) {
                          //console.log(obj);
                          return obj.details.name
                        }
                      },
                      {
                        id: "votes", editor: "combo", options: [{id: '1', value: '1a'}, {id: '2', value: '2a'}],
                        header: {text: "Votes"}
                      }
                    ],
                    data: [
                      {
                        id: '0', title: "My Fair Lady1", year: 1964, votes: '1', rating: 8.9, rank: 1,
                        details: {name: 'ABC'}
                      },
                      {
                        id: '1', title: "My Fair Lady2", year: 1964, votes: '2', rating: 8.9, rank: 2, selecte: true,
                        details: {name: 'XYZ'}
                      }
                    ]
                  },
                  {
                    margin: 0, rows: [{
                      view: "button", width: 40, height: 40, type: "icon", icon: "fas fa-plus-square", align: 'top',
                      click: '_gridAddRow($$("theTable"))'
                    },
                      {
                        view: "button", width: 40, height: 40, type: "icon", icon: "fas fa-minus-square", align: 'top',
                        click: '_gridDeleteRow()'
                      }]
                  }

                ]
              },
              {
                margin: 6,
                cols: [
                  {view: "label", label: "Quantity To Build*", align: "right", width: 200},
                  {view: "text", value: '12.2', id: "qtyToBuild2", inputWidth: 150, attributes: {maxlength: 4}}
                ]
              },
              {
                margin: 6,
                cols: [
                  {view: "label", label: "Enabled", align: "right", width: 200},
                  {
                    view: "multiComboEF", width: 300,
                    id: "fruit", name: 'fruit', editable: true,
                    value: "1,3",
                    /*
                                        $prepareValue: function (v) {
                                          console.log(v);
                                          return v + ",";
                                        },
                                        on: {
                                          'onChange': function (id, e2) {
                                            var v = $$("fruit").getInputNode().value;
                                            $$("fruit").getInputNode().value = 'Old,' + v;
                                            //console.log(e2);
                                            //console.log(this.getValue());
                                            //this.setValue(this.getValue()+',')
                                          }
                                        },
                                        click: function (id, event) {
                                          //console.log(this);
                                          return false;
                                        },
                    */
                    options: [
                      {id: "3", value: "Banana"},
                      {id: "2", value: "Papaya"},
                      {id: "1", value: "Apple"}
                    ]
                  }
                ]
              },
              {
                margin: 6,
                cols: [
                  {view: "label", label: "Due Date*", align: "right", width: 200},
                  {view: "datepicker", id: "dueDate", editable: true, inputWidth: 250, value: "2018-06-04"},
                  {view: "button", value: "Check", width: 200, click: clicked, align: "left"},
                  {}

                ]
              }

            ]
          }
        },
        {
          header: 'Other',
          body: {
            id: 'otherBody',
            rows: [
              {view: "text", name: "value2", label: "value2"},
              {view: "text", value: '123pass', label: "Passwoooooord", labelAlign: 'right', labelWidth: 200},
              {view: "text", name: "value3", label: "value2"}
            ]
          }
        }
      ]
    },
    {
      margin: 15, cols: [
        {},
        {view: "button", value: "Create", width: 200, type: "form", click: clicked},
        {}
      ]
    }
  ];

  webix.ui({
    container: 'w',
    type: "space", margin: 0, padding: 2, cols: [
      {
        align: "center", /*height: tk.ph('75%'),*/body: {
          rows: [
            {
              view: "toolbar", paddingY: -2,
              elements: [
                {
                  view: "button", width: tk.pw('10%'), type: "icon", icon: "fas fa-th-list", click: 'home();',
                  label: 'List', tooltip: 'i18n: List Page'
                },
                {
                  view: "button", width: tk.pw('10%'), type: "icon", icon: "fas fa-plus-square", click: 'home();',
                  label: 'Create', tooltip: 'i18n: Create'
                },
                {
                  view: "button", width: tk.pw('10%'), type: "icon", icon: "fas fa-edit", click: 'home();',
                  label: 'Edit', tooltip: 'i18n: Edit'
                },
                {
                  view: "menu", css: 'toolbar-with-submenu',
                  openAction: "click",
                  data: [
                    {id: "3", value: "More...", submenu: [{id: 'delete', value: "Delete..."}]}
                  ],
                  type: {subsign: true},
                  on: {
                    onMenuItemClick: function (id) {
                      if (id == 'delete') {
                        console.log('delete clicked');
                      }
                    }
                  }
                }
              ]
            },
            {view: "form", id: 'theForm', scroll: false, width: tk.pw('90%'), elements: form3}
          ]

        }
      }
    ]
  });
  var selectedTab = ef._retrieveLocal('theTabView');
  if (selectedTab) {
    $$("theTabView").getTabbar().setValue(selectedTab);  // Sets the current tab.
  }
  $$("order2Key").focus();

  function _gridForwardTabHandler(view) {
    return _gridTabHandler(view, 1);
  }

  function _gridBackwardTabHandler(view) {
    return _gridTabHandler(view, -1);
  }

  function _gridTabHandler(view, direction) {
    //console.log(view);
    //console.log(webix.UIManager.getNext($$("theTable")));
    var ed = view.getEditor();
    if (ed) {
      var rowId = ed.row;
      var colId = ed.column;
      // Move forward or backward.
      if (direction == 1) {
        //colId=colId+1;
      } else {
        //colId=colId-1;
      }
      //console.log(colId);
      //console.log(rowId);
      //console.log(view.getPrevId(rowId));
      //console.log(view.getNextId(rowId));
      var column = view.config.columns[view.getColumnIndex(colId) + direction];
      //console.log(column);
      //console.log('row '+rowId+' exists: '+view.exists(rowId)+ ', prevId: '+view.getPrevId(rowId)+', nextId: '+view.getNextId(rowId));
      //console.log('col '+colId+' exists: '+view.exists(colId)+ ', prevId: '+view.getPrevId(colId)+', nextId: '+view.getNextId(colId));
      //console.log(view.data);
      if (direction == 1 && view.getNextId(rowId) == undefined && column == undefined) {
        view.editStop();
        return true;
      } else if (direction == -1 && view.getPrevId(rowId) == undefined && column == undefined) {
        view.editStop();
        return true;
      }
      if (!view.exists(colId)) {
        //console.log('!exists');
        // Column is not on this row, so try the row before/after.
        if (direction == 1) {
        } else {
        }
      }
      //return true;
    } else {

      return true;
    }
  }

  function _gridStartEditing(view, event) {
    var ed = view.getEditor();
    if (ed) {
      // Let the default key handler take take of it.
      return;
    }
    //var colId = view.config.columns[0];
    //console.log(colId);
    var rowId = view.getSelectedId();
    view.editCell(rowId, 0);
    // The space is eaten.
    return false;
  }

  function _gridAddRow(view, b) {
    var ed = view.getEditor();
    if (ed) {
      // Finish any editing.
      view.editStop();
    }

    //console.log(a.count());
    var max = 0;
    view.eachRow(function (id) {
      if (parseInt(id) > max) {
        max = parseInt(id);
      }
    });
    var id = (max + 1).toString();
    //console.log(id);
    view.add({
      id: id, title: "My Fair Lady " + id, year: 1964, votes: 533848, rating: 8.9, rank: 1, details: {name: 'ABC'}
    });
    view.select(id);
    view.showItem(id);
    _gridStartEditing(view);
    //webix.UIManager.setFocus(a);
    //console.log(b);
  }

  webix.UIManager.removeHotKey('tab', $$('theTable'));
  webix.UIManager.removeHotKey('shift-tab', $$('theTable'));
  webix.UIManager.addHotKey('tab', _gridForwardTabHandler, $$('theTable'));
  webix.UIManager.addHotKey('shift-tab', _gridBackwardTabHandler, $$('theTable'));
  webix.UIManager.addHotKey('space', _gridStartEditing, $$('theTable'));
  webix.UIManager.addHotKey('enter', _gridStartEditing, $$('theTable'));
  webix.UIManager.addHotKey('alt+a', _gridAddRow, $$('theTable'));

  $$("theTable").attachEvent("onFocus", function (view) {
    if (view.getSelectedItem() == undefined) {
      view.select('0');
    }
    console.log(view.getSelectedItem());
  });

  $$("theTable").attachEvent("onbeforeeditstop", function (value, editor) {
    console.log(value);
    console.log(editor);
    /*
        var suggest_id = editor.config.suggest;
        var list = $$(suggest_id).getBody(); //list object inside the popup
        webix.message(list.config.id)
    */
  });

  /*
    webix.editors.myeditor = webix.extend({
      render:function(){
        console.log(this);
        return webix.html.create("div", {
          "class":"webix_dt_editor"
        }, "<input type='email'>");
      }}, webix.editors.combo);
  */


</script>
<#include "../../includes/footer.ftl" />

