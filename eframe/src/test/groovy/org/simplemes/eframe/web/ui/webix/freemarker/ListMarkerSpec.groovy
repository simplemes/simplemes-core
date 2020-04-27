/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.UnitTestUtils
import sample.controller.OrderController

/**
 * Tests.
 */
class ListMarkerSpec extends BaseMarkerSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "verify that this marker uses the columns option correctly"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="workList" dashboard="true">
        <@efList id="workListGrid237" columns="order,product,qtyToBuild,qtyReleased"/>
      </@efForm>
    """
    def page = execute(source: src, controllerClass: OrderController, dataModel: [params: [_variable: 'A']])

    then: 'the page is valid javascript'
    checkJavascript(page)

    then: 'the correct fields are in the column list'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'columns: [')
    columnsBlock.contains('id: "order"')
    columnsBlock.contains('id: "product"')

    and: 'the ID is set'
    def listText = JavascriptTestUtils.extractBlock(page, '{view: "datatable"')
    JavascriptTestUtils.extractProperty(listText, 'id') == 'workListGrid237'
  }

  def "verify that this marker passes the action button options to the widget"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="workList" dashboard="true">
        <@efList id="workListGrid237" columns="order,product"
          remove@buttonIcon="fa-minus-square"   
          remove@buttonLabel="remove.label"   
          remove@buttonHandler="console.log()"   
          remove@buttonEnableColumn="canBeRemoved" 
        />
      </@efForm>
    """
    def page = execute(source: src, controllerClass: OrderController, dataModel: [params: [_variable: 'A']])

    then: 'the page is valid javascript'
    checkJavascript(page)
    //println "page = $page"

    then: 'the button column is correct'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'columns: [')
    def actionsColumn = TextUtils.findLine(columnsBlock, 'id: "_actionButtons"')
    actionsColumn
  }

  def "verify that this marker passes the _pageSrc to the ListWidget for preferences"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="workList" dashboard="true">
        <@efList id="workListGrid237" columns="order,product,qtyToBuild,qtyReleased"/>
      </@efForm>
    """
    def page = execute(source: src, controllerClass: OrderController,
                       dataModel: [params: [_variable: 'A', _pageSrc: '/dashboard']])

    then: 'the page is valid javascript'
    checkJavascript(page)

    then: 'the correct fields are in the column list'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'columns: [')
    columnsBlock.contains('id: "order"')
    columnsBlock.contains('id: "product"')

    and: 'the ID is set'
    def listText = JavascriptTestUtils.extractBlock(page, '{view: "datatable"')
    JavascriptTestUtils.extractProperty(listText, 'id') == 'workListGrid237'
  }

  def "verify that the marker detects when not used inside of an efForm"() {
    when: 'the marker is built'
    def src = """
      <@efList columns="name,title"/>
    """
    execute(source: src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['efList', 'efForm'])
  }


}
