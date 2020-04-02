package org.simplemes.mes.demand

import groovy.json.JsonSlurper
import org.simplemes.eframe.dashboard.controller.DashboardTestController
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.page.WorkCenterSelectionDashboardPage
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog
import org.simplemes.mes.tracking.domain.ProductionLog
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
class ReverseStartWorkGUISpec extends BaseDashboardSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, ProductionLog, Order]

  def "verify that reverse start activity works - order level"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection'], buttons: ['/work/reverseStartActivity'])

    and: 'an order with a qty in work'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2, qtyInWork: 1.2)

    when: 'the dashboard is displayed'
    displayDashboard(page: WorkCenterSelectionDashboardPage)

    and: 'the order is filled in'
    orderLSNField.input.value(order.order)

    and: 'the start is performed'
    clickDashboardButton(0)
    waitForRecordChange(order)

    then: 'the order is updated in the DB'
    def order2 = Order.findByUuid(order.uuid)
    order2.qtyInQueue == order.qtyToBuild
    order2.qtyInWork == 0.0

    and: 'the info message displays the start message'
    //reversedStart.message=Reversed Start of quantity {1} for {0}.
    messages.text() == lookup('reversedStart.message', null, order.order, NumberUtils.formatNumber(order2.qtyInQueue))

    and: 'the undo is not available'
    !undoButtonEnabled
  }

  def "verify that reverseStart activity publishes the ORDER_LSN_STATUS_CHANGED event - order level"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection', DashboardTestController.DISPLAY_EVENT_ACTIVITY],
                   buttons: ['/work/reverseStartActivity'])
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2, qtyInWork: 1.2)

    when: 'the dashboard is displayed'
    displayDashboard(page: WorkCenterSelectionDashboardPage)

    and: 'the order is filled in'
    orderLSNField.input.value(order.order)

    and: 'the start is performed'
    clickDashboardButton(0)
    waitFor {
      $('#events').text().contains('ORDER_LSN_STATUS_CHANGED')
    }

    then: 'the event is triggered and contains the correct values'
    def s = $('#events').text()
    def json = new JsonSlurper().parseText(TextUtils.findLine(s, 'ORDER_LSN_STATUS_CHANGED'))
    json.type == 'ORDER_LSN_STATUS_CHANGED'
    json.source == '/work/reverseStartActivity'
    List list = json.list
    list.size() == 1
    list[0].order == order.order
  }

  def "verify that reverseStart activity gracefully handles missing order"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection'], buttons: ['/work/reverseStartActivity'])

    when: 'the dashboard is displayed'
    displayDashboard(page: WorkCenterSelectionDashboardPage)

    and: 'the start is performed'
    clickDashboardButton(0)
    waitFor {
      messages.text()
    }

    then: 'the message is correct'
    messages.text() == lookup('orderLSN.missing.message')

    and: 'is an error'
    messages.isError()
  }

  def "verify that reverseStart activity gracefully handles server-side error"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection'], buttons: ['/work/reverseStartActivity'])
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2)

    when: 'the dashboard is displayed'
    displayDashboard(page: WorkCenterSelectionDashboardPage)

    and: 'the order is filled in'
    orderLSNField.input.value(order.order)

    and: 'the start is performed'
    clickDashboardButton(0)
    waitFor {
      messages.text()
    }

    then: 'the message is correct'
    messages.text().contains('(3017)')
    messages.isError()
  }

}