package org.simplemes.mes.system

import groovy.json.JsonSlurper
import org.openqa.selenium.Keys
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.dashboard.controller.DashboardTestController
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.service.WorkService
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.system.page.ScanDashboardPage
import org.simplemes.mes.system.service.ScanService
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog
import org.simplemes.mes.tracking.domain.ProductionLog
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
@SuppressWarnings(["GroovyUnusedDeclaration", "GroovyAssignabilityCheck"])
class ScanDashboardGUISpec extends BaseDashboardSpecification {

  WorkService workService

  /**
   * Semi-unique ID for records created by this test.
   */
  public static final String ID = 'SDG'

  @SuppressWarnings("GroovyUnusedDeclaration")
  static dirtyDomains = [ActionLog, ProductionLog, Order, Product]

  def setup() {
    workService = Holders.getBean(WorkService)
  }

  /**
   * Sends a scan text to the client, followed by the terminator (TAB).
   * @param barcode The barcode string.
   */
  void sendScan(String barcode) {
    sendKey(barcode)
    sendKey(Keys.TAB)
  }

  def "verify that the scan with order in queue starts the order"() {
    given: 'a dashboard with the display page from the scan dashboard'
    buildDashboard(defaults: ['/scan/scanActivity'])

    and: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2)

    and: 'the dashboard is displayed'
    displayDashboard(page: ScanDashboardPage)

    when: 'the order ID is typed as if scanned by the user'
    sendScan(order.order)
    waitFor {
      orderDiv.text() == order.order
    }

    then: 'the start happened'
    Order.withTransaction {
      order = Order.findByOrder(order.order)
    }
    order.qtyInQueue == 0.0
    order.qtyInWork == 1.2

    and: 'the message is displayed with the correct values'
    messages.text == GlobalUtils.lookup('started.message', order.order, NumberUtils.formatNumber(order.qtyInWork, currentLocale))

    and: 'the current order is displayed'
    orderDiv.text() == order.order

    and: 'the status of the order is displayed'
    orderStatusDiv.text().contains(lookup('inWork.status.message', null, [1.2]))
  }

  def "verify that scan publishes the actions from the server"() {
    given: 'a dashboard with the display page from the scan dashboard'
    buildDashboard(defaults: ['/scan/scanActivity', DashboardTestController.DISPLAY_EVENT_ACTIVITY])

    and: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2)

    and: 'the dashboard is displayed'
    displayDashboard(page: ScanDashboardPage)

    when: 'the order ID is typed as if scanned by the user'
    sendScan(order.order)
    waitFor {
      $('#events').text().contains('ORDER_LSN_CHANGED')
    }

    then: 'the change event is triggered and contains the correct values'
    def s = $('#events').text()
    def json = new JsonSlurper().parseText(TextUtils.findLine(s, 'ORDER_LSN_CHANGED'))
    json.type == 'ORDER_LSN_CHANGED'
    json.source == ScanService.EVENT_SOURCE
    json.list[0].order == order.order

    then: 'the refresh event is triggered and contains the correct values'
    def json2 = new JsonSlurper().parseText(TextUtils.findLine(s, 'REFRESH_ORDER_STATUS'))
    json2.type == 'REFRESH_ORDER_STATUS'
    json2.source == ScanService.EVENT_SOURCE
    json2.order == order.order
  }

  def "verify that the scan with bad order fails"() {
    given: 'a dashboard with the display page from the scan dashboard'
    buildDashboard(defaults: ['/scan/scanActivity'])

    and: 'the dashboard is displayed'
    displayDashboard(page: ScanDashboardPage)

    when: 'the bad order ID is typed as if scanned by the user'
    sendScan('gibberish')
    waitFor {
      messages.text()
    }

    then: 'an error is displayed'
    messages.text().contains(lookup('scanDashboard.couldNotFind.message', null, 'gibberish'))

    and: 'the current order is still blank'
    !orderDiv.text().contains('gibberish')
  }

  def "verify that the scan of button barcode works"() {
    given: 'a dashboard with the display page from the scan dashboard'
    buildDashboard(defaults: ['/scan/scanActivity'], buttons: [[url: '/work/completeActivity', buttonID: 'COMPLETE', panel: 'A']])

    and: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2)

    and: 'the dashboard is displayed'
    displayDashboard(page: ScanDashboardPage)

    and: 'the order ID is typed as if scanned by the user'
    sendScan(order.order)
    waitFor {
      orderDiv.text() == order.order
    }

    when: 'the complete button barcode is scanned'
    sendScan('^BTN^COMPLETE')
    waitFor {
      messages.text
    }

    then: 'the complete happened'
    def order2 = Order.findByOrder(order.order)
    order2.qtyInQueue == 0.0
    order2.qtyInWork == 0.0

    and: 'the message is displayed with the correct values'
    //completedToDone.message=Completed quantity {1} for {0}. All work is done.
    messages.text == GlobalUtils.lookup('completedToDone.message', order2.order,
                                        NumberUtils.formatNumber(order2.qtyToBuild, currentLocale))

    and: 'the current order is still displayed'
    orderDiv.text() == order2.order

    and: 'the status of the order is displayed correctly as unknown'
    orderStatusDiv.text().contains('...')
  }

  def "verify that the user can undo the scan-start of an order"() {
    given: 'a dashboard with the display page from the scan dashboard'
    buildDashboard(defaults: ['/scan/scanActivity'])

    and: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2)

    and: 'the dashboard is displayed'
    displayDashboard(page: ScanDashboardPage)

    when: 'the order ID is typed as if scanned by the user'
    sendScan(order.order)
    waitForRecordChange(order)

    and: 'the undo is triggered'
    undoButton.click()
    waitForRecordChange(order)

    then: 'the status of the order is displayed'
    orderStatusDiv.text().contains(lookup('inQueue.status.message', currentLocale))

    and: 'the current order is still displayed'
    orderDiv.text() == order.order

    and: 'the reversed message is displayed'
    messages.text().contains(lookup('reversedStart.message', currentLocale, order.order, 1.2))

    and: 'the undo button is now disabled'
    !undoButtonEnabled
  }

  def "verify that the scan dashboard activity passes the order with later scans"() {
    given: 'a dashboard with the display page from the scan dashboard'
    buildDashboard(defaults: ['/scan/scanActivity'])

    and: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2)

    and: 'the dashboard is displayed'
    displayDashboard(page: ScanDashboardPage)

    when: 'the order ID is typed as if scanned by the user'
    sendScan(order.order)
    waitFor {
      orderDiv.text() == order.order
    }

    and: 'the debug scan is sent to the extension'
    sendScan('^XYZZY^ORDER_TEST')

    then: 'the order is in the message'
    messages.text.contains('XYZZY')
    messages.text.contains(order.order)
  }


}
// TODO: Test Complete scan with undo.
