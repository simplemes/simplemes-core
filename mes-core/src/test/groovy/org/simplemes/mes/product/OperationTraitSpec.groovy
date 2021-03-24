package org.simplemes.mes.product

import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.mes.product.domain.MasterOperation
import org.simplemes.mes.product.domain.MasterRouting

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class OperationTraitSpec extends BaseSpecification {

  @SuppressWarnings('unused')
  static specNeeds = GUI

  def "constraint enforces sequence greater than 0"() {
    given: 'a routing operation'
    def mr = new MasterRouting()
    def operation = new MasterOperation(sequence: sequence, masterRouting: mr, title: 'X')

    when: 'the object is validated'
    def errors = DomainUtils.instance.validate(operation)

    then: 'the right validation error is found'
    //error.137.message=Invalid Value "{1}" for "{0}". Value should be greater than {2}.
    UnitTestUtils.assertContainsError(errors, 137, 'sequence', [sequence.toString(), '0'])

    where:
    sequence | _
    0        | _
    -1       | _
    -237     | _
  }

  def "test compareTo works"() {
    expect: 'compareTo works'
    def r1 = new MasterOperation(sequence: sequenceA)
    def r2 = new MasterOperation(sequence: sequenceB)
    (r1 <=> r2) == result

    where:
    sequenceA | sequenceB | result
    1         | 2         | -1
    2         | 1         | 1
    2         | 2         | 0
  }


}
