package org.simplemes.mes.demand.domain

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.system.EnabledStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.mes.demand.CompleteRequest
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.OrderStatus
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
class OrderSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [HIBERNATE]

  def "test standard constraints"() {
    given: 'some initial data loaded'
    loadInitialData(OrderSequence)

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain Order
      requiredValues order: 'M1003', qtyToBuild: 2.0
      maxSize 'order', FieldSizes.MAX_CODE_LENGTH
      notNullCheck 'qtyToBuild'
      fieldOrderCheck false     // TODO: Re-enable when state is ported
      //notInFieldOrder (['operationStates', 'dateReleased', 'dateFirstQueued', 'dateFirstStarted', 'dateQtyQueued', 'dateQtyStarted'])
    }
  }

  @Rollback
  def "verify that toShortString and toString works on the order with child LSNs"() {
    // Some versions generated a stack over flow exception
    given: 'a simulated current user'
    setCurrentUser()

    and: 'an order'
    def order = MESUnitTestUtils.releaseOrder(qty: 90.0, lsnTrackingOption: LSNTrackingOption.LSN_ONLY, lotSize: 100.0)
    def lsn = order.lsns[0]

    expect: 'the constraints are enforced'
    TypeUtils.toShortString(order) == order.order
    TypeUtils.toShortString(lsn) == lsn.lsn
    order.toString()
  }

  @Rollback
  def "test local constraints "() {
    expect: 'negative qtyToBuild'
    def order1 = new Order(order: 'a', qtyToBuild: -1)
    !order1.validate()
    //noinspection SpellCheckingInspection
    order1.errors["qtyToBuild"].codes.contains('min.notmet.qtyToBuild')

    and: '0 qtyToBuild'
    def order2 = new Order(order: 'a', qtyToBuild: 0)
    !order2.validate()
    //noinspection SpellCheckingInspection
    order2.errors["qtyToBuild"].codes.contains('min.notmet.qtyToBuild')
  }

  @Rollback
  def "test new order name creation from OrderSequence"() {
    given: 'some initial data loaded'
    loadInitialData(OrderSequence)

    when: 'an order without an order name'
    Order order = new Order()
    order.save()

    then: 'the order is set from the sequence'
    order.order == 'M1000'
  }

  @Rollback
  def "test default status is used on creation"() {
    when: 'an order without a status'
    Order order = new Order(order: 'M1001')
    order.save()

    then: 'the default status is used '
    order.overallStatus == OrderStatus.default
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "wrong status types are detected on save"() {
    when: 'the wrong type is used'
    new Order(order: '1234', overallStatus: EnabledStatus).save()

    then: 'the right exception is thrown'
    thrown(Exception)
  }

  @Rollback
  def "valid LSNs can be saved with the order"() {
    given: 'an order with two LSNs saved'
    LSN sn1 = new LSN(lsn: '1234-001')
    LSN sn2 = new LSN(lsn: '1234-002')
    Order order = new Order(order: '1234', qtyToBuild: 2.0)
    order.addToLsns(sn1)
    order.addToLsns(sn2)
    order.save()

    when: 'the LSNs are re-read'
    order = Order.findByOrder('1234')

    then: 'the LSNs are still part of the order'
    order.lsns.size() == 2
    order.lsns[0] == sn1
    order.lsns[1] == sn2
  }

  @Rollback
  def "wrong number of LSNs can't be saved"() {
    given: 'an order with two LSNs saved and a qty of 3.0'
    LSN sn1 = new LSN(lsn: '1234-001')
    LSN sn2 = new LSN(lsn: '1234-002')
    Order order = new Order(order: '1234', qtyToBuild: 3.0)
    order.addToLsns(sn1)
    order.addToLsns(sn2)

    when: 'the order is checked'
    order.validate()

    then: 'the error is correct'
    def s = GlobalUtils.lookupValidationErrors(order).qtyToBuild[0]
    UnitTestUtils.assertContainsAllIgnoreCase(s, ['1234', '2', '3', 'wrong', 'number', 'LSNs'])
  }

  @Rollback
  def "duplicate LSNs can't be saved"() {
    given: 'an order with two identical LSNs saved'
    LSN sn1 = new LSN(lsn: '1234-001')
    LSN sn2 = new LSN(lsn: '1234-001')
    Order order = new Order(order: '1234', qtyToBuild: 2.0)
    order.addToLsns(sn1)
    order.addToLsns(sn2)

    when: 'the order is checked'
    order.validate()

    then: 'the error is correct'
    def s = GlobalUtils.lookupValidationErrors(order).lsns[0]
    UnitTestUtils.assertContainsAllIgnoreCase(s, ['1234', '1234-001', 'duplicate', 'LSN'])
  }

  @Rollback
  def "populateLSNs fails when there is no default LSN Sequence"() {
    given: 'no default LSN Sequence exists'
    LSNSequence.findDefaultSequence()?.delete(flush: true)

    and: 'an order'
    Order order = new Order(order: '1234', qtyToBuild: 5.0)
    assert order.validate()
    assert order.save()

    when: 'the LSN are created'
    order.populateLSNs()

    then: 'should fail with proper message'
    //error.102.message=Could not find expected default value for {0}.
    def e = thrown(BusinessException)
    assert UnitTestUtils.allParamsHaveValues(e)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['default'])
    e.code == 2009
  }

  @Rollback
  def "populateLSNs creates the correct LSNs with a lotSize=1"() {
    given: 'a product with a lot size of 1.0'
    Product product = new Product(product: 'PC', lotSize: 1.0).save()

    and: 'an order for multiple LSNs'
    Order order = new Order(order: '1234', qtyToBuild: 5.0, product: product)
    order.save()

    and: 'the LSN Sequence is loaded'
    loadInitialData(LSNSequence)

    when: 'the LSN are created'
    order.populateLSNs()

    then: 'the right LSNs are created'
    order.lsns.size() == 5
    order.lsns[0].qty == 1.0
  }

  @Rollback
  def "populateLSNs creates the correct LSNs with a lotSize>1 and is a fraction"() {
    given: 'a product with a lot size of 12.0'
    Product product = new Product(product: 'PC', lotSize: 1.2).save()

    and: 'an order for multiple LSNs'
    Order order = new Order(order: '1234', qtyToBuild: 11.0, product: product)
    order.save()

    and: 'the LSN Sequence is loaded'
    loadInitialData(LSNSequence)

    when: 'the LSN are created'
    order.populateLSNs()

    then: 'the right LSNs are created'
    order.lsns.size() == 10  // 9 LSNs=1.2 and last=0.2
    order.lsns[0].qty == 1.2
    order.lsns[9].qty == 0.2
  }

  @Rollback
  def "populateLSNs creates the correct LSNs when qtyToBuild is later increased"() {
    given: 'a product with a lot size of 1.0'
    Product product = new Product(product: 'PC', lotSize: 1.2).save()

    and: 'the LSN Sequence is loaded'
    loadInitialData(LSNSequence)

    and: 'an order for multiple LSNs has LSN populated once'
    Order order = new Order(order: '1234', qtyToBuild: 5.0, product: product)
    order.populateLSNs()
    order.save()

    when: 'the qtyToBuild is increased and more LSN are created'
    order.qtyToBuild = 10.0
    order.populateLSNs()

    then: 'the right LSNs are created'
    order.lsns.size() == 10
  }

  @Rollback
  def "verify that the fallback to the product lsnTrackingOption works"() {
    given: 'a product with a tracking option'
    Product product = new Product(product: 'PC', lsnTrackingOption: LSNTrackingOption.LSN_ALLOWED).save()

    when: 'an order is saved without a tracking option'
    Order order = new Order(order: '1234', qtyToBuild: 5.0, product: product).save()

    then: 'the orders option to be set'
    order.lsnTrackingOption == LSNTrackingOption.LSN_ALLOWED
  }

  @Rollback
  def "test that order can't be created with LSNs when LSNTrackingOption is order only"() {
    given: 'a product with a tracking option'
    Product product = new Product(product: 'PC', lsnTrackingOption: LSNTrackingOption.ORDER_ONLY)

    and: 'an order that also has LSNs'
    Order order = new Order(order: 'M001', qtyToBuild: 2.0, product: product)
    LSN sn1 = new LSN(lsn: 'M001-010')
    LSN sn2 = new LSN(lsn: 'M001-011')
    order.addToLsns(sn1)
    order.addToLsns(sn2)

    when: 'the order is validated'
    assert !order.validate()

    then: 'the correct error is returned'
    //order.lsns.notAllowed.error=LSNs not allowed for tracking option {3} provided for order {4}.
    def s = GlobalUtils.lookupValidationErrors(order).lsns[0]
    UnitTestUtils.assertContainsAllIgnoreCase(s, ['M001', lookup('lsnTrackingOption.O.label'), 'tracking option'])
  }

  @Rollback
  def "verify that the LSNTrackingOption in the order can't be changed after creation if the Product value changes."() {
    given: 'a product with a tracking option'
    Product product = new Product(product: 'PC', lsnTrackingOption: LSNTrackingOption.LSN_ONLY).save()

    and: 'an order for the product is saved'
    Order order = new Order(order: '1234', qtyToBuild: 5.0, product: product).save()
    assert order.lsnTrackingOption == LSNTrackingOption.LSN_ONLY

    and: 'the product tracking option is changed'
    product.lsnTrackingOption = LSNTrackingOption.ORDER_ONLY
    product.save()

    when: 'the order is re-saved'
    order.qtyToBuild = 4.0
    order.save()

    then: 'the original tracking option is still used by the order'
    order.lsnTrackingOption == LSNTrackingOption.LSN_ONLY
  }

  @Rollback
  def "test resolving specific LSNs with only qty in queue."() {
    given: 'a released order with LSNs'
    setCurrentUser()
    Order order = MESUnitTestUtils.releaseOrder(qty: 5, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)

    when: 'the resolver looks for LSN for 1.0 pieces'
    def lsns = order.resolveSpecificLSNs(1.0)

    then: 'the first LSN is returned'
    lsns[0] == order.lsns[0]
  }

  @Rollback
  def "test checkForOrderDone identifies when the order is fully done"() {
    given: 'an order that has all the qty done'
    Order order = new Order(order: 'M001', qtyToBuild: 1, qtyDone: 1).save()

    when: 'the order is checked'
    order.checkForOrderDone(new CompleteRequest(order: order))

    then: 'the order is marked as fully completed'
    order.dateCompleted
    order.overallStatus.done
  }

  @Rollback
  def "verify that findRelatedRecords finds the ActionLog records"() {
    given: 'an order that has all the qty done'
    setCurrentUser()
    Order order = new Order(order: 'M001', qtyToBuild: 1, qtyDone: 1).save()

    and: 'a related action log record is created'
    ActionLog actionLog = new ActionLog()
    actionLog.action = 'TEST'
    actionLog.qty = order.qtyToBuild
    actionLog.order = order
    actionLog.save()

    when: 'the related records are searched'
    def list = order.findRelatedRecords()

    then: 'the action log records are found'
    list.size() == 1
    list[0] == actionLog
  }


}
