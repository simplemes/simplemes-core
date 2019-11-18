package org.simplemes.eframe.test
/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class JavascriptTestUtilsSpec extends BaseSpecification {

  def "verify that checkScriptsOnPage works on valid scripts"() {
    expect: ''
    JavascriptTestUtils.checkScriptsOnPage(script)

    where:
    script                                                                      | _
    "<script>alert('<script>');</script>"                                       | _
    "<script>a({enabled: true});</script>"                                      | _
    '<script>$("#id").toolkitGrid({columns:[]});</script>'                      | _
    '<script>$("#id").toolkitCombobox({columns:[{enabled: "true"}]});</script>' | _
    '<script src="xyz">a({enabled: true});</script>'                            | _
    "<script>a('0');</script><script>a('1');</script><script>a('2');</script>"  | _
  }

  def "verify that checkScriptsOnPage fails on invalid scripts"() {
    when: 'the bad script is checked'
    JavascriptTestUtils.checkScriptsOnPage(script)

    then: 'the right exception is returned'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, [])

    where:
    script                                                                 | _
    "<script>'gibberish</script>"                                          | _
    '<script>$("#id").toolkitGrid({columns:[enabled:]});</script>'         | _
    '<script>$("#id").toolkitGrid({columns:[enabled: "true",]});</script>' | _
    "<script><script>'gibberish</script></script>"                         | _
    "<script>'gibberish</script>"                                          | _
  }

  def "verify that the error line is printed correctly"() {
    given: 'some javascript with an error'
    def page = '''
         <script>  // Ok Script
           $("#id").toolkitCombobox({columns:[{enabled: "true"}]});
         </script>
         <script>
           // Comments <script>
           $("#id").toolkitGrid({columns:[enabled: "true",]});
         </script>
    '''

    when: 'the script is checked'
    JavascriptTestUtils.checkScriptsOnPage(page)

    then: 'the right exception is returned'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['toolkitGrid'])
  }

  def "verify that extractScriptsFromPage fails without matching script tags"() {
    when: 'the script is checked'
    JavascriptTestUtils.extractScriptsFromPage("<script>")

    then: 'the right exception is returned'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['</script> tag'])
  }

  def "verify that extractBlock detects a quote in a comment"() {
    given: 'a script'
    def js = """
        function abc() {
          // Don't try this
          someMethod();
        }
      """

    when: 'the script is checked'
    JavascriptTestUtils.checkScriptsOnPage("<script>${js}</script>")
    JavascriptTestUtils.extractBlock(js, 'function abc(')

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['Mismatched', 'quote'])
  }

  def "verify that extractBlock returns nothing when function not found"() {
    given: 'a script'
    def js = """
      function abc() {
        someMethod();
      }
    """
    expect: 'the missing method is not found'
    !JavascriptTestUtils.extractBlock(js, 'function xyz(')
  }

  def "verify that extractBlock can use square brackets too"() {
    given: 'a script'
    def js = """
      function abc() {
        someMethod();
        var outside1;
        var array1 = [
          {abc: 123},
          {xyz: 456}
        ];
        var outside2;
      }
    """

    expect: 'the array is found'
    JavascriptTestUtils.checkScriptsOnPage("<script>${js}</script>")
    def s = JavascriptTestUtils.extractBlock(js, 'var array1 = [')
    s.contains('var array1')
    s.count('[') == 1
    s.count(']') == 1
    s.count('{') == 2
    s.count('}') == 2
    !s.contains('outside1')
    !s.contains('someMethod')
    !s.contains('outside2')
  }

  def "verify that extractScriptsFromPage handles null script"() {
    when: 'the script is checked'
    def res = JavascriptTestUtils.extractBlock(null, 'dummy')

    then: 'no block found'
    !res
  }

  def "verify that extractBlock handles the simplest script"() {
    given: 'a script'
    def js = """
      var outside1;
      function abc() {
        someMethod();
      }
      var outside2;
    """

    expect: 'the function is found'
    JavascriptTestUtils.checkScriptsOnPage("<script>${js}</script>")
    def s = JavascriptTestUtils.extractBlock(js, 'function abc(')
    s.contains('someMethod()')
    s.count('{') == 1
    s.count('}') == 1
    s.count('()') == 2
    !s.contains('outside1')
    !s.contains('outside2')
  }

  def "verify that extractBlock handles a medium complex script with quoted strings"() {
    given: 'a script'
    def js = """
      var outside1;
      function abc() {
        var unbalanced1='}'+"}"+"'}"+'\\}';
        someMethod();
      }
      var outside2;
    """

    expect: 'the method is found'
    JavascriptTestUtils.checkScriptsOnPage("<script>${js}</script>")
    def s = JavascriptTestUtils.extractBlock(js, 'function abc(')
    s.contains('someMethod()')
    s.contains('unbalanced1')
    s.count('{') == 1
    s.count('()') == 2
    !s.contains('outside1')
    !s.contains('outside2')
  }

  def "verify that extractBlock handles a complex script"() {
    given: 'a script'
    def js = """
    function dateLoadedEditor(container, options) {
      \$('<input name="'+options.field +'"/>')
          .appendTo(container).toolkitDatePicker({format: "M/d/yy" });
    }
    childDateFormats['dateLoaded']='M/d/yy';
    function flexTypeEditor(container, options) {
      var unbalanced1='}';
      var unbalanced2="}";
      var unbalanced3="'}";
      var unbalanced4='\\}';
            \$('<input required data-text-field="flexTypeText" data-value-field="id" data-bind="value:' + options.field + '"/>')
                .appendTo(container)
    .toolkitComboBox({
      filter: "contains",
      template: '#if (data.link) {# <a href="\${data.link}" target="blank" id="flexTypeAddLink">\${ data.flexTypeText }</a> #} else {#\${data.flexTypeText}#}#',
      dataTextField: "flexTypeText",
      dataValueField: "id",
      suggest: true,
      select: function(e) {
        var item = e.item;
        if (item.html().indexOf('<a')>=0) {
          e.preventDefault();  // Revert selection
        }
      },
      dataSource: {
        data: [
            {id: "null", flexTypeText: ""},
              {id: "1000000000000602", flexTypeText: "LOT (<script>)"},
              {id: "1000000000000603", flexTypeText: "SERIAL (Serial Number)"},
              {id: "1000000000000600", flexTypeText: "TYPE1 (Type1\\")"},
              {id: "1000000000000601", flexTypeText: "VENDOR (Vendor)"},
              { flexTypeText: "Add a new Flex Type", value: "link", link: "/eframe/flexType/create"  }]}
      });
      }
      function dateOrderedEditor(container, options) {
        \$('<input name="'+options.field +'"/>')
            .appendTo(container).toolkitDateTimePicker({format: "M/d/yy h:mm:ss tt" });
      }
      childDateFormats['dateOrdered']='M/d/yy h:mm:ss tt';
"""

    when: 'the block is extracted'
    JavascriptTestUtils.checkScriptsOnPage("<script>${js}</script>")
    def s = JavascriptTestUtils.extractBlock(js, 'function flexTypeEditor(')

    then: 'the method is found along with its text'
    s.contains('dataSource')
    s.contains('toolkitComboBox(')
    s.contains('});')
    !s.contains('dateLoadedEditor(')
    !s.contains('dateOrderedEditor(')

    when: 'a sub-block is extracted'
    def s1 = JavascriptTestUtils.extractBlock(js, '.toolkitComboBox({')

    then: 'right block is found'
    s1.contains('dataSource:')


    when: 'a sub-sub-block is extracted'
    def s2 = JavascriptTestUtils.extractBlock(js, 'dataSource:')

    then: 'right block is found'
    s2.contains('data:')
  }

  def "verify that extractBlock handles a super complex script"() {
    given: 'a script'
    def js = """
<div id="ChildList"></div><script>
  \$(document).ready(function () {var firstChildList=true;

        \$("#ChildList").toolkitGrid({
          sortable: { mode: "multiple" },
          navigatable: true,
          reorderable: true,
          resizable: true,
          editable: {mode: "incell", confirmation: false},
          toolbar: [{
  template:'<span style="float: right;"><button type="button" class="button button-icon" title="Add a new row" onclick="ChildListAdd();"><span class="sprite icon add"></span></button><button type="button" class="button button-icon" title="Remove selected row" onclick="ChildListRemove();"><span class="sprite icon delete"></span></button></span>'
}],
          columnResize: toolkitWebNotifyColumnResized,


          dataBound: function(e) {
            // Do not call on the first dataBound event since,
            if (!firstChildList) {
              toolkitWebNotifyListSorted(e);
            }
            firstChildList=false;
          },


            edit: function(e) {
              var input = \$(e.container).find("input[type=text]");
              input.focus();
              input.select();
            },

          columns: [
            { field: "sequence", title: "Sequence"  , attributes: { style: "text-align: center" }, headerAttributes: { style: "text-align: center" }  },
{ field: "description", title: "Description"    },
{ field: "enabled", title: "Enabled"    },
{ field: "flexType", title: "Flex Type"   , template: '#: data.flexType ? data.flexType.flexTypeText : ""#',editor: flexTypeEditor },
{ field: "dateLoaded", title: "Date Loaded"   , template: '#= toolkitWebPluginConvertISODateOnly((data.dateLoaded) ? dateLoaded : "", "M/d/yy") #',editor: dateLoadedEditor },
{ field: "dateOrdered", title: "Ordered"   , template: '#= toolkitWebPluginConvertISODate((data.dateOrdered) ? dateOrdered : "", "M/d/yy h:mm:ss tt") #',editor: dateOrderedEditor }
          ],
          dataSource: {
  batch: true,
  sort: [{ field: "sequence", dir: "asc"}],
  data: [
{ sequence: 10,description: "A Description &quot;",enabled: true,flexType: "",dateLoaded: "2014-03-01",dateOrdered: "2014-03-01T16:28:21.961Z"},
{ sequence: 20,description: "Another Description &quot;",enabled: true,flexType: "",dateLoaded: "2014-03-01",dateOrdered: "2014-03-01T16:28:21.962Z"} ]
}

        });

      \$("form").submit(function (event) {
        //event.preventDefault();
        buildChildRecordInputElements(event.target.id,"ChildList","null");
      });

 });

        function ChildListRemove() {
          var grid=\$("#ChildList").data("toolkitGrid");
          if (grid.current()!=undefined) {
            grid.removeRow(grid.current().closest('tr'));
          } else {
            // No current selection, so fallback and try to delete the current edit row (if any).
            if (grid.tbody.find(".grid-edit-row").length) {
              grid.removeRow(grid.tbody.find(".grid-edit-row"));
            }
          }
        }
        var seq=10;
        function ChildListAdd() {
          var grid=\$("#ChildList").data("toolkitGrid");
          var model = grid.dataSource.insert(0, { sequence: seq, enabled: true, dateLoaded: "2014-03-01", dateOrdered: "2014-03-01T16:28:21.970Z"});
          seq+=10;
          var id = model.uid;
          var row = grid.table.find("tr[" + toolkit.attr("uid") + "=" + id + "]");
          var cell = row.children("td:not(.group-cell,.hierarchy-cell)").eq(grid._firstEditableColumnIndex(row));
          grid.editCell(cell);
        }

          function flexTypeEditor(container, options) {
            \$('<input required data-text-field="flexTypeText" data-value-field="id" data-bind="value:' + options.field + '"/>')
                .appendTo(container)
    .toolkitComboBox({
      filter: "contains",
      template: '#if (data.link) {# <a href="\${data.link}"   id="flexTypeAddLink">\${data.flexTypeText}</a> #} else {#\${data.flexTypeText }#}#',
      dataTextField: "flexTypeText",
      dataValueField: "id",
      suggest: true,
      select: comboBoxHyperlinkSelected,
      dataSource: {
        data: [
    {id: "null", flexTypeText: ""},
{ flexTypeText: "add.new.label[Flex Type]", value: "link", link: "/app/flexType/create"  }]}
    });
    }
            function dateLoadedEditor(container, options) {
              \$('<input name="'+options.field +'"/>')
                  .appendTo(container).toolkitDatePicker({format: "M/d/yy" });
            }
            childDateFormats['dateLoaded']='M/d/yy';

            function dateOrderedEditor(container, options) {
              \$('<input name="'+options.field +'"/>')
                  .appendTo(container).toolkitDateTimePicker({format: "M/d/yy h:mm:ss tt" });
            }
            childDateFormats['dateOrdered']='M/d/yy h:mm:ss tt';
      </script> """
    when: 'the block is extracted'
    JavascriptTestUtils.checkScriptsOnPage(js)
    def s = JavascriptTestUtils.extractBlock(js, '$("#ChildList").toolkitGrid({')

    then: 'the block is found'
    s.contains('navigatable: true')
  }

  def "verify that extractProperty works on valid scripts"() {
    expect: ''
    //println "/$script/, /$result/, /${JavascriptTestUtils.extractProperty(script, name)}/"
    JavascriptTestUtils.extractProperty(script, name) == result

    where:
    script                            | name     | result
    null                              | 'view'   | null
    ''                                | 'view'   | null
    '{view: "ABC"}'                   | 'view'   | 'ABC'
    '{view: ""}'                      | 'view'   | ''
    '{name: "XYZ", view: "ABC"}'      | 'view'   | 'ABC'
    '{name: "XYZ", view: "ABC"}'      | 'name'   | 'XYZ'
    '{id: "name", name: "ABC"}'       | 'name'   | 'ABC'
    ' view: "ABC"'                    | 'view'   | 'ABC'
    " view: 'ABC'"                    | 'view'   | "ABC"
    '{view: "ABC"}'                   | 'name'   | null
    '{margin: 6}'                     | 'margin' | '6'
    '{ok: true}'                      | 'ok'     | 'true'
    '{ok: '                           | 'ok'     | null
    '{width: tk.pw("10%")}'           | 'width'  | 'tk.pw("10%")'
    '{width: tk.pw("10%"), ok: true}' | 'width'  | 'tk.pw("10%")'
  }

}
