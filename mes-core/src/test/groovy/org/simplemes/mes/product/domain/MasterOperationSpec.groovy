package org.simplemes.mes.product.domain

import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.misc.FieldSizes

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class MasterOperationSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

def "test constraints"() {
  given: 'an oper'
  def mr = new MasterRouting()

  expect: 'the constraints are enforced'
  DomainTester.test {
    domain MasterOperation
    requiredValues title: 'n', masterRouting: mr, sequence: 1
    maxSize 'title', FieldSizes.MAX_TITLE_LENGTH
    fieldOrderCheck false
  }
}

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

  @Rollback
  def "copy constructor works"() {
    given: 'a routing operation'
    def r1 = new MasterOperation(sequence: 100, title: 'XYZ')

    when: 'the copy constructor is used'
    def r2 = new MasterOperation(r1)

    then: 'the fields are copied'
    r1.sequence == r2.sequence
    r1.title == r2.title
    r1 <=> r2 == 0
  }

  @Rollback
  def "copy constructor works with custom fields"() {
    given: 'custom field on the routing operation'
    DataGenerator.buildCustomField(fieldName: 'color', domainClass: MasterOperation)

    and: 'a routing operation with a custom field value'
    def r1 = new MasterOperation(sequence: 100, title: 'XYZ')
    r1.setFieldValue('color', 'green')

    when: 'the copy constructor is used'
    def r2 = new MasterOperation(r1)

    then: 'the custom field is copied'
    r2.getFieldValue('color') == 'green'
  }


}
