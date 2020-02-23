package org.simplemes.mes.product.domain


import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.mes.misc.FieldSizes

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests for Routings.  These tests are done on the sub-class MasterRouting since it is a standalone entity.
 */
class MasterRoutingSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [MasterRouting]

  def "verify domain constraints"() {
    given: 'an oper'
    def o1 = new MasterOperation(sequence: 1, title: "Prep")

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain MasterRouting
      requiredValues routing: 'routing', operations: [o1]
      maxSize 'routing', FieldSizes.MAX_PRODUCT_LENGTH
      maxSize 'title', FieldSizes.MAX_TITLE_LENGTH
      notNullCheck 'routing'
      fieldOrderCheck false
    }
  }

}
