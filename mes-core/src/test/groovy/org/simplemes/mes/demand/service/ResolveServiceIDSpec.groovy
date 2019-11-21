package org.simplemes.mes.demand.service

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.ResolveIDRequest
import org.simplemes.mes.demand.domain.LSNSequence
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog
import org.simplemes.mes.tracking.domain.ProductionLog

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the the resolveID method scenarios for the Resolve Service actions.
 */
class ResolveServiceIDSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, ProductionLog, Order, Product, LSNSequence]

  def setup() {
    setCurrentUser()
    loadInitialData(LSNSequence)
  }

  @Rollback
  def "test resolveID with order match"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.ORDER_ONLY)

    and: 'a resolve request with the right ID'
    ResolveIDRequest resolveIDRequest = new ResolveIDRequest(barcode: order.order)

    when: 'resolve is attempted'
    def resolveIDResponse = new ResolveService().resolveID(resolveIDRequest)

    then: 'the correct order is returned'
    resolveIDResponse.order == order

    and: 'the LSN is not returned'
    !resolveIDResponse.lsn

    and: 'the resolved flag is set correctly'
    resolveIDResponse.resolved
  }

  @Rollback
  def "test resolveID with LSN match"() {
    given: 'a released order with an LSN'
    def order = MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0]

    and: 'a resolve request with the right ID'
    ResolveIDRequest resolveIDRequest = new ResolveIDRequest(barcode: lsn.lsn)

    when: 'resolve is attempted'
    def resolveIDResponse = new ResolveService().resolveID(resolveIDRequest)

    then: 'the correct lsn is returned'
    resolveIDResponse.lsn == lsn

    and: 'the Order is not returned'
    !resolveIDResponse.order

    and: 'the resolved flag is set correctly'
    resolveIDResponse.resolved
  }

  @Rollback
  def "test resolveID with duplicate ID for LSN and Order - LSN returned"() {
    given: 'a released order with an LSN'
    def order = MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0]

    and: 'the LSN matches the order exactly'
    lsn.lsn = order.order
    lsn.save(flush: true)

    and: 'a resolve request with the right ID'
    ResolveIDRequest resolveIDRequest = new ResolveIDRequest(barcode: lsn.lsn)

    when: 'resolve is attempted'
    def resolveIDResponse = new ResolveService().resolveID(resolveIDRequest)

    then: 'the correct lsn is returned'
    resolveIDResponse.lsn == lsn

    and: 'the Order is not returned'
    !resolveIDResponse.order
  }

  @Rollback
  def "test resolveID with no object found"() {
    given: 'a resolve request with the a non-existent ID'
    ResolveIDRequest resolveIDRequest = new ResolveIDRequest(barcode: 'GIBBERISH')

    when: 'resolve is attempted'
    def resolveIDResponse = new ResolveService().resolveID(resolveIDRequest)

    then: 'resolve fails'
    !resolveIDResponse.resolved

    and: 'the Order and LSN are not returned'
    !resolveIDResponse.order
    !resolveIDResponse.lsn
  }

  // test with structured barcode (2D with delimiters).


}
