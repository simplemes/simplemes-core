/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain

import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.AllFieldsDomain
import sample.domain.CustomOrderComponent
import sample.domain.Order

/**
 * Tests.
 */
class EFrameJdbcRepositoryOperationsSpec extends BaseSpecification {
  @SuppressWarnings("unused")
  static dirtyDomains = [Order, AllFieldsDomain]

  void saveWithTransaction(boolean fail) {
    Order.withTransaction {
      new Order(order: 'SAMPLE1').save()
      if (fail) {
        throw new BusinessException(code: 1)
      }
      new Order(order: 'SAMPLE2').save()
    }
  }

  def "verify that save calls inside a explicit transaction will work - commit scenario"() {
    when: 'a save is made using withTransaction'
    saveWithTransaction(false)

    then: 'the records are saved'
    Order.list().size() == 2
  }

  def "verify that save calls inside a explicit transaction will work - fail scenario"() {
    when: 'a save is made using withTransaction'
    try {
      saveWithTransaction(true)
    } catch (BusinessException e) {
      assert e instanceof BusinessException
    }

    then: 'the records are not saved'
    Order.list().size() == 0
  }

  def "verify that insert outside of a transaction will fail"() {
    when: 'a save is made without a Transaction'
    new Order(order: 'M1001').save()

    then: 'the right exception is thrown'
    def ex = thrown(IllegalStateException)
    UnitTestUtils.assertExceptionIsValid(ex, ['active', 'transaction'])
  }

  def "verify that update outside of a transaction will fail"() {
    given: 'a saved domain'
    Order order = null
    Order.withTransaction {
      order = new Order(order: 'M1001').save()
    }
    when: 'an update is made without a Transaction'
    order.qtyToBuild = 2.0
    order.save()

    then: 'the right exception is thrown'
    def ex = thrown(IllegalStateException)
    UnitTestUtils.assertExceptionIsValid(ex, ['active', 'transaction'])
  }

  def "verify that delete outside of a transaction will fail"() {
    given: 'a saved domain'
    Order order = null
    Order.withTransaction {
      order = new Order(order: 'M1001').save()
    }
    when: 'an update is made without a Transaction'
    order.delete()

    then: 'the right exception is thrown'
    def ex = thrown(IllegalStateException)
    UnitTestUtils.assertExceptionIsValid(ex, ['active', 'transaction'])
  }

  @Rollback
  def "verify that updating foreign references works with null value - tests workAroundXXX"() {
    given: 'a saved record with and without a foreign reference'
    def afd = new AllFieldsDomain(name: 'AFD1').save()
    def order = new Order(order: 'M1001').save()
    def comp1 = new CustomOrderComponent(order: order, sequence: 1, product: 'PROD1', foreignReference: afd).save()
    def comp2 = new CustomOrderComponent(order: order, sequence: 2, product: 'PROD2').save()

    when: 'the record is updated'
    comp1.product = 'PROD1A'
    comp2.product = 'PROD2A'
    comp1.save()
    comp2.save()

    then: 'the record is saved'
    def comp1_a = CustomOrderComponent.findById(comp1.uuid)
    def comp2_a = CustomOrderComponent.findById(comp2.uuid)
    comp1_a.product == 'PROD1A'
    comp2_a.product == 'PROD2A'
  }

  @Rollback
  def "verify that queries with dates work correctly with a timezone - less than"() {
    given: 'a saved record with a date for now'
    def date = new Date()
    new AllFieldsDomain(name: 'AFD1', dateTime: date).save()

    when: 'the query is made'
    def list = AllFieldsDomain.findAllByDateTimeLessThan(new Date(date.time + 1))

    then: 'the record is found'
    list.size() == 1
    list[0].name == 'AFD1'
  }

  @Rollback
  def "verify that queries with dates work correctly with a timezone - greater than"() {
    given: 'a saved record with a date for now'
    def date = new Date()
    new AllFieldsDomain(name: 'AFD1', dateTime: date).save()

    when: 'the query is made'
    def list = AllFieldsDomain.findAllByDateTimeGreaterThan(new Date(date.time - 1))

    then: 'the record is found'
    list.size() == 1
    list[0].name == 'AFD1'
  }

  def "verify that update record by uuid fails if the record is not found - optimistic locking scenario"() {
    given: 'a record'
    def afd = null
    AllFieldsDomain.withTransaction {
      afd = new AllFieldsDomain(name: 'AFD1', version: 437).save()
    }

    and: 'another process has updated the record'
    AllFieldsDomain.withTransaction {
      def afd2 = AllFieldsDomain.findByUuid(afd.uuid)
      afd2.save()
    }

    when: 'the update on the original record is attempted'
    AllFieldsDomain.withTransaction {
      afd.save()
    }

    then: 'the right exception is thrown'
    def ex = thrown(OptimisticLockException)
    //error.210.message=Record updated by another user.  {0}: {1}, version: {2}.
    UnitTestUtils.assertExceptionIsValid(ex, ['update', 'another', 'AFD1', '438', AllFieldsDomain.simpleName])

    and: 'the toShortString logic is used for the message'
    !ex.toString().contains('title:')
  }

  def "verify that update record by uuid fails if the record is not found - no version field"() {
    given: 'a record with a UUID that does not exist'
    def order = null
    Order.withTransaction {
      order = new Order(order: 'SAMPLE1').save()
    }
    def orderComponent = new CustomOrderComponent(order: order, sequence: 537)
    orderComponent.uuid = UUID.randomUUID()

    when: 'the update is attempted'
    CustomOrderComponent.withTransaction {
      orderComponent.save()
    }

    then: 'the right exception is thrown'
    def ex = thrown(OptimisticLockException)
    //error.211.message=Record updated by another user.  {0}: {1}.
    UnitTestUtils.assertExceptionIsValid(ex, ['update', 'another', TypeUtils.toShortString(orderComponent), CustomOrderComponent.simpleName])
  }

}
