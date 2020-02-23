package org.simplemes.mes.demand

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.test.MESUnitTestUtils

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *  Tests.
 */
class OrderUtilsSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def setup() {
    setCurrentUser()
  }

@Rollback
  def "test resolveIdOrName with an order name"() {
    given: 'an order'
    def order = MESUnitTestUtils.releaseOrder()

    when: 'the name is used to resolve the order'
    def resolved = OrderUtils.resolveUuidOrName(order.order)

    then: 'the right value is returned'
    resolved.order == order.order
  }

  @Rollback
  def "test resolveIdOrName with an LSN name"() {
    given: 'an order with LSN'
    def order = MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0]

    when: 'the name is used to resolve the LSN'
    def resolved = OrderUtils.resolveUuidOrName(lsn.lsn)

    then: 'the right value is returned'
    resolved.lsn == lsn.lsn
  }

  @Rollback
  def "test resolveIdOrName with an order ID"() {
    given: 'an order'
    def order = MESUnitTestUtils.releaseOrder()

    when: 'the name is used to resolve the order'
    def resolved = OrderUtils.resolveUuidOrName(order.uuid.toString())

    then: 'the right value is returned'
    resolved.order == order.order
  }

  @Rollback
  def "test resolveIdOrName with an LSN ID"() {
    given: 'an order with LSN'
    def order = MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0]

    when: 'the name is used to resolve the LSN'
    def resolved = OrderUtils.resolveUuidOrName(lsn.uuid.toString())

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
    lsn.save()

    when: 'the name is used to resolve the LSN'
    def resolved = OrderUtils.resolveUuidOrName(lsn.lsn)

    then: 'the right value is returned'
    resolved.lsn == lsn.lsn
  }

  @Rollback
  def "test resolveIdOrName finds nothing"() {
    given: 'an order with LSN'
    MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)

    when: 'the name is used to resolve nothing'
    def resolved = OrderUtils.resolveUuidOrName('gibberish')

    then: 'the right value is returned'
    !resolved
  }

  @Rollback
  def "test resolveIdOrName fails with missing argument"() {
    when: 'null is used to resolve nothing'
    OrderUtils.resolveUuidOrName(null)

    then: 'an exception is triggered'
    def ex = thrown(Exception)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['id'])
  }

  @Rollback
  def "test resolveIdOrName fails with empty string argument"() {
    given: 'an order with LSN'
    MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)

    when: 'null is used to resolve nothing'
    def resolved = OrderUtils.resolveUuidOrName('')

    then: 'nothing is found'
    !resolved
  }

  @Rollback
  def "test resolveIdOrName fails with spaces argument"() {
    given: 'an order with LSN'
    MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)

    when: 'null is used to resolve nothing'
    def resolved = OrderUtils.resolveUuidOrName('  ')

    then: 'nothing is found'
    !resolved
  }

}

