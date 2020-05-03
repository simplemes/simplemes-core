/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.preference.ColumnPreference
import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockDomainUtils
import org.simplemes.eframe.test.MockPreferenceHolder
import org.simplemes.eframe.test.UnitTestUtils
import sample.controller.SampleParentController
import sample.domain.SampleParent
import sample.pogo.FindWorkResponse

/**
 * Tests.
 */
class ListWidgetSpec extends BaseWidgetSpecification {

  def setup() {
    new MockPreferenceHolder(this, []).install()
  }

  def "verify that simple case produces the right script - defaults"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [id: 'dummyID'], controllerClass: SampleParentController)
    def page = new ListWidget(widgetContext).build().toString()
    //println "page = $page"

    then: 'the page is valid'
    checkJavascriptFragment(page)

    and: 'the table is created with a height'
    def tableText = JavascriptTestUtils.extractBlock(page, '{view: "datatable"')
    JavascriptTestUtils.extractProperty(tableText, 'height') == null
  }

  def "verify that simple case does not create HTML"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [id: 'dummyID'], controllerClass: SampleParentController)
    def page = new ListWidget(widgetContext).build().toString()
    def postscript = widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkJavascriptFragment(page)
    checkJavascript(postscript)

    and: 'the correct div and script tags are created'
    !page.contains('<div')

    and: 'no toolbar is created'
    !page.contains('{ view: "toolbar"')

    and: 'no call to the ui init method is in the page'
    !page.contains('webix.ui({')
  }

  def "verify that the column resize event handler is correct"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [id: 'dummyID'], controllerClass: SampleParentController)
    def page = new ListWidget(widgetContext).build().toString()
    def postscript = widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkJavascriptFragment(page)
    checkJavascript(postscript)

    and: 'the event handler is correct'
    def eventBlock = JavascriptTestUtils.extractBlock(postscript, '$$("dummyID").attachEvent("onColumnResize", function(id,newWidth,oldWidth,user_action) {')
    eventBlock.contains('tk._columnResized("dummyID",window.location.pathname,id,newWidth);')
  }

  def "verify that the sorting event handlers are correct"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [id: 'dummyID'], controllerClass: SampleParentController)
    def page = new ListWidget(widgetContext).build().toString()
    def postscript = widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkJavascriptFragment(page)
    checkJavascript(postscript)

    and: 'the onStoreLoad event handler is correct'
    def eventBlock = JavascriptTestUtils.extractBlock(postscript, '$$("dummyID").data.attachEvent("onStoreLoad", function (driver, data) {')
    eventBlock.contains('$$("dummyID").markSorting(data.sort, data.sortDir);')

    and: 'the onStoreLoad event handler is correct'
    def eventBlock2 = JavascriptTestUtils.extractBlock(postscript, '$$("dummyID").attachEvent("onBeforeSort", function(by, dir, as) {')
    eventBlock2.contains('tk._columnSorted("dummyID",window.location.pathname,by,dir,"name");')
    eventBlock2.contains('$$("dummyID").url="/sampleParent/list";')
  }

  def "verify that the height and padding option is supported"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [height: '237', paddingX: '137'], controllerClass: SampleParentController)
    def page = new ListWidget(widgetContext).build().toString()
    //println "page = $page"

    then: 'the page is valid'
    checkJavascriptFragment(page)

    and: 'the table is created with a height'
    def tableText = JavascriptTestUtils.extractBlock(page, '{view: "datatable"')
    JavascriptTestUtils.extractProperty(tableText, 'height') == "tk.ph('237em')"

    and: 'the spacer is added to the horizontal'
    page.contains("cols: [ {width: tk.pw('137em')}")
    page.contains(",{width: tk.pw('137em')}")
  }

  def "verify that the uri option is supported"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [uri: '/order/findWork'], controllerClass: SampleParentController)
    def page = new ListWidget(widgetContext).build().toString()

    then: 'the page is valid'
    checkJavascriptFragment(page)

    and: 'the table is created with the right settings'
    def tableText = JavascriptTestUtils.extractBlock(page, '{view: "datatable"')
    JavascriptTestUtils.extractProperty(tableText, 'url') == "/order/findWork"
  }

  def "verify that a non-domain model is supported - model option"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [columns: 'order,inWork', model: "$FindWorkResponse.name"], controllerClass: SampleParentController)
    def page = new ListWidget(widgetContext).build().toString()

    then: 'the page is valid'
    checkJavascriptFragment(page)

    and: 'the POGO fields are used to create the column definitions'
    def tableText = JavascriptTestUtils.extractBlock(page, '{view: "datatable"')
    def inWorkText = TextUtils.findLine(tableText, 'id: "inWork"')
    JavascriptTestUtils.extractProperty(inWorkText, 'template') == '{common.readOnlyCheckbox()}'
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
    def widgetContext = buildWidgetContext(parameters: [columns: 'title,name'], controllerClass: SampleParentController)
    def page = new ListWidget(widgetContext).build().toString()

    then: 'the page is valid'
    checkJavascriptFragment(page)
    checkJavascript(widgetContext.markerCoordinator.postscript)

    and: 'the title column is the correct width'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'columns: [')
    def titleColumn = TextUtils.findLine(columnsBlock, 'id: "title"')
    titleColumn.contains('width: tk.pw("23.73%")')
    def nameColumn = TextUtils.findLine(columnsBlock, 'id: "name"')
    nameColumn.contains('width: tk.pw("14.23%")')
  }

  def "verify that the widget supports data array in javascript"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [dataFunction: "ABC.buildData"], controllerClass: SampleParentController)
    def page = new ListWidget(widgetContext).build().toString()
    def postscript = widgetContext.markerCoordinator.postscript
    //println "page = $page"

    then: 'the page is valid'
    checkJavascriptFragment(page)
    checkJavascript(postscript)

    and: 'the datatable uses the dataFunction'
    def tableText = JavascriptTestUtils.extractBlock(page, '{view: "datatable"')
    JavascriptTestUtils.extractProperty(tableText, 'data') == "ABC.buildData()"

    and: 'the datatable contains no url'
    JavascriptTestUtils.extractProperty(tableText, 'url') == null
  }

  def "verify that the widget supports onSelection"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [onSelect: "ABC.selectedRow"], controllerClass: SampleParentController)
    def page = new ListWidget(widgetContext).build().toString()
    def postscript = widgetContext.markerCoordinator.postscript
    //println "page = $page"
    //println "postscript = $postscript"

    then: 'the page is valid'
    checkJavascriptFragment(page)
    checkJavascript(postscript)

    and: 'the event handler is correct'
    def eventBlock = JavascriptTestUtils.extractBlock(postscript, '$$("sampleParentList").attachEvent("onAfterSelect", function (selection) {')
    //def eventBlock = JavascriptTestUtils.extractBlock(postscript, '$$("dummyID").attachEvent("onAfterSelect"')
    eventBlock.contains('{ABC.selectedRow})(rowData,"sampleParentList")')
  }

  def "verify that the action buttons options are supported"() {
    when: 'the UI element is built'
    def params = [id: 'dummyID']
    params['remove@buttonLabel'] = 'remove.label'
    params['remove@buttonHandler'] = 'handleRemove(rowData,listID)'
    def widgetContext = buildWidgetContext(parameters: params, controllerClass: SampleParentController)
    def page = new ListWidget(widgetContext).build().toString()
    def globalPostscript = widgetContext.markerCoordinator.globalPostscript
    //println "page = $page $globalPostscript"

    then: 'the page is valid'
    checkJavascriptFragment(page)
    checkJavascript(globalPostscript)

    and: 'the table is created with the right column definition'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'columns: [')
    def actionsColumn = TextUtils.findLine(columnsBlock, 'id: "_actionButtons"')
    JavascriptTestUtils.extractProperty(actionsColumn, 'template') == "dummyIDActionsRenderer"
    JavascriptTestUtils.extractProperty(actionsColumn, 'header').contains('text: "Actions"')

    and: 'the renderer has the handler'
    def renderer = JavascriptTestUtils.extractBlock(globalPostscript, 'function dummyIDActionsRenderer(')
    //println "renderer = $renderer"
    renderer.contains('handleRemove(rowData,listID)')
    renderer.contains('tk._gridActionButtonHandler')

    and: 'the button label is correct'
    def label = lookup('remove.label')
    def tooltip = lookup('remove.tooltip')
    renderer.contains("<span>$label</span>")
    renderer.contains("title='$tooltip'")

    def buttonLine = TextUtils.findLine(renderer, '<button')
    buttonLine.contains("id='remove'")
  }

  def "verify that the action buttons options are supported - icon"() {
    when: 'the UI element is built'
    def params = [id: 'dummyID']
    params['remove@buttonIcon'] = 'fa-minus-oval'
    params['remove@buttonLabel'] = 'remove.label'
    params['remove@buttonHandler'] = 'handleRemove(rowData,listID)'
    def widgetContext = buildWidgetContext(parameters: params, controllerClass: SampleParentController)
    def page = new ListWidget(widgetContext).build().toString()
    def globalPostscript = widgetContext.markerCoordinator.globalPostscript
    //println "page = $page $globalPostscript"

    then: 'the page is valid'
    checkJavascriptFragment(page)
    checkJavascript(globalPostscript)

    and: 'the button icon is correct'
    def renderer = JavascriptTestUtils.extractBlock(globalPostscript, 'function dummyIDActionsRenderer(')
    //println "renderer = $renderer"
    renderer.contains("<span class='webix_icon_btn fas fa-minus-oval'")

    and: 'the tooltip is correct'
    def tooltip = lookup('remove.tooltip')
    renderer.contains("title='$tooltip'")
  }

  def "verify that the action buttons options are supported - enabled flag on two buttons"() {
    when: 'the UI element is built'
    def params = [id: 'dummyID']
    params['add@buttonEnableColumn'] = 'canBeAdded'
    params['add@buttonLabel'] = 'add.label'
    params['add@buttonHandler'] = 'handleAdd(rowData,listID)'
    params['remove@buttonEnableColumn'] = 'canBeRemoved'
    params['remove@buttonIcon'] = 'remove.label'
    params['remove@buttonHandler'] = 'handleRemove(rowData,listID)'
    def widgetContext = buildWidgetContext(parameters: params, controllerClass: SampleParentController)
    def page = new ListWidget(widgetContext).build().toString()
    def globalPostscript = widgetContext.markerCoordinator.globalPostscript
    //println "page = $page $globalPostscript"

    then: 'the page is valid'
    checkJavascriptFragment(page)
    checkJavascript(globalPostscript)

    and: 'the first if block is correct'
    def renderer = JavascriptTestUtils.extractBlock(globalPostscript, 'function dummyIDActionsRenderer(')
    //println "renderer = $renderer"
    def addIfBlock = JavascriptTestUtils.extractBlock(renderer, 'if (obj.canBeAdded) {')
    addIfBlock.contains('handleAdd(')

    and: 'the second if block is correct'
    def removeIfBlock = JavascriptTestUtils.extractBlock(renderer, 'if (obj.canBeRemoved) {')
    removeIfBlock.contains('handleRemove(')
  }

  def "verify that the action buttons options gracefully detects illegal characters in the script - quotes"() {
    when: 'the UI element is built'
    def params = [id: 'dummyID']
    params['add@buttonEnableColumn'] = 'canBeAdded'
    params['add@buttonLabel'] = 'add.label'
    params['add@buttonHandler'] = "xyz$badCharacter".toString()
    def widgetContext = buildWidgetContext(parameters: params, controllerClass: SampleParentController)
    new ListWidget(widgetContext).build().toString()

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['add@buttonHandler', 'quote', 'xyz'])

    where:
    badCharacter | _
    "'"          | _
    '"'          | _
  }

  // test GUI page


}
