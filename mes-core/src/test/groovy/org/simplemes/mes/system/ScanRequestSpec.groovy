package org.simplemes.mes.system


import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.test.MESUnitTestUtils

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ScanRequestSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [SERVER]

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that the request can be created from a map from the input JSON"() {
    given: 'an order with LSNs'
    setCurrentUser()
    def order = MESUnitTestUtils.releaseOrder(qty: 90.0, lsnTrackingOption: LSNTrackingOption.LSN_ONLY, lotSize: 100.0)
    def lsn = order.lsns[0]

    when: 'the constructor is called with string values'
    def scanRequest = new ScanRequest(barcode: 'BC1', order: order.order, lsn: lsn.lsn)

    then: 'the request is created correctly'
    scanRequest.order == order
    scanRequest.lsn == lsn
  }

  @Rollback
  @SuppressWarnings(["GroovyAssignabilityCheck", "GroovyResultOfObjectAllocationIgnored"])
  def "verify that the request detects invalid order"() {
    when: 'the constructor is called with string values'
    new ScanRequest(barcode: 'BC1', order: 'gibberish')

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['order', 'gibberish'])
  }

  @Rollback
  @SuppressWarnings(["GroovyAssignabilityCheck", "GroovyResultOfObjectAllocationIgnored"])
  def "verify that the request detects invalid lsn"() {
    when: 'the constructor is called with string values'
    new ScanRequest(barcode: 'BC1', lsn: 'gibberish')

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['lsn', 'gibberish'])
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that the map constructor is lenient when invalid values are sent in"() {
    expect:
    new ScanRequest(barcode: 'BC1').barcode == 'BC1'
    new ScanRequest(barcode: 'BC1', gibberish: 'bad').barcode == 'BC1'
    new ScanRequest(barcode: 'BC1', order: '').barcode == 'BC1'
  }

  // order/LSN not exists
}
