package org.simplemes.mes.assy.demand

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class FindComponentAssemblyStateRequestSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "verify that the order constructor works"() {
    given: 'an order'
    def order = new Order(order: 'ABC')

    when: 'the constructor is used'
    def request = new FindComponentAssemblyStateRequest(order)

    then: 'the order is used'
    request.demand == order
  }

  def "verify that the LSN constructor works"() {
    given: 'an LSN'
    def lsn = new LSN(lsn: 'ABC')

    when: 'the constructor is used'
    def request = new FindComponentAssemblyStateRequest(lsn)

    then: 'the lsn is used'
    request.demand == lsn
  }

  @Rollback
  def "verify that the Map constructor works - Order"() {
    given: 'an order'
    def order = new Order(order: 'ABC').save()

    when: 'the constructor is used'
    def request = new FindComponentAssemblyStateRequest([order: order.order, hideAssembled: 'true'])

    then: 'the order is used'
    request.demand == order
    request.hideAssembled
  }

  @Rollback
  def "verify that the Map constructor works - LSN"() {
    given: 'an LSN'
    def order = new Order(order: 'ABC').save()
    def lsn = new LSN(lsn: 'XYZ', order: order).save()

    when: 'the constructor is used'
    def request = new FindComponentAssemblyStateRequest([lsn: lsn.lsn, hideAssembled: 'false'])

    then: 'the order is used'
    request.demand == lsn
    !request.hideAssembled
  }

  @Rollback
  def "verify that the Map constructor detects error - LSN is not part of order"() {
    given: 'an LSN'
    def order1 = new Order(order: 'ABC1').save()
    def order2 = new Order(order: 'ABC2').save()
    def lsn = new LSN(lsn: 'XYZ', order: order1).save()

    when: 'the constructor is used'
    new FindComponentAssemblyStateRequest([order: order2.order, lsn: lsn.lsn]).hideAssembled

    then: 'the right exception is thrown'
    //error.3009.message=LSN {0} is not part of Order {1}
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, [order2.order, lsn.lsn], 3009)
  }

  @Rollback
  def "verify that the Map constructor silently ignores when no order or LSN is given"() {
    when: 'the constructor is used'
    def request = new FindComponentAssemblyStateRequest([:])

    then: 'no error is triggered'
    request != null
  }

  @Rollback
  def "verify that the Map constructor detects error - order not found"() {
    when: 'the constructor is used'
    new FindComponentAssemblyStateRequest([order: 'gibberish']).hideAssembled

    then: 'the right exception is thrown'
    //error.110.message=Could not find {0} {1}
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['gibberish', lookup('order.label')], 110)
  }

  @Rollback
  def "verify that the Map constructor detects error - LSN not found"() {
    when: 'the constructor is used'
    new FindComponentAssemblyStateRequest([lsn: 'gibberish']).hideAssembled

    then: 'the right exception is thrown'
    //error.110.message=Could not find {0} {1}
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['gibberish', lookup('lsn.label')], 110)
  }

  @Rollback
  def "verify that the Map constructor detects error - non-unique LSN"() {
    given: 'an LSN'
    def order1 = new Order(order: 'ABC1').save()
    def lsn1 = new LSN(lsn: 'XYZ', order: order1).save()
    new LSN(lsn: 'XYZ', order: order1).save()

    when: 'the constructor is used'
    new FindComponentAssemblyStateRequest([lsn: lsn1.lsn]).hideAssembled

    then: 'the right exception is thrown'
    //error.3011.message=More than one LSN matches "{0}". {1} LSNs exist with the same ID.
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, [lsn1.lsn, '2'], 3011)
  }

}
