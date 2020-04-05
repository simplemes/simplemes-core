package org.simplemes.mes.product


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
class ProductGUISpec extends BaseGUISpecification {

  def "verify that the standard pages work"() {
    given: 'some operations'
    def operations = [[sequence: 10, title: 'oper 10'], [sequence: 20, title: 'oper 20']]
    expect: 'the pages work'
    CRUDGUITester.test {
      tester this
      domain Product
      recordParams product: 'BIKE-27', lotSize: 27.2, lsnTrackingOption: LSNTrackingOption.LSN_ALLOWED,
                   operations: operations, description: 'a long description', title: 'a title'
      minimalParams product: 'X'
      listColumns 'product,title,lsnTrackingOption,lsnSequence,lotSize,masterRouting'
    }
  }


}
