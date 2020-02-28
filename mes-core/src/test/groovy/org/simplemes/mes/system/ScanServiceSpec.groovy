package org.simplemes.mes.system

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.exception.MessageHolder
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.StartUndoAction
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.system.service.ScanService
import org.simplemes.mes.test.MESUnitTestUtils

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ScanServiceSpec extends BaseSpecification {
  @SuppressWarnings("unused")
  static specNeeds = SERVER

  /**
   * The scan service being tested.
   */
  ScanService service

  def setup() {
    setCurrentUser()
    service = Holders.getBean(ScanService)
/*
    service.resolveService = new ResolveService()
    service.orderService = new OrderService()
    service.workService = new WorkService()
    service.workService.resolveService = service.resolveService
    service.workService.productionLogService = new ProductionLogService()
*/
  }

  def "test scan with unresolved barcode"() {
    given: 'a scan request'
    def scanRequest = new ScanRequest(barcode: 'GIBBERISH')

    when: 'the scan is processed'
    def scanResponse = service.scan(scanRequest)

    then: 'the scan is returned as un-resolved'
    !scanResponse.resolved
    scanResponse.barcode == 'GIBBERISH'
  }

  def "test scan with encoded dashboard button barcode"() {
    given: 'a scan request'
    def scanRequest = new ScanRequest(barcode: '^BTN^START')

    when: 'the scan is processed'
    def scanResponse = service.scan(scanRequest)

    then: 'the scan is returned with an action to trigger a dashboard button'
    scanResponse.resolved
    scanResponse.scanActions[0].type == ButtonPressAction.TYPE_BUTTON_PRESS
    scanResponse.scanActions[0].button == 'START'
  }

  @Rollback
  def "test scan with order scanned"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder()
    //Order.findByOrder(order.order)  // Force the order to be read so the service can see it.

    and: 'a scan request'
    def scanRequest = new ScanRequest(barcode: order.order)

    when: 'the scan is processed'
    def scanResponse = service.scan(scanRequest)

    and: 'the order is re-read from the DB'
    order = Order.findByUuid(order.uuid)

    then: 'the scan is returned with the order'
    scanResponse.resolved
    scanResponse.order == order

    and: 'scan action tells the client to refresh the Order status with a type REFRESH_ORDER_STATUS'
    def refreshAction = scanResponse.scanActions.find {
      it.type == RefreshOrderStatusAction.TYPE_REFRESH_ORDER_STATUS
    }
    refreshAction.order == order.order

    and: 'scan action tells the client that the order changed'
    def orderChangedAction = scanResponse.scanActions.find { it.type == OrderLSNChangeAction.TYPE_ORDER_LSN_CHANGE }
    orderChangedAction.order == order.order
    orderChangedAction.qtyInQueue == order.qtyInQueue
    orderChangedAction.qtyInWork == order.qtyInWork

    and: 'order was started'
    order.qtyInQueue == 0.0
    order.qtyInWork == 1.0

    and: 'scan messages indicate the order was started'
    scanResponse.messageHolder.level == MessageHolder.LEVEL_INFO
    UnitTestUtils.assertContainsAllIgnoreCase(scanResponse.messageHolder.text, [order.order, '1'])

    and: 'the response has the right undo action'
    scanResponse.undoActions.size() == 1
    def undoAction = scanResponse.undoActions[0]
    undoAction instanceof StartUndoAction
    UnitTestUtils.assertContainsAllIgnoreCase(undoAction.JSON, [order.order, '1'])
    UnitTestUtils.assertContainsAllIgnoreCase(undoAction.infoMsg, [order.order, '1'])
  }

  def "test parseScan with badly formed internal format - odd number of values - 3"() {
    given: 'a scan request'
    def scanRequest = new ScanRequest(barcode: '^BTN^START^PRF')

    when: 'the scan is parsed'
    def parsedResponse = service.parseScan(scanRequest)

    then: 'the right values are returned'
    parsedResponse[ScanService.BARCODE_BUTTON] == 'START'
    !parsedResponse['PRF']
  }

  def "test parseScan with badly formed internal format - odd number of values - 1"() {
    given: 'a scan request'
    def scanRequest = new ScanRequest(barcode: '^BTN')

    when: 'the scan is parsed'
    def parsedResponse = service.parseScan(scanRequest)

    then: 'the right values are returned'
    !parsedResponse[ScanService.BARCODE_BUTTON]
  }

  def "test parseScan with unknown prefix"() {
    given: 'a scan request'
    def scanRequest = new ScanRequest(barcode: '^BTA^COMPLETE')

    when: 'the scan is parsed'
    def parsedResponse = service.parseScan(scanRequest)

    then: 'the right values are returned'
    parsedResponse['BTA'] == 'COMPLETE'
  }

  // TODO: Support barcode format when additions do
/*
  def "verify that the internal barcode format can be extended by an addition"() {
    given: "the getBarcodePrefixMapping method is extended to provide additional barcode prefixes"
    def script = """static customMethodX(Map results) {
                      results.PRD = 'PRODUCT'
                      //return "extended"
                    }"""
    AdditionTstUtils.buildArtefact([methods    : 'getBarcodePrefixMapping(target: ScanService, extension: this.&customMethodX, passResult: true)',
                                    otherSource: script, target: ScanService, imports: 'import org.simplemes.mes.system.*'])
    MethodExtensionHelper.setupMethodExtensions()

    when: "the core method is called"
    def map = service.getBarcodePrefixMapping()

    then: "the custom result is given"
    map['PRD'] == 'PRODUCT'

    and: "the core values are still in place"
    map['BTN'] == ScanService.BARCODE_BUTTON

    cleanup: 'cleanup the test addition artefact'
    AdditionTstUtils.cleanupAdditionArtefacts()

  }
*/

  def "verify that the parse scan map is stored in the result"() {
    given: 'a scan request'
    def scanRequest = new ScanRequest(barcode: '^BTN^START')

    when: 'the scan is processed'
    def scanResponse = service.scan(scanRequest)

    then: 'parsed map is stored in the response'
    scanResponse.parsedBarcode == [BUTTON: 'START']
  }

  // test scan order with no qty in queue/work

  // test scan with order - routing
  // test scan with order - in work
  // test scan with LSN
  // test scan with LSN - in work
  // test scan with LSN - routing
  // test scan with order in structured prefix.

  // GUI TESTS
  // test no action or message from scan() - displays unknown scan message
}
