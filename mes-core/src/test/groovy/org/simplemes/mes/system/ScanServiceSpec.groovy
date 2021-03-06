package org.simplemes.mes.system

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.exception.MessageHolder
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.CompleteUndoAction
import org.simplemes.mes.demand.StartUndoAction
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.system.service.ScanService
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog
import org.simplemes.mes.tracking.domain.ProductionLog
import sample.SampleScanExtension

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
  static dirtyDomains = [ActionLog, ProductionLog, Order]

  /**
   * The scan service being tested.
   */
  ScanService service

  def setup() {
    setCurrentUser()
    service = Holders.getBean(ScanService)
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

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "test scan with order scanned - starts inQueue"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2)
    GlobalUtils.defaultLocale = locale

    and: 'a scan request'
    def scanRequest = new ScanRequest(barcode: order.order)

    when: 'the scan is processed'
    def scanResponse = service.scan(scanRequest)

    and: 'the order is re-read from the DB'
    order = Order.findByUuid(order.uuid)

    then: 'the scan is returned with the order'
    scanResponse.resolved
    scanResponse.order == order

    and: 'scan action tells the client to refresh the Order status with a type ORDER_LSN_STATUS_CHANGED'
    def refreshAction = scanResponse.scanActions.find {
      it.type == OrderLSNStatusChangedAction.TYPE_ORDER_LSN_STATUS_CHANGED
    }
    refreshAction.list[0].order == order.order

    and: 'scan action tells the client that the order changed'
    def orderChangedAction = scanResponse.scanActions.find { it.type == OrderLSNChangeAction.TYPE_ORDER_LSN_CHANGED }
    List list = orderChangedAction.list
    list[0].order == order.order
    list[0].qtyInQueue == order.qtyInQueue
    list[0].qtyInWork == order.qtyInWork

    and: 'order was started'
    order.qtyInQueue == 0.0
    order.qtyInWork == 1.2

    and: 'scan messages indicate the order was started'
    scanResponse.messageHolder.level == MessageHolder.LEVEL_INFO
    scanResponse.messageHolder.text == GlobalUtils.lookup('started.message', order.order, NumberUtils.formatNumber(order.qtyInWork, locale))

    and: 'the response has the right undo action'
    scanResponse.undoActions.size() == 1
    def undoAction = scanResponse.undoActions[0]
    undoAction instanceof StartUndoAction
    UnitTestUtils.assertContainsAllIgnoreCase(undoAction.JSON, [order.order, '1'])
    UnitTestUtils.assertContainsAllIgnoreCase(undoAction.infoMsg, [order.order, '1'])

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "test scan with order scanned - completes inWork"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2, qtyInWork: 1.2)
    GlobalUtils.defaultLocale = locale

    and: 'a scan request'
    def scanRequest = new ScanRequest(barcode: order.order)

    when: 'the scan is processed'
    def scanResponse = service.scan(scanRequest)

    and: 'the order is re-read from the DB'
    order = Order.findByUuid(order.uuid)

    then: 'the scan is returned with the order'
    scanResponse.resolved
    scanResponse.order == order

    and: 'scan action tells the client to refresh the Order status with a type ORDER_LSN_STATUS_CHANGED'
    def refreshAction = scanResponse.scanActions.find {
      it.type == OrderLSNStatusChangedAction.TYPE_ORDER_LSN_STATUS_CHANGED
    }
    refreshAction.list[0].order == order.order

    and: 'scan action tells the client that the order changed'
    def orderChangedAction = scanResponse.scanActions.find { it.type == OrderLSNChangeAction.TYPE_ORDER_LSN_CHANGED }
    List list = orderChangedAction.list
    list[0].order == order.order
    list[0].qtyInQueue == order.qtyInQueue
    list[0].qtyInWork == order.qtyInWork
    list[0].qtyDone == order.qtyDone

    and: 'order was started'
    order.qtyInQueue == 0.0
    order.qtyInWork == 0.0
    order.qtyDone == 1.2

    and: 'scan messages indicate the order was started'
    scanResponse.messageHolder.level == MessageHolder.LEVEL_INFO
    scanResponse.messageHolder.text == GlobalUtils.lookup('completed.message', order.order, NumberUtils.formatNumber(order.qtyDone, locale))

    and: 'the response has the right undo action'
    scanResponse.undoActions.size() == 1
    def undoAction = scanResponse.undoActions[0]
    undoAction instanceof CompleteUndoAction
    UnitTestUtils.assertContainsAllIgnoreCase(undoAction.JSON, [order.order, '1'])
    UnitTestUtils.assertContainsAllIgnoreCase(undoAction.infoMsg, [order.order, '1'])

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
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

  def "verify that the parse scan map is stored in the result"() {
    given: 'a scan request'
    def scanRequest = new ScanRequest(barcode: '^BTN^START')

    when: 'the scan is processed'
    def scanResponse = service.scan(scanRequest)

    then: 'parsed map is stored in the response'
    scanResponse.parsedBarcode == [BUTTON: 'START']
  }

  @Rollback
  def "verify that the scan extension point logic works in a running app server"() {
    given: 'the extension bean'
    def extensionBean = Holders.getBean(SampleScanExtension)
    extensionBean.preScanRequest = null

    when: 'the core service method is called'
    def scanRequest = new ScanRequest(barcode: '^BTN^START')
    def response = service.scan(scanRequest)

    then: 'the pre method was called'
    extensionBean.preScanRequest == scanRequest

    and: 'the post method was called'
    response.operationSequence == 237
  }

  @Rollback
  def "verify that the getBarcodePrefixMapping extension point logic works in a running app server"() {
    given: 'the extension bean'
    def extensionBean = Holders.getBean(SampleScanExtension)
    extensionBean.preBarcodeCalled = false

    when: 'the core service method is called'
    def response = service.getBarcodePrefixMapping()

    then: 'the pre method was called'
    extensionBean.preBarcodeCalled

    and: 'the post method was called'
    response.extension == 'XYZZY'
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
