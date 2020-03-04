package org.simplemes.mes.demand.domain

import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.CRUDGUITester
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.page.OrderShowPage
import org.simplemes.mes.tracking.domain.ActionLog
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
class OrderGUISpec extends BaseGUISpecification {
  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, Order]

  def "verify that the standard pages work"() {
    expect: 'the pages work'
    CRUDGUITester.test {
      tester this
      domain Order
      recordParams order: 'M1001', qtyToBuild: 27.2, lsnTrackingOption: LSNTrackingOption.LSN_ALLOWED
      minimalParams order: 'X'
      readOnlyFields 'qtyReleased,qtyInQueue,qtyInWork,qtyDone'
    }
  }

  def "verify that release works via GUI"() {
    given: 'an order that can be released'
    def order = null
    Order.withTransaction {
      order = new Order(order: 'ABC', qtyToBuild: 437.2).save()
    }

    when: 'the order is released'
    login()
    to OrderShowPage, order
    releaseButton.click()
    waitFor {
      messages.text()
    }

    then: 'we make it to the show page with the message'
    messages.text().contains('437')

    and: 'the order is released'
    def order1 = Order.findByOrder('ABC')
    order1.qtyInQueue == 437.2
    order1.qtyReleased == 437.2
  }

  def "verify that release fails gracefully when the order cannot be released"() {
    given: 'an order that can be released'
    def order = null
    Order.withTransaction {
      order = new Order(order: 'ABC', qtyToBuild: 437.2, qtyReleased: 437.2).save()
    }

    when: 'the order is released'
    login()
    to OrderShowPage, order
    releaseButton.click()
    waitFor {
      messages.text()
    }

    then: 'we make it to the show page with the message'
    //error.3005.message=Can''t release more quantity.  All of the quantity to build ({0}) has been released.
    def msg = GlobalUtils.lookup('error.3005.message', null, 437.2)
    messages.text().contains(msg)
  }

}
