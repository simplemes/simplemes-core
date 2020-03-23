package org.simplemes.mes.demand

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.simplemes.eframe.dashboard.controller.DashboardTestController
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.eframe.test.page.GridModule
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
class WorkListGUISpec extends BaseDashboardSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, Order]

  def "verify that work list displays the orders correctly"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/workList/workListActivity'])
    List<Order> orders = null
    Order.withTransaction {
      orders = MESUnitTestUtils.releaseOrders(nOrders: 4, spreadQueuedDates: true)
    }

    when: 'the dashboard is displayed'
    displayDashboard()

    then: 'the list is correct'
    def workList = $("body").module(new GridModule(field: 'workListA'))
    workList.cell(0, 0).text() == orders[0].order
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that a basic list widget works - column resize is used on next display"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/workList/workListActivity'])

    when: 'the dashboard is displayed'
    displayDashboard()
    def workList = $("body").module(new GridModule(field: 'workListA'))
    def headers = workList.headers
    int origWidth = headers[1].width

    and: 'the column is resized by +50 pixels'
    interact {
      def offset = -5 - (headers[2].width / 2) as int
      moveToElement(headers[2], offset, 10)
      clickAndHold()
      moveByOffset(50, 0)
      release()
    }
    waitFor {
      nonZeroRecordCount(UserPreference)
    }

    headers = workList.headers
    int newWidth = headers[1].width

    then: 'the column is resized by roughly the right amount'
    Math.abs(newWidth - origWidth - 50) < 5

    when: 'the page is re-displayed'
    displayDashboard()
    workList = $("body").module(new GridModule(field: 'workListA'))

    then: 'the new column width is used'
    def headers2 = workList.headers
    int finalWidth = headers2[1].width
    Math.abs(finalWidth - origWidth - 50) < 5
  }

  def "verify that work list triggers the WORK_LIST_SELECTED event when user selects a row"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/workList/workListActivity', DashboardTestController.DISPLAY_EVENT_ACTIVITY])

    and: 'some orders to select from'
    List<Order> orders = null
    Order.withTransaction {
      orders = MESUnitTestUtils.releaseOrders(nOrders: 4, spreadQueuedDates: true)
    }

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the user selects one'
    def workList = $("body").module(new GridModule(field: 'workListA'))
    workList.cell(1, 0).click()

    then: 'the event is published'
    waitFor {
      $('#events').text().contains('WORK_LIST_SELECTED')
    }

    then: 'the event is triggered and contains the correct values'
    //     var list = [{order: rowData.order}];
    //    dashboard.sendEvent({type: 'WORK_LIST_SELECTED',source: "/workList/workListActivity",  list: list});
    def s = $('#events').text()
    def json = new JsonSlurper().parseText(s)
    json.type == 'WORK_LIST_SELECTED'
    json.source == '/workList/workListActivity'
    json.list.size() == 1
    //noinspection GroovyAssignabilityCheck
    json.list[0].order == orders[1].order
  }

  def "verify that the activity handles the ORDER_LSN_STATUS_CHANGED event - single Order case"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/workList/workListActivity', DashboardTestController.TRIGGER_EVENT_ACTIVITY])

    and: 'a test order'
    Order order = null
    Order.withTransaction {
      order = MESUnitTestUtils.releaseOrder()
    }

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the order is changed by another mechanism'
    Order.withTransaction {
      order = Order.findByUuid(order.uuid)
      order.qtyInWork = order.qtyInQueue
      order.qtyInQueue = 0.0
      order.save()
    }

    and: 'the event is triggered'
    def event = [type: 'ORDER_LSN_STATUS_CHANGED', source: 'abc', list: [[order: order.order]]]
    textField("eventSource").input.value(JsonOutput.toJson(event))
    clickButton('triggerEvent')
    waitForCompletion()

    then: 'the list is updated'
    def workList = $("body").module(new GridModule(field: 'workListA'))
    NumberUtils.parseNumber(workList.cell(0, 2).text() as String) == order.qtyInQueue
    NumberUtils.parseNumber(workList.cell(0, 3).text() as String) == order.qtyInWork
  }


  // When work center is supported in findWork.
  //   test WC on URL
  //   test WC change in workCenterSelection activity   WORK_CENTER_CHANGED
  // When implemented in controller, test column sorting - preferences

}