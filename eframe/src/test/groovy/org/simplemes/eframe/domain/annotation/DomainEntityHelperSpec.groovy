package org.simplemes.eframe.domain.annotation

import io.micronaut.transaction.SynchronousTransactionManager
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.Order
import sample.domain.OrderLine
import sample.domain.OrderRepository

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DomainEntityHelperSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [Order]
  //static specNeeds= SERVER

  def "verify that getRepository works for simple case"() {
    expect: 'the repository is found'
    DomainEntityHelper.instance.getRepository(Order) instanceof OrderRepository
  }

  def "verify that the annotation adds the static repository holder"() {
    when: ' the class is compiled'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      
      @DomainEntity(repository=sample.domain.OrderRepository)
      class TestClass {
        UUID uuid
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    //println "clazz = ${clazz.declaredFields}"
    //println "clazz = ${clazz.declaredMethods}"

    then: 'the field has the repository '
    clazz.repository instanceof OrderRepository
  }

  def "verify that save will create a record"() {
    when: ' the record is saved'
    def order = new Order('M1001')
    DomainEntityHelper.instance.save((DomainEntityInterface) order)

    then: 'the record is in the DB'
    def list = (List<Order>) DomainEntityHelper.instance.list(Order)
    list[0].uuid == order.uuid
  }

  def "verify that delete will delete a record"() {
    when: ' the record is saved'
    def order = new Order('M1001')
    DomainEntityHelper.instance.save((DomainEntityInterface) order)

    and: 'the record is deleted'
    DomainEntityHelper.instance.delete((DomainEntityInterface) order)

    then: 'the record is not in the DB'
    Order.list().size() == 0
  }

  def "verify that getTransactionManager works"() {
    expect: 'the method works'
    DomainEntityHelper.instance.getTransactionManager() instanceof SynchronousTransactionManager
  }

  def "verify that determineRepository works"() {
    expect: 'the method works'
    DomainEntityHelper.instance.determineRepository(Order) instanceof OrderRepository
  }

  def "verify that executeWrite works - commit"() {
    when: 'the method works'
    DomainEntityHelper.instance.executeWrite { status ->
      new Order('M1001').save()
    }

    then: 'there are no records in the DB'
    Order.list().size() == 1
  }

  def "verify that executeWrite works - rollback"() {
    when: 'the method works'
    DomainEntityHelper.instance.executeWrite { status ->
      new Order('M1001').save()
      status.setRollbackOnly()
    }

    then: 'there are no records in the DB'
    Order.list().size() == 0
  }

  @Rollback
  def "verify that lazyChildLoad works"() {
    given: 'a domain record with children'
    def order = new Order(order: 'M1001')
    order.save()
    def orderLine1 = new OrderLine(order: order, product: 'BIKE', sequence: 1).save()
    def orderLine2 = new OrderLine(order: order, qty: 2.0, product: 'WHEEL', sequence: 2).save()

    when: 'the lazy load is used'
    def list = (List<OrderLine>) DomainEntityHelper.instance.lazyChildLoad(order, 'orderLines', 'order', OrderLine)

    then: 'the record is in the DB'
    list.size() == 2
    list[0].uuid == orderLine1.uuid
    list[1].uuid == orderLine2.uuid

    and: 'the field has the list - the list is the same on later calls'
    DomainEntityHelper.instance.lazyChildLoad(order, 'orderLines', 'order', OrderLine).is(list)
  }

}
