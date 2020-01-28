/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain


import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import sample.domain.Order

/**
 * Tests.
 */
class EFrameJdbcRepositoryOperationsSpec extends BaseSpecification {
  @SuppressWarnings("unused")
  static dirtyDomains = [Order]

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

}
