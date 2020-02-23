package org.simplemes.mes.product.domain

import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.test.MESUnitTestUtils

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
    }
  }

  @Rollback
  def "verify that validate uses validateOperations from trait"() {
    given: 'a routing with no operations'
    def routing = new MasterRouting(routing: 'ABC')

    when: 'the object is validated'
    def errors = DomainUtils.instance.validate(routing)

    then: 'the right validation error is found'
    //error.4002.message=Operations missing for routing "{1}".  At least one operation is required.
    UnitTestUtils.assertContainsError(errors, 4002, 'operations', ['ABC'])
  }

  @Rollback
  def "verify that save will call sortOperations from trait"() {
    given: 'a routing with operations in random order'
    def routing = MESUnitTestUtils.buildMasterRouting(operations: [2, 4, 3, 5, 1])

    expect: 'the opers to be sorted'
    routing.operations[0].sequence == 1
    routing.operations[1].sequence == 2
    routing.operations[2].sequence == 3
    routing.operations[3].sequence == 4
    routing.operations[4].sequence == 5
  }

}
