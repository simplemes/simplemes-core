package org.simplemes.mes.demand


import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.page.WorkCenterSelectionDashboardPage
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
class WorkGUISpec extends BaseDashboardSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, Order]

  def "verify that start activity works - order level"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection'], buttons: ['/work/startActivity'])
    def order = MESUnitTestUtils.releaseOrder()

    when: 'the dashboard is displayed'
    displayDashboard(page: WorkCenterSelectionDashboardPage)

    and: 'the order is filled in'
    orderLSNField.input.value(order.order)

    and: 'the start is performed'
    clickDashboardButton(0)
    waitForRecordChange(order)

    then: 'the order is updated in the DB'
    def order2 = Order.findByUuid(order.uuid)
    order2.qtyInQueue == 0.0
    order2.qtyInWork == order2.qtyReleased

    and: 'the info message displays the start message'
    //started.message=Started quantity {1} for {0}.
    messages.text() == lookup('started.message', null, order.order, NumberUtils.formatNumber(order2.qtyInWork))

/*
      var list = [{order: value}];
      var event = {type: 'ORDER_LSN_STATUS_CHANGED', source: 'startActivity', list: list};
      dashboard.sendEvent(event);
      dashboard.finished({panel: '${panel}', info: formatSuccessMessage(xhr.responseJSON)});

    //DashboardTestController.DISPLAY_EVENT_ACTIVITY
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
*/

  }

  // Success message with fractions - locale-specific.
  // missing order/lsn with i18n.
  // test event is sent
  // test failed event
  // test undo is populated correctly.
}