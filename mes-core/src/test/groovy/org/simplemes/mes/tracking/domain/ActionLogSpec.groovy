package org.simplemes.mes.tracking.domain

import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.test.MESUnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ActionLogSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [SERVER]

  def "test constraints"() {
    given: 'no user in request'
    SecurityUtils.simulateNoUserInUnitTest = true

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain ActionLog
      requiredValues action: 'ABC', userName: 'TEST'
      maxSize 'action', FieldSizes.MAX_CODE_LENGTH
      notNullCheck 'action'
      notNullCheck 'userName'
    }

    cleanup:
    SecurityUtils.simulateNoUserInUnitTest = false
  }

  def "test default user name"() {
    given: 'a test user is simulated'
    setCurrentUser()

    when: 'a record is created'
    def al = new ActionLog()
    al.beforeValidate()

    then: 'the default user name is used'
    al.userName == SecurityUtils.currentUserName
  }

  def "test specific user name works"() {
    when: 'a record is created'
    def al = new ActionLog(userName: 'ABC')

    then: 'the specified user is used'
    al.userName == 'ABC'
  }

  @Rollback
  def "test use of order.product"() {
    given: 'a product for the order'
    def pn = new Product(product: 'PC').save()

    and: 'an order'
    def order = new Order(order: 'M001', product: pn).save()

    when: 'an action log record is saved'
    new ActionLog(order: order, action: 'RELEASE', userName: 'TEST').save()

    then: 'the product is used in the new record'
    ActionLog.list().get(0).product == pn
  }

  @Rollback
  def "verify that toShortString works with this domain"() {
    given: 'a simulated current user'
    setCurrentUser()

    when: 'an action log record is created with order/lsn'
    MESUnitTestUtils.releaseOrder(qty: 90.0, lsnTrackingOption: LSNTrackingOption.LSN_ONLY, lotSize: 100.0)
    //def lsn = order.lsns[0]

    then: 'the product is used in the new record'
    TypeUtils.toShortString(ActionLog.list().get(0))
  }
}
