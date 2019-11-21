package org.simplemes.mes.product.domain

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.mes.test.MESUnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests Routing, uses MasterRouting as a concrete test class.
 */
class RoutingSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [MasterRouting]

  @Rollback
  def "verify auto-sort of operations on save"() {
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
    def routing = new MasterRouting(routing: 'ROUTING')
    routing.addToOperations(new RoutingOperation(sequence: 1, title: "Prep"))
    routing.addToOperations(new RoutingOperation(sequence: 823, title: "Test"))
    routing.addToOperations(new RoutingOperation(sequence: 823, title: "Pack"))

    expect: 'validation to fail'
    !routing.validate()

    and: 'has the right error info'
    //routing.operations.duplicate.error=Two or more routing operations have the same sequence {3}.  Each sequence must be unique.
    def s = GlobalUtils.lookupValidationErrors(routing).operations[0]
    s.contains('823')
    s.contains('unique')
  }

  @Rollback
  def "validate detects no operations"() {
    given: 'a routing with no operations'
    def routing = new MasterRouting(routing: 'ABC')

    expect: 'validation to fail'
    !routing.validate()

    and: 'has the right error info'
    //routing.operations.noOperations.error=Operations missing for routing.  At least one operation is required.
    def s = GlobalUtils.lookupValidationErrors(routing).operations[0]
    assert s.contains('missing')
  }

  @Rollback
  def "determineNextOperation finds the correct operation"() {
    given: 'a routing with multiple steps'
    def routing = MESUnitTestUtils.buildMasterRouting(operations: [1, 2, 3])

    expect: 'the correct next step is found'
    routing.determineNextOperation(sequence) == nextSequence

    where:
    sequence | nextSequence
    1        | 2
    2        | 3
    3        | 0
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
}
