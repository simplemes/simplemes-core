package org.simplemes.mes.demand.domain

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.system.EnabledStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
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
  static specNeeds = SERVER

  def "test standard constraints"() {
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain Order
      requiredValues order: 'M1003', qtyToBuild: 2.0
      maxSize 'order', FieldSizes.MAX_CODE_LENGTH
      notNullCheck 'qtyToBuild'
      //fieldOrderCheck true
      notInFieldOrder(['operations', 'operationStates', 'dateReleased', 'dateFirstQueued', 'dateFirstStarted', 'dateQtyQueued', 'dateQtyStarted'])
    }
  }

  def "verify that an Order can be serialized and de-serialized to JSON"() {
    when: 'an order is serialized'
    def order = new Order(order: 'M1001')
    def s = Holders.objectMapper.writeValueAsString(order)

    then: 'the JSON can be de-serialized'
    def order2 = Holders.objectMapper.readValue(s, Order)
    order2.order == order.order
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
  def "verify that qty validations work"() {
    expect: 'negative qtyToBuild'
    def order1 = new Order(order: 'a', qtyToBuild: -237)
    def errors = DomainUtils.instance.validate(order1)
    //error.137.message=Invalid Value "{1}" for "{0}". Value should be greater than {2}.
    UnitTestUtils.assertContainsError(errors, 137, 'qtyToBuild', ['-237', '0'])

    and: '0 qtyToBuild'
    def order2 = new Order(order: 'a', qtyToBuild: 0)
    def errors2 = DomainUtils.instance.validate(order2)
    //error.137.message=Invalid Value "{1}" for "{0}". Value should be greater than {2}.
    UnitTestUtils.assertContainsError(errors2, 137, 'qtyToBuild', ['0'])

    and: 'negative qtyReleased'
    def order3 = new Order(order: 'a', qtyReleased: -437)
    def errors3 = DomainUtils.instance.validate(order3)
    //error.136.message=Invalid Value "{1}" for "{0}". Value should be greater than or equal to {2}.
    UnitTestUtils.assertContainsError(errors3, 136, 'qtyReleased', ['-437', '0'])
  }

  @Rollback
  def "verify that lsn child validations are made"() {
    when: 'the order is saved'
    def order = new Order(order: 'a', qtyToBuild: 1.0)
    order.lsns << new LSN()
    order.save()

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['missing', 'lsn'])
  }

  @Rollback
  def "verify that lsn child beforeValidate methods are called"() {
    when: 'the order is saved'
    def order = new Order(order: 'a', qtyToBuild: 1.0)
    order.lsns << new LSN(lsn: 'x')
    order.save()

    then: 'the LSN beforeValidate was called'
    def lsn = LSN.findByUuid(order.lsns[0].uuid)
    lsn.status != null
  }

  @Rollback
  def "test new order name creation from OrderSequence"() {
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
    order.lsns << sn1
    order.lsns << sn2
    order.save()

    when: 'the LSNs are re-read'
    order = Order.findByOrder('1234')

    then: 'the LSNs are still part of the order'
    order.lsns.size() == 2
    order.lsns[0] == sn1
    order.lsns[1] == sn2
  }

  @Rollback
  def "verify that wrong number of LSNs cannot be saved"() {
    given: 'an order with two LSNs saved and a qty of 3.0'
    LSN sn1 = new LSN(lsn: '1234-001')
    LSN sn2 = new LSN(lsn: '1234-002')
    Order order = new Order(order: '1234', qtyToBuild: 3.0)
    order.lsns << sn1
    order.lsns << sn2

    when: 'the order is checked'
    def errors = DomainUtils.instance.validate(order)

    then: 'the error is correct'
    //error.3013.message=Wrong number of LSNs {1} provided for order {2}.  Should be {3}.
    UnitTestUtils.assertContainsError(errors, 3013, 'lsns', ['2', '3', '1234'])
  }

  @Rollback
  def "duplicate LSNs can't be saved"() {
    given: 'an order with two identical LSNs saved'
    LSN sn1 = new LSN(lsn: '1234-001')
    LSN sn2 = new LSN(lsn: '1234-001')
    Order order = new Order(order: '1234', qtyToBuild: 2.0)
    order.lsns << sn1
    order.lsns << sn2

    when: 'the order is checked'
    def errors = DomainUtils.instance.validate(order)

    then: 'the error is correct'
    //error.3015.message=Duplicate LSN {1} provided for order {2}.
    UnitTestUtils.assertContainsError(errors, 3015, 'lsns', ['1234-001', '1234'])
  }

  /**
   * Updates the standard LSN sequence (SERIAL) with the given default sequence setting.
   * @param isDefault The new setting
   */
  def setDefaultLSNSequence(boolean isDefault) {
    LSNSequence.withTransaction {
      def sequence = LSNSequence.findBySequence('SERIAL')
      sequence.setDefaultSequence(isDefault)
      sequence.save()
    }
  }

  @Rollback
  def "populateLSNs fails when there is no default LSN Sequence"() {
    given: 'no default LSN Sequence exists'
    setDefaultLSNSequence(false)

    and: 'an order'
    Order order = new Order(order: '1234', qtyToBuild: 5.0)
    order.save()

    when: 'the LSN are created'
    order.populateLSNs()

    then: 'should fail with proper message'
    def e = thrown(BusinessException)
    //error.102.message=Could not find expected default value for {0}.
    UnitTestUtils.assertExceptionIsValid(e, ['LSNSequence'], 102)
  }

  @Rollback
  def "populateLSNs creates the correct LSNs with a lotSize=1"() {
    given: 'a product with a lot size of 1.0'
    Product product = new Product(product: 'PC', lotSize: 1.0).save()

    and: 'an order for multiple LSNs'
    Order order = new Order(order: '1234', qtyToBuild: 5.0, product: product)
    order.save()

    when: 'the LSN are created'
    order.populateLSNs()

    then: 'the right LSNs are created'
    order.lsns.size() == 5
    order.lsns[0].qty == 1.0

    cleanup: 'reset the default LSN Sequence flag for other tests'
    setDefaultLSNSequence(true)
  }

  @Rollback
  def "populateLSNs creates the correct LSNs with a lotSize>1 and is a fraction"() {
    given: 'a product with a lot size of 12.0'
    Product product = new Product(product: 'PC', lotSize: 1.2).save()

    and: 'an order for multiple LSNs'
    Order order = new Order(order: '1234', qtyToBuild: 11.0, product: product)
    order.save()

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
    order.lsns << sn1
    order.lsns << sn2

    when: 'the order is checked'
    def errors = DomainUtils.instance.validate(order)

    then: 'the error is correct'
    //error.3014.message=LSNs not allowed for LSN tracking option "{1}" on order {2}.
    UnitTestUtils.assertContainsError(errors, 3014, 'lsns', [LSNTrackingOption.ORDER_ONLY.toString(), 'M001'])
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
