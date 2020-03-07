/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import sample.controller.SampleParentController

/**
 * Tests.
 */
class DefinitionListMarkerSpec extends BaseMarkerSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "verify that this marker uses the columns option correctly"() {
    when: 'the marker is built'
    def page = execute(source: '<@efDefinitionList id="dummyID" columns="name,title"/>', controllerClass: SampleParentController)

    then: 'the page is valid'
    checkPage(page)

    then: 'the correct fields are in the column list'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'columns: [')
    columnsBlock.contains('id: "name"')
    columnsBlock.contains('id: "title"')

    and: 'the correct div and script tags are created'
    page.contains('<div id="dummyID"></div>')
    page.contains('<script>')
    page.contains('</script>')

    and: 'the right layout is started'
    def uiBlock = JavascriptTestUtils.extractBlock(page, 'webix.ui({')
    uiBlock.contains('container: "dummyID",')
    def typeLine = TextUtils.findLine(uiBlock, 'type')
    typeLine.contains('id: "dummyIDLayout"')
    typeLine.contains('rows: [')

    and: 'the event handlers are in the page'
    page.contains('onStoreLoad')
    page.contains('onEnter')
    page.contains('onColumnResize')
    def eventBlock = JavascriptTestUtils.extractBlock(page, '$$("dummyID").data.attachEvent("onStoreLoad", function (driver, data) {')
    eventBlock.contains('$$("dummyID").markSorting(data.sort, data.sortDir);')

  }

  def "verify that controller name is used in the default ID"() {
    when: 'the UI element is built'
    def page = execute(source: '<@efDefinitionList />', controllerClass: SampleParentController)

    then: 'the page is valid'
    checkPage(page)

    and: 'the correct controller is used'
    page.contains('<div id="sampleParentDefinitionList"></div>')
    def viewBlock = JavascriptTestUtils.extractBlock(page, '{view: "datatable"')
    TextUtils.findLine(viewBlock, 'url:').contains('/sampleParent/list')
  }

}
