package org.simplemes.mes.demand.domain

import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.CRUDGUITester
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.product.domain.Product
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
  static dirtyDomains = [Product/*, LSNSequence*/]

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

  // test release
  // test release error

}
