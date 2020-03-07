/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

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
    checkPage(page)

    then: 'the correct fields are in the column list'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'columns: [')
    columnsBlock.contains('id: "order"')
    columnsBlock.contains('id: "product"')

    and: 'the ID is set'
    def listText = JavascriptTestUtils.extractBlock(page, '{view: "datatable"')
    JavascriptTestUtils.extractProperty(listText, 'id') == 'workListGrid237'
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
    checkPage(page)

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
