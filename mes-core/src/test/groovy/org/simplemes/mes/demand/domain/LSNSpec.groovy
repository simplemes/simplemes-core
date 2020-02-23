package org.simplemes.mes.demand.domain


import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.LSNStatus
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class LSNSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, Order, Product]

  void setup() {
    waitForInitialDataLoad()
  }

def "test constraints"() {
  given: 'an order'
  def order = new Order()

  expect: 'the constraints are enforced'
  DomainTester.test {
    domain LSN
    requiredValues lsn: 'SN10024', order: order
    maxSize 'lsn', FieldSizes.MAX_LSN_LENGTH
    notNullCheck 'lsn'
    fieldOrderCheck false
  }
}

  @Rollback
  def "test default LSNStatus is found on save"() {
    given: 'an order with an LSN'
    LSN lsn = new LSN(lsn: 'SN1047')
    def order = new Order(order: 'M1047')
    order.lsns << lsn

    when: 'the LSN is saved'
    order.save()

    then: 'the status is set'
    lsn.status == LSNStatus.default
  }

  @Rollback
  def "test equals when LSN is same, but order is different"() {
    given: 'two LSNs with same LSN but different order'
    def sn1 = new LSN(lsn: 'SN1')
    def sn2 = new LSN(lsn: 'SN1')
    Order order1 = new Order(order: 'M001')
    Order order2 = new Order(order: 'M002')
    order1.lsns << sn1
    order2.lsns << sn2
    order1.save()
    order2.save()

    expect: 'the two LSNs are not equal'
    sn1 != sn2
  }

  @Rollback
  def "test validateStart fails with bad qty to make sure WorkStateTrait is used properly in LSN"() {
    given: 'a simulated current user'
    setCurrentUser()

    and: 'an LSN to start'
    def order = MESUnitTestUtils.releaseOrder(qty: 90.0, lsnTrackingOption: LSNTrackingOption.LSN_ONLY, lotSize: 100.0)
    def lsn = order.lsns[0]

    when: 'the qty is started'
    lsn.validateStartQty(100.2)

    then: 'an exception is thrown with the correct info'
    def ex = thrown(BusinessException)
    // error.3003.message=Quantity to start ({0}) must be less than or equal to the quantity in queue ({1})
    UnitTestUtils.allParamsHaveValues(ex)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['100.2', '90', 'quantity', 'queue', lsn.lsn])
    ex.code == 3003
  }

  @Rollback
  def "test validate complete with bad qty to make sure WorkStateTrait is used properly in LSN"() {
    given: 'a simulated current user'
    setCurrentUser()

    and: 'an LSN'
    def order = MESUnitTestUtils.releaseOrder(qty: 100.0, lsnTrackingOption: LSNTrackingOption.LSN_ONLY, lotSize: 100.0)
    def lsn = order.lsns[0]

    and: 'the qty is started'
    lsn.startQty(20.2)

    when: 'the qty is completed'
    lsn.validateCompleteQty(30.2)

    then: 'an exception is thrown with the correct info'
    def ex = thrown(BusinessException)
    // error.3008.message=Quantity to complete ({0}) must be less than or equal to the quantity in work ({1})
    UnitTestUtils.allParamsHaveValues(ex)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['30.2', '20.2', 'quantity', lsn.lsn])
    ex.code == 3008
  }

  @Rollback
  def "test determineNextWorkable for first operation"() {
    given: 'a simulated current user'
    setCurrentUser()

    and: 'a released order with LSNs to a routing'
    def order = MESUnitTestUtils.releaseOrder(id: 'WSS', qty: 1, operations: [1, 2, 3], lsns: ['SN001'],
                                              lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0]

    when: 'the determineNextWorkable() is called with the first operation'
    def workable = lsn.determineNextWorkable(lsn.operationStates[0])

    then: 'the correct workable is found'
    workable.sequence == 2
  }

  @Rollback
  def "test determineNextWorkable for last operation"() {
    given: 'a simulated current user'
    setCurrentUser()

    and: 'a released order with LSNs to a routing'
    def order = MESUnitTestUtils.releaseOrder(id: 'WSS', qty: 1, operations: [1, 2, 3], lsns: ['SN001'],
                                              lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0]

    when: 'the determineNextWorkable() is called with the last operation'
    def workable = lsn.determineNextWorkable(lsn.operationStates[2])

    then: 'the correct workable is found'
    workable == null
  }

  @Rollback
  def "test determineNextWorkable with no routing"() {
    given: 'a simulated current user'
    setCurrentUser()

    and: 'a released order with LSNs to a routing'
    def order = MESUnitTestUtils.releaseOrder(id: 'WSS', qty: 1, lsns: ['SN001'],
                                              lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0]

    when: 'the determineNextWorkable() is called with no operation'
    def workable = lsn.determineNextWorkable()

    then: 'the correct workable is found'
    workable == null
  }

  def "verify that toShortString works"() {
    expect: 'the method returns the LSN'
    new LSN(lsn: 'ABC').toShortString() == 'ABC'
  }

  @Rollback
  def "verify that the dates are populated on save"() {
    given: 'an order with an LSN'
    LSN lsn = new LSN(lsn: 'SN1047')
    def order = new Order(order: 'M1047')
    order.lsns << lsn

    when: 'the LSN is saved'
    order.save()

    then: 'the dates are set'
    UnitTestUtils.dateIsCloseToNow(lsn.dateCreated)
    UnitTestUtils.dateIsCloseToNow(lsn.dateUpdated)
  }

  def "verify that workStateTrait method saveChanges forces update of record with transactions"() {
    given: 'a simulated current user'
    setCurrentUser()

    and: 'an LSN that has qty in queue'
    Order order = null
    Order.withTransaction {
      order = MESUnitTestUtils.releaseOrder(qty: 1, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    }

    when: 'the LSN is started and saved'
    Order.withTransaction {
      def order2 = Order.findByOrder(order.order)
      order2.lsns[0].startQty(1.0)
    }

    then: 'the LSN is updated'
    Order.withTransaction {
      def order3 = Order.findByOrder(order.order)
      assert order3.lsns[0].qtyInQueue == 0.0
      true
    }
  }

}
