package org.simplemes.mes.demand

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.mes.test.MESUnitTestUtils

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
class OrderUtilsSpec extends BaseSpecification {

  static specNeeds = HIBERNATE

  def setup() {
    setCurrentUser()
  }

  @Rollback
  def "test resolveIdOrName with an order name"() {
    given: 'an order'
    def order = MESUnitTestUtils.releaseOrder()

    when: 'the name is used to resolve the order'
    def resolved = OrderUtils.resolveIdOrName(order.order)

    then: 'the right value is returned'
    resolved.order == order.order
  }

  @Rollback
  def "test resolveIdOrName with an LSN name"() {
    given: 'an order with LSN'
    def order = MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0]

    when: 'the name is used to resolve the LSN'
    def resolved = OrderUtils.resolveIdOrName(lsn.lsn)

    then: 'the right value is returned'
    resolved.lsn == lsn.lsn
  }

  @Rollback
  def "test resolveIdOrName with an order ID"() {
    given: 'an order'
    def order = MESUnitTestUtils.releaseOrder()

    when: 'the name is used to resolve the order'
    def resolved = OrderUtils.resolveIdOrName(order.id.toString())

    then: 'the right value is returned'
    resolved.order == order.order
  }

  @Rollback
  def "test resolveIdOrName with an LSN ID"() {
    given: 'an order with LSN'
    def order = MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0]

    when: 'the name is used to resolve the LSN'
    def resolved = OrderUtils.resolveIdOrName(lsn.id.toString())

    then: 'the right value is returned'
    resolved.lsn == lsn.lsn
  }

  @Rollback
  def "test resolveIdOrName with an order and LSN with the same name"() {
    given: 'an order with LSN'
    def order = MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0]

    and: 'the LSN matches the order exactly'
    lsn.lsn = order.order
    lsn.save(flush: true)

    when: 'the name is used to resolve the LSN'
    def resolved = OrderUtils.resolveIdOrName(lsn.lsn)

    then: 'the right value is returned'
    resolved.lsn == lsn.lsn
  }

  @Rollback
  def "test resolveIdOrName finds nothing"() {
    given: 'an order with LSN'
    MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)

    when: 'the name is used to resolve nothing'
    def resolved = OrderUtils.resolveIdOrName('gibberish')

    then: 'the right value is returned'
    !resolved
  }

  @Rollback
  def "test resolveIdOrName fails with missing argument"() {
    when: 'null is used to resolve nothing'
    OrderUtils.resolveIdOrName(null)

    then: 'an exception is triggered'
    def ex = thrown(Exception)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['id'])
  }

  @Rollback
  def "test resolveIdOrName fails with empty string argument"() {
    given: 'an order with LSN'
    MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)

    when: 'null is used to resolve nothing'
    def resolved = OrderUtils.resolveIdOrName('')

    then: 'nothing is found'
    !resolved
  }

  @Rollback
  def "test resolveIdOrName fails with spaces argument"() {
    given: 'an order with LSN'
    MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)

    when: 'null is used to resolve nothing'
    def resolved = OrderUtils.resolveIdOrName('  ')

    then: 'nothing is found'
    !resolved
  }

}

