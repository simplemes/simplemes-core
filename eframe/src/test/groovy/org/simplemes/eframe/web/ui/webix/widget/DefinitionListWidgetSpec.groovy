/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.preference.ColumnPreference
import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockControllerUtils
import org.simplemes.eframe.test.MockDomainUtils
import org.simplemes.eframe.test.MockPreferenceHolder
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.web.ui.UIDefaults
import sample.controller.AllFieldsDomainController
import sample.controller.RMAController
import sample.controller.SampleParentController
import sample.domain.AllFieldsDomain
import sample.domain.RMA
import sample.domain.SampleParent

/**
 * Tests.
 */
class DefinitionListWidgetSpec extends BaseWidgetSpecification {

  def setup() {
    new MockPreferenceHolder(this, []).install()
  }

  def "verify that basic HTML structure is correct for a simple grid"() {
    when: 'the UI element is built'
    def page = new DefinitionListWidget(new WidgetContext(parameters: [id: 'dummyID'], controllerClass: SampleParentController)).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the correct div and script tags are created'
    page.contains('<div id="dummyID"></div>')
    page.contains('<script>')
    page.contains('</script>')

    and: 'the URL is checked for messages'
    page.contains('efd._checkURLMessages();')


    and: 'the columns are sortable with server sorting'
    def name = TextUtils.findLine(page, 'id: "name"')
    JavascriptTestUtils.extractProperty(name, 'sort') == 'server'
  }

  def "verify that layout script is correct for a simple grid"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, SampleParent).install()

    when: 'the UI element is built'
    def page = new DefinitionListWidget(new WidgetContext(parameters: [id: 'dummyID'], controllerClass: SampleParentController)).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the right layout is started'
    def uiBlock = JavascriptTestUtils.extractBlock(page, 'webix.ui({')
    uiBlock.contains('container: "dummyID",')
    def typeLine = TextUtils.findLine(uiBlock, 'type')
    typeLine.contains('id: "dummyIDLayout"')
    typeLine.contains('rows: [')
  }

  def "verify that the table toolbar is correct for a simple grid"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, SampleParent).install()

    when: 'the UI element is built'
    def page = new DefinitionListWidget(new WidgetContext(parameters: [id: 'dummyID'], controllerClass: SampleParentController)).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the toolbar is correct'
    def viewBlock = JavascriptTestUtils.extractBlock(page, '{ view: "toolbar"')
    viewBlock.contains('id: "dummyIDToolbar",')

    and: 'it has the right elements'
    def elementsBlock = JavascriptTestUtils.extractBlock(page, 'elements: [')
    elementsBlock

    and: 'the toolbar has the right search field'
    def searchView = TextUtils.findLine(elementsBlock, 'view: "text"')
    searchView.contains('id: "dummyIDSearch"')
    searchView.contains('width: tk.pw(')
    searchView.contains("placeholder: \"${lookup('search.label')}\"")

    and: 'the toolbar has a spacer label'
    def labelView = TextUtils.findLine(elementsBlock, 'view: "label"')
    labelView.contains('<span>')
    labelView.contains('</span>')

    and: 'the toolbar has a create button'
    def buttonView = TextUtils.findLine(elementsBlock, 'view: "button"')
    JavascriptTestUtils.extractProperty(buttonView, 'id') == 'dummyIDCreate'
    JavascriptTestUtils.extractProperty(buttonView, 'autowidth') == 'true'
    JavascriptTestUtils.extractProperty(buttonView, 'type') == 'icon'
    JavascriptTestUtils.extractProperty(buttonView, 'icon') == 'fas fa-plus-square'
    JavascriptTestUtils.extractProperty(buttonView, 'label') == lookup('create.button.label', null, lookup('sampleParent.label'))
    JavascriptTestUtils.extractProperty(buttonView, 'tooltip') == lookup('create.button.tooltip', null, lookup('sampleParent.label'))
    JavascriptTestUtils.extractProperty(buttonView, 'click') == "window.location='/sampleParent/create'"
  }

  def "verify that the create action uses the right case for the URL"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, RMA).install()

    when: 'the UI element is built'
    def page = new DefinitionListWidget(new WidgetContext(parameters: [id: 'dummyID'], controllerClass: RMAController)).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the create button has the right URL'
    def elementsBlock = JavascriptTestUtils.extractBlock(page, 'elements: [')
    def buttonView = TextUtils.findLine(elementsBlock, 'view: "button"')
    JavascriptTestUtils.extractProperty(buttonView, 'click') == "window.location='/rma/create'"
  }

  def "verify that the table options are correct for a simple grid"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, SampleParent).install()

    when: 'the UI element is built'
    def page = new DefinitionListWidget(new WidgetContext(parameters: [id: 'dummyID'], controllerClass: SampleParentController)).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the grid view is created'
    def viewBlock = JavascriptTestUtils.extractBlock(page, '{view: "datatable"')
    viewBlock
    //println "viewBlock = $viewBlock"

    and: 'the height is specified'
    def heightLine = TextUtils.findLine(viewBlock, 'height:')
    heightLine =~ /tk\.ph\("[0-9][0-9]%"\)/   // tk.ph("70%")

    and: 'the id passed in is used'
    TextUtils.findLine(viewBlock, 'id:').contains('dummyID')

    and: 'the columns are set to allow resize'
    TextUtils.findLine(viewBlock, 'resizeColumn:').contains('size: 6')

    and: 'column dragging is allowed'
    TextUtils.findLine(viewBlock, 'dragColumn:').contains('true')

    and: 'single row selection is allowed'
    TextUtils.findLine(viewBlock, 'select:').contains('row')

    and: 'the page size is correct'
    TextUtils.findLine(viewBlock, 'datafetch:').contains(UIDefaults.PAGE_SIZE.toString())

    and: 'the read only checkbox display option is correct'
    TextUtils.findLine(viewBlock, 'type: {readOnlyCheckbox:').contains('tk._readOnlyCheckbox')

    and: 'the data url is correct'
    TextUtils.findLine(viewBlock, 'url:').contains('/sampleParent/list')

    and: 'the pager reference is correct'
    TextUtils.findLine(viewBlock, 'pager:').contains('pager: "dummyIDPager",')
  }

  def "verify that the column definitions are correct for a simple grid"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, SampleParent).install()

    when: 'the UI element is built'
    def page = new DefinitionListWidget(new WidgetContext(parameters: [id: 'dummyID'], controllerClass: SampleParentController)).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the correct fields are in the column list'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'columns: [')
    columnsBlock.contains('id: "name"')
    columnsBlock.contains('id: "title"')
    columnsBlock.contains('id: "notes"')
  }

  def "verify that the column list is honored"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, SampleParent).install()

    when: 'the UI element is built'
    def widgetContext = new WidgetContext(parameters: [id: 'dummyID', columns: 'title,name'], controllerClass: SampleParentController)
    def page = new DefinitionListWidget(widgetContext).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the correct fields are in the column list'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'columns: [')
    columnsBlock.contains('id: "name"')
    columnsBlock.contains('id: "title"')

    and: 'the columns are in the right order'
    columnsBlock.indexOf('id: "title"') < columnsBlock.indexOf('id: "name"')

    and: 'the missing field is not displayed'
    !columnsBlock.contains('id: "notes"')
  }

  def "verify that the pager view is correct"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, SampleParent).install()

    when: 'the UI element is built'
    def widgetContext = new WidgetContext(parameters: [id: 'dummyID', columns: 'title,name'], controllerClass: SampleParentController)
    def page = new DefinitionListWidget(widgetContext).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the correct fields are in the column list'
    def pagerBlock = JavascriptTestUtils.extractBlock(page, '{view: "pager"')
    pagerBlock.contains("size: ${UIDefaults.PAGE_SIZE}")
    pagerBlock.contains("group: 5")
  }

  def "verify that the sorting mark event handler is correct"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, SampleParent).install()

    when: 'the UI element is built'
    def widgetContext = new WidgetContext(parameters: [id: 'dummyID', columns: 'title,name'], controllerClass: SampleParentController)
    def page = new DefinitionListWidget(widgetContext).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the event handler is correct'
    def eventBlock = JavascriptTestUtils.extractBlock(page, '$$("dummyID").data.attachEvent("onStoreLoad", function (driver, data) {')
    eventBlock.contains('$$("dummyID").markSorting(data.sort, data.sortDir);')
  }

  def "verify that the field definition for choice list elements are correct"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the UI element is built'
    def widgetContext = new WidgetContext(parameters: [id: 'dummyID', columns: 'status,reportTimeInterval'],
                                          controllerClass: AllFieldsDomainController)
    def page = new DefinitionListWidget(widgetContext).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the status template is correct'
    def statusLine = TextUtils.findLine(page, 'id: "status"')
    statusLine.contains(""",template:function(obj){return ef._getMemberSafely(obj,"_statusDisplay_");}""")

    and: 'the enum handler is correct'
    def reportLine = TextUtils.findLine(page, 'id: "reportTimeInterval"')
    reportLine.contains(""",template:function(obj){return ef._getMemberSafely(obj,"_reportTimeIntervalDisplay_");}""")
  }

  def "verify that the search enter key handler is correct"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, SampleParent).install()

    when: 'the UI element is built'
    def widgetContext = new WidgetContext(parameters: [id: 'dummyID', columns: 'title,name'], controllerClass: SampleParentController)
    def page = new DefinitionListWidget(widgetContext).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the event handler is correct'
    def eventBlock = JavascriptTestUtils.extractBlock(page, '$$("dummyIDSearch").attachEvent("onEnter", function (id) {')
    eventBlock

    and: 'the default enter behavior is prevented'
    eventBlock.contains('return true;')
  }

  def "verify that the column resize event handler is correct"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, SampleParent).install()

    when: 'the UI element is built'
    def widgetContext = new WidgetContext(parameters: [id: 'dummyID', columns: 'title,name'], controllerClass: SampleParentController)
    def page = new DefinitionListWidget(widgetContext).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the event handler is correct'
    def eventBlock = JavascriptTestUtils.extractBlock(page, '$$("dummyID").attachEvent("onColumnResize", function(id,newWidth,oldWidth,user_action) {')
    eventBlock.contains('tk._columnResized("dummyID",window.location.pathname,id,newWidth);')
  }

  def "verify that the initial display uses the default column size from the user preferences"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, SampleParent).install()

    and: 'a mocked request'
    mockRequest([uri: '/user'])

    and: 'a preference with some column sizes'
    def preferences = [new ColumnPreference(column: 'title', width: 23.73), new ColumnPreference(column: 'name', width: 14.23)]
    new MockPreferenceHolder(this, preferences).install()

    when: 'the UI element is built'
    def widgetContext = new WidgetContext(parameters: [id: 'dummyID', columns: 'title,name'], controllerClass: SampleParentController)
    def page = new DefinitionListWidget(widgetContext).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the title column is the correct width'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'columns: [')
    def titleColumn = TextUtils.findLine(columnsBlock, 'id: "title"')
    titleColumn.contains('width: tk.pw("23.73%")')
    def nameColumn = TextUtils.findLine(columnsBlock, 'id: "name"')
    nameColumn.contains('width: tk.pw("14.23%")')
  }

  def "verify that the initial display uses the default column sorting from the user preferences"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, SampleParent).install()

    and: 'a mocked request'
    mockRequest([uri: '/user'])

    and: 'a preference with a new default sort order'
    new MockPreferenceHolder(this, [new ColumnPreference(column: 'title', sortLevel: 1, sortAscending: false)]).install()

    when: 'the UI element is built'
    def widgetContext = new WidgetContext(parameters: [id: 'dummyID', columns: 'title,name'], controllerClass: SampleParentController)
    def page = new DefinitionListWidget(widgetContext).build().toString()

    then: 'the page is valid'
    // Can't use since the page contains order=... checkPage(page)
    JavascriptTestUtils.checkScriptsOnPage(page)

    and: 'the url has the default sort order'
    def urlLine = TextUtils.findLine(page, 'url:')
    urlLine.contains('url: "/sampleParent/list?sort=title&order=desc"')
  }

  def "verify that data parsing scheme is used for dates"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the UI element is built'
    def widgetContext = new WidgetContext(controllerClass: AllFieldsDomainController)
    def page = new DefinitionListWidget(widgetContext).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the parsing logic is in the javascript'
    def scheme = JavascriptTestUtils.extractBlock(page, 'scheme:{')
    scheme.contains('obj.dueDate')
    scheme.contains('obj.dateTime')
  }


  def "verify that the widget will fail gracefully when no column list can be found - no fieldOrder or passed in"() {
    given: 'a mocked domain and controller'
    def src = """
    package sample
    
    class SampleDomain  {
    }
    """
    def domainClass = CompilerTestUtils.compileSource(src)
    def src2 = """
    package sample
    
    class SampleDomainController  {
    }
    """
    def controllerClass = CompilerTestUtils.compileSource(src2)
    new MockDomainUtils(this, domainClass).install()

    when: 'the UI element is built'
    def widgetContext = new WidgetContext(controllerClass: controllerClass)
    new DefinitionListWidget(widgetContext).build().toString()

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['columns', 'fieldOrder', 'SampleDomain'])
  }

  def "verify that controller name is used in the default ID"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, [SampleParent]).install()
    new MockControllerUtils(this, [SampleParentController]).install()

    when: 'the UI element is built'
    def widgetContext = new WidgetContext(parameters: [:], controllerClass: SampleParentController)
    def page = new DefinitionListWidget(widgetContext).build().toString()

    then: 'the page is valid'
    checkPage(page)

    and: 'the correct controller is used'
    page.contains('<div id="sampleParentDefinitionList"></div>')
    def viewBlock = JavascriptTestUtils.extractBlock(page, '{view: "datatable"')
    TextUtils.findLine(viewBlock, 'url:').contains('/sampleParent/list')
  }
}
