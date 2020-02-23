package org.simplemes.mes.product

import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.product.domain.MasterOperation
import org.simplemes.mes.product.domain.MasterRouting
import org.simplemes.mes.test.MESUnitTestUtils

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class RoutingTraitSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Rollback
  def "verify sort works"() {
    given: 'a routing with operations in random order'
    def routing = MESUnitTestUtils.buildMasterRouting(operations: [2, 4, 3, 5, 1])

    expect: 'the opers to be sorted'
    routing.operations[0].sequence == 1
    routing.operations[1].sequence == 2
    routing.operations[2].sequence == 3
    routing.operations[3].sequence == 4
    routing.operations[4].sequence == 5
  }

  @Rollback
  def "validate detects duplication operations"() {
    given: 'a routing with a duplicate oper sequence'
    def routing = new MasterRouting(routing: 'ROUTING_237')
    routing.operations << new MasterOperation(sequence: 1, title: "Prep")
    routing.operations << new MasterOperation(sequence: 823, title: "Test")
    routing.operations << new MasterOperation(sequence: 823, title: "Pack")

    when: 'the object is validated'
    def errors = DomainUtils.instance.validate(routing)

    then: 'the right validation error is found'
    //error.4003.message=Two or more routing operations have the same sequence {1} on routing "{2}".  Each sequence must be unique.
    UnitTestUtils.assertContainsError(errors, 4003, 'sequence', ['823', 'ROUTING_237'])
  }

  @Rollback
  def "determineNextOperation fails with bad input"() {
    given: 'a multi-step routing'
    def routing = MESUnitTestUtils.buildMasterRouting(operations: [1, 2, 3])

    when: 'determineNextOperation() is called with a bad sequence'
    routing.determineNextOperation(7)

    then: 'an exception is thrown with the correct info'
    def e = thrown(BusinessException)
    // error.4001.message=Operation Sequence {0} not found one routing {1}
    UnitTestUtils.allParamsHaveValues(e)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['Sequence', 'not found', '7', routing.routing])
    e.code == 4001
  }

  @Rollback
  def "validate detects no operations"() {
    given: 'a routing with no operations'
    def routing = new MasterRouting(routing: 'ABC')

    when: 'the object is validated'
    def errors = DomainUtils.instance.validate(routing)

    then: 'the right validation error is found'
    //error.4002.message=Operations missing for routing "{1}".  At least one operation is required.
    UnitTestUtils.assertContainsError(errors, 4002, 'operations', ['ABC'])
  }

  def "determineNextOperation finds the correct operation"() {
    given: 'a routing with multiple steps'
    def routing = new MasterRouting()
    routing.operations << new MasterOperation(sequence: 1)
    routing.operations << new MasterOperation(sequence: 2)
    routing.operations << new MasterOperation(sequence: 3)

    expect: 'the correct next step is found'
    routing.determineNextOperation(sequence) == nextSequence

    where:
    sequence | nextSequence
    1        | 2
    2        | 3
    3        | 0
  }
}
