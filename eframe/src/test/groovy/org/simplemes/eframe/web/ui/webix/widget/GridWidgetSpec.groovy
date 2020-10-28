/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget


import org.simplemes.eframe.data.format.ChildListFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.ISODate
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.preference.ColumnPreference
import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockPreferenceHolder
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import org.simplemes.eframe.web.ui.webix.DomainToolkitUtils
import sample.domain.SampleChild

/**
 * Tests.
 */
class GridWidgetSpec extends BaseWidgetSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "verify that widget builds basic structure correctly - table definition"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: [], format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the first column is defined'
    def keyText = TextUtils.findLine(page, 'id: "key"')
    JavascriptTestUtils.extractProperty(keyText, 'editor') == 'text'
    JavascriptTestUtils.extractProperty(keyText, 'sort') == 'text'
    JavascriptTestUtils.extractProperty(keyText, 'format') == DomainToolkitUtils.FORMAT_ESCAPE_HTML_WITH_SAFE_TAGS
    JavascriptTestUtils.extractProperty(keyText, 'width') =~ /tk\.pw\("[\d\.]*%"\)/  // example: tk.pw("30.0%")

    and: 'the other column is defined'
    TextUtils.findLine(page, 'id: "title"') != null

    and: 'the basic table definition values are correct'
    def tableText = JavascriptTestUtils.extractBlock(page, '{ view: "datatable"')
    //println "tableText = $tableText"
    JavascriptTestUtils.extractProperty(tableText, 'id') == 'aField'
    JavascriptTestUtils.extractProperty(tableText, 'height') == 'tk.ph("30%")'
    JavascriptTestUtils.extractProperty(tableText, 'editable') == 'true'
    JavascriptTestUtils.extractProperty(tableText, 'select') == 'row'

    and: 'the inline grid is registered for form submission'
    page.contains('efd._registerInlineGridName("aField");')
  }

  def "verify that widget builds basic structure correctly - data rows"() {
    given: 'a value'
    def list = []
    def dueDate = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def dateTime = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
    list << new SampleChild(key: 'k1-abc', title: 'abc', enabled: false,
                            dueDate: dueDate, dateTime: dateTime, uuid: UUID.randomUUID())
    list << new SampleChild(key: 'k2-xyz', title: 'xyz', enabled: true, uuid: UUID.randomUUID())

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [id: 'TheTable'], value: list,
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the data rows contain the passed in data'
    def dataText = JavascriptTestUtils.extractBlock(page, 'data: [')

    def rowText1 = TextUtils.findLine(dataText, "id: \"${list[0].uuid}\"")
    JavascriptTestUtils.extractProperty(rowText1, 'key') == 'k1-abc'
    JavascriptTestUtils.extractProperty(rowText1, 'title') == 'abc'
    JavascriptTestUtils.extractProperty(rowText1, '_dbId') == list[0].uuid.toString()
    JavascriptTestUtils.extractProperty(rowText1, 'enabled') == 'false'

    def rowText2 = TextUtils.findLine(dataText, "id: \"${list[1].uuid}\"")
    JavascriptTestUtils.extractProperty(rowText2, 'key') == 'k2-xyz'
    JavascriptTestUtils.extractProperty(rowText2, 'title') == 'xyz'
    JavascriptTestUtils.extractProperty(rowText2, '_dbId') == list[1].uuid.toString()
    JavascriptTestUtils.extractProperty(rowText2, 'enabled') == 'true'

    and: 'the boolean values are enclosed in quotes'
    rowText1.contains('enabled: false')
    rowText2.contains('enabled: true')

    and: 'the date types are correct'
    JavascriptTestUtils.extractProperty(rowText1, 'dueDate') == ISODate.format(dueDate)
    JavascriptTestUtils.extractProperty(rowText1, 'dateTime') == ISODate.format(dateTime)

    and: 'the first row is auto-selected'
    def start = page.indexOf('var tableTheTable')
    def handlerText = page[start..-1]
    handlerText.contains("""tableTheTable.select("${list[0].uuid}")""")
  }

  def "verify that widget builds basic structure correctly - buttons"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: [], format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the add row button is correct'
    def addText = JavascriptTestUtils.extractBlock(page, '{ id: "aFieldAdd"')
    JavascriptTestUtils.extractProperty(addText, 'view') == 'button'
    JavascriptTestUtils.extractProperty(addText, 'type') == 'icon'
    JavascriptTestUtils.extractProperty(addText, 'icon') == 'fas fa-plus-square'
    JavascriptTestUtils.extractProperty(addText, 'click') == 'aFieldAddRow()'
    JavascriptTestUtils.extractProperty(addText, 'tooltip') == GlobalUtils.lookup('addRow.tooltip')

    and: 'the delete row button is correct'
    def removeText = JavascriptTestUtils.extractBlock(page, '{ id: "aFieldRemove"')
    JavascriptTestUtils.extractProperty(removeText, 'view') == 'button'
    JavascriptTestUtils.extractProperty(removeText, 'type') == 'icon'
    JavascriptTestUtils.extractProperty(removeText, 'icon') == 'fas fa-minus-square'
    JavascriptTestUtils.extractProperty(removeText, 'click') == 'tk._gridRemoveRow($$("aField"))'
    JavascriptTestUtils.extractProperty(removeText, 'tooltip') == GlobalUtils.lookup('deleteRow.tooltip')
  }

  def "verify that widget builds basic structure correctly - handlers"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [id: 'TheTable'], value: [],
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the local variable is correct for the table element'
    def start = page.indexOf('var tableTheTable')
    def handlerText = page[start..-1]
    //println "handlerText = $handlerText"
    handlerText.contains('var tableTheTable = $$("TheTable");')

    and: 'the hotKey changes are in the script'
    handlerText.contains("webix.UIManager.removeHotKey('tab', tableTheTable);")
    handlerText.contains("webix.UIManager.removeHotKey('shift-tab', tableTheTable);")
    handlerText.contains("webix.UIManager.addHotKey('tab', tk._gridForwardTabHandler, tableTheTable);")
    handlerText.contains("webix.UIManager.addHotKey('shift-tab', tk._gridBackwardTabHandler, tableTheTable);")
    handlerText.contains("webix.UIManager.addHotKey('space', tk._gridStartEditing, tableTheTable);")
    handlerText.contains("webix.UIManager.addHotKey('enter', tk._gridStartEditing, tableTheTable);")
    handlerText.contains("webix.UIManager.addHotKey('alt+a', TheTableAddRow, tableTheTable);")

    and: 'there is no focus handler'
    !handlerText.contains('tableTheTable.attachEvent("onFocus"')

    and: 'the resize handler is correct'
    def resizeText = JavascriptTestUtils.extractBlock(handlerText, 'tableTheTable.attachEvent("onColumnResize"')
    resizeText.contains('tk._columnResized("TheTable"')

    and: 'the sort handler is correct'
    def sortText = JavascriptTestUtils.extractBlock(handlerText, 'tableTheTable.attachEvent("onBeforeSort"')
    sortText.contains('tk._columnSorted("TheTable"')
  }

  def "verify that widget builds basic structure correctly - readOnly case"() {
    given: 'a value'
    def list = []
    list << new SampleChild(key: 'k1-abc', title: 'abc', uuid: UUID.randomUUID())
    list << new SampleChild(key: 'k2-xyz', title: 'xyz', uuid: UUID.randomUUID())

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(readOnly: true, value: list, parameters: [id: 'TheTable'],
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the table itself is flagged as not editable'
    def tableText = JavascriptTestUtils.extractBlock(page, '{ view: "datatable"')
    JavascriptTestUtils.extractProperty(tableText, 'editable') == 'false'

    and: 'the first column has no editor'
    def keyText = TextUtils.findLine(page, 'id: "key"')
    !JavascriptTestUtils.extractProperty(keyText, 'editor')

    and: 'there are no add/remove buttons'
    !page.contains('{ id: "TheTableAdd"')
    !page.contains('{ id: "TheTableRemove"')

    and: 'there is no initial select logic'
    !page.contains("tableTheTable.select(")
  }

  def "verify that widget supports a readOnly parameter"() {
    given: 'a value'
    def list = []
    list << new SampleChild(key: 'k1-abc', title: 'abc', uuid: UUID.randomUUID())
    list << new SampleChild(key: 'k2-xyz', title: 'xyz', uuid: UUID.randomUUID())

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: list, parameters: [id: 'TheTable', readOnly: 'true'],
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the table itself is flagged as not editable'
    def tableText = JavascriptTestUtils.extractBlock(page, '{ view: "datatable"')
    JavascriptTestUtils.extractProperty(tableText, 'editable') == 'false'

    and: 'the inline grid is not registered for form submission'
    !page.contains('efd._registerInlineGridName("TheTable");')
  }

  def "verify that widget builds basic structure correctly - data is escaped correctly"() {
    given: 'a value'
    def list = []
    list << new SampleChild(key: 'k1-abc', title: '<script>"</script>', uuid: UUID.randomUUID())
    list << new SampleChild(key: 'k2-xyz', title: "<script>'</script>", uuid: UUID.randomUUID())

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [id: 'TheTable'], value: list,
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the escaped value is in the data element'
    def dataText = JavascriptTestUtils.extractBlock(page, 'data: [')
    def rowText1 = TextUtils.findLine(dataText, """id: "${list[0].uuid}""")
    rowText1.contains('"<script>\\"<\\/script>"')

    def rowText2 = TextUtils.findLine(dataText, """id: "${list[1].uuid}""")
    rowText2.contains('"<script>\'<\\/script>"')

    and: 'the values are HTML escaped correctly on the client'
    def keyText = TextUtils.findLine(page, 'id: "key"')
    JavascriptTestUtils.extractProperty(keyText, 'format') == DomainToolkitUtils.FORMAT_ESCAPE_HTML_WITH_SAFE_TAGS
  }

  def "verify that widget builds basic structure correctly - client sort is based on the field type"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [id: 'TheTable'], value: [],
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the client sort type is correct'
    def keyText = TextUtils.findLine(page, 'id: "key"')
    JavascriptTestUtils.extractProperty(keyText, 'sort') == 'text'
    def seqText = TextUtils.findLine(page, 'id: "sequence"')
    JavascriptTestUtils.extractProperty(seqText, 'sort') == 'int'
  }

  def "verify that widget uses the sorting preferences"() {
    given: 'a preference with a new default sort order'
    new MockPreferenceHolder(this, [new ColumnPreference(column: 'sequence', sortLevel: 1, sortAscending: false)]).install()

    and: 'a value'
    def list = []
    list << new SampleChild(key: 'k1-abc', title: 'abc', sequence: 10, uuid: UUID.randomUUID())
    list << new SampleChild(key: 'k2-xyz', title: "xyz", sequence: 1, uuid: UUID.randomUUID())
    list << new SampleChild(key: 'k3-pdq', title: "pdq", sequence: 13, uuid: UUID.randomUUID())

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [id: 'TheTable'], value: list,
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the sorting is marked in the post script text'
    def markLine = TextUtils.findLine(page, 'tableTheTable.markSorting')
    markLine.contains('markSorting("sequence","desc");')

    and: 'the data list is in the correct order - integer descending sort on sequence'
    page.indexOf(list[2].uuid.toString()) < page.indexOf(list[0].uuid.toString())   // Sequence 13 is before sequence 10
    page.indexOf(list[0].uuid.toString()) < page.indexOf(list[1].uuid.toString())   // Sequence 10 is before sequence 1
  }

  def "verify that widget uses the sorting preferences - readOnly case"() {
    given: 'a preference with a new default sort order'
    new MockPreferenceHolder(this, [new ColumnPreference(column: 'title', sortLevel: 1, sortAscending: true)]).install()

    and: 'a value'
    def list = []
    list << new SampleChild(key: 'k1-abc', title: 'abc', sequence: 10,
                            reportTimeInterval: ReportTimeIntervalEnum.LAST_6_MONTHS, uuid: UUID.randomUUID())
    list << new SampleChild(key: 'k2-xyz', title: "xyz", sequence: 1, uuid: UUID.randomUUID())
    list << new SampleChild(key: 'k3-pdq', title: "pdq", sequence: 13, uuid: UUID.randomUUID())

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(readOnly: true, parameters: [id: 'TheTable'], value: list,
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the sorting is marked in the post script text'
    def markLine = TextUtils.findLine(page, 'tableTheTable.markSorting')
    markLine.contains('markSorting("title","asc");')

    and: 'the data list is in the correct order - integer descending sort on sequence'
    page.indexOf(list[0].uuid.toString()) < page.indexOf(list[2].uuid.toString())   // title 'abc' is before 'pdq'
    page.indexOf(list[2].uuid.toString()) < page.indexOf(list[1].uuid.toString())   // title 'pdq' is before 'xyz'

    and: 'the value is the display value, not the encoded value'
    page.contains("""reportTimeInterval: "${ReportTimeIntervalEnum.LAST_6_MONTHS.toStringLocalized()}" """)
  }

  def "verify that widget uses the column width preferences"() {
    given: 'a preference with a new default sort order'
    def preferences = [new ColumnPreference(column: 'key', width: 23.73), new ColumnPreference(column: 'title', width: 14.23)]
    new MockPreferenceHolder(this, preferences).install()

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [id: 'TheTable'], value: [],
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the title column is the correct width'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'columns: [')
    def titleColumn = TextUtils.findLine(columnsBlock, 'id: "key"')
    titleColumn.contains('width: tk.pw("23.73%")')
    def nameColumn = TextUtils.findLine(columnsBlock, 'id: "title"')
    nameColumn.contains('width: tk.pw("14.23%")')
  }

  def "verify that widget supports the height option"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: [], parameters: [height: '23.2%'],
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the basic table definition values are correct'
    def tableText = JavascriptTestUtils.extractBlock(page, '{ view: "datatable"')
    //println "tableText = $tableText"
    JavascriptTestUtils.extractProperty(tableText, 'height') == 'tk.ph("23.2%")'
  }

  def "verify that widget supports the no label option"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: [], parameters: [label: ''],
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)


    and: 'the label is not displayed'
    !page.contains('id: "aFieldLabel"')

    and: 'the column widths the the label width space'
    // This is based on the use of (JSPageOptions.LABEL_WIDTH_DEFAULT + DEFAULT_TOTAL_WIDTH)/10 (80/10=10.0)
    def keyText = TextUtils.findLine(page, 'id: "key"')
    JavascriptTestUtils.extractProperty(keyText, 'width') =~ /tk\.pw\("8.0[\d]*%"\)/  // example: tk.pw("10.0%")
    def sequenceText = TextUtils.findLine(page, 'id: "sequence"')
    JavascriptTestUtils.extractProperty(sequenceText, 'width') =~ /tk\.pw\("8.0[\d]*%"\)/  // example: tk.pw("10.0%")
  }

  def "verify that widget has correct addRow logic using the default values from the domain class"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: [], format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the generated addRow method is correct'
    def addRowText = JavascriptTestUtils.extractBlock(page, 'function aFieldAddRow(')
    addRowText.contains('tk._gridAddRow($$("aField"),rowData)')

    and: 'has the default value'
    addRowText.contains('"sequence":10')
  }

  def "verify that widget has custom addRow logic "() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: [], parameters: ['sequence@default': 'customMethod()'],
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the generated addRow method is correct'
    def addRowText = JavascriptTestUtils.extractBlock(page, 'function aFieldAddRow(')
    addRowText.contains('rowData.sequence=sequenceDefault();')
    addRowText.contains('var gridName = "aField";')

    def addRowText2 = JavascriptTestUtils.extractBlock(addRowText, 'function sequenceDefault(')
    addRowText2.contains('return customMethod();')

    and: 'the default value is used in the new row data'
    addRowText.contains('rowData.sequence=sequenceDefault();')
  }

  def "verify that widget has custom addRow logic with return statement"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: [], parameters: ['sequence@default': 'return customizedMethod()'],
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the generated addRow method is correct'
    def addRowText = JavascriptTestUtils.extractBlock(page, 'function aFieldAddRow(')
    def addRowText2 = JavascriptTestUtils.extractBlock(addRowText, 'function sequenceDefault(')
    addRowText2.contains('{return customizedMethod()')
  }

  def "verify that widget has prefix for custom addRow prefix"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: [], parameters: ['addRowPrefix': '_dialogContentY.'],
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)

    and: 'the hotkey reference is correct'
    def hotKeyText = TextUtils.findLine(page, ".addHotKey('alt+a'")
    hotKeyText.contains('_dialogContentY.aFieldAddRow')

    and: 'the add row button click reference is correct'
    def addText = JavascriptTestUtils.extractBlock(page, '{ id: "aFieldAdd"')
    JavascriptTestUtils.extractProperty(addText, 'click') == '_dialogContentY.aFieldAddRow()'

    and: 'the add row function definition is correct'
    page.contains('_dialogContentY.aFieldAddRow = function () {')
  }

  def "verify that widget fails when the column list is not specified and no fieldOrder on domain"() {
    given: 'a domain with its no fieldOrder'
    def src = """
    package sample
    
    class SampleClass {
      String field1
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [id: 'TheTable'], value: [],
                                           format: ChildListFieldFormat.instance, referenceType: clazz)
    new GridWidget(widgetContext).build().toString()

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['columns', 'fieldOrder'])
  }

  def "verify that widget handles null list gracefully"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [id: 'TheTable'], value: null,
                                           format: ChildListFieldFormat.instance, referenceType: SampleChild)
    def page = new GridWidget(widgetContext).build().toString() + widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkPage(page)
  }


}
