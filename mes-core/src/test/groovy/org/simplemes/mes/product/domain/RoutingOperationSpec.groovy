package org.simplemes.mes.product.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
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
class RoutingOperationSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "test constraints"() {
    given: 'an oper'
    def mr = new MasterRouting()

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain RoutingOperation
      requiredValues title: 'n', routing: mr, sequence: 1
      maxSize 'title', FieldSizes.MAX_TITLE_LENGTH
      fieldOrderCheck false
    }
  }

  // TODO: Restore @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "constraint enforces sequence greater than 0"() {
    given: 'a routing operation'
    def r = new RoutingOperation(sequence: sequence)

    when: 'the object is validated'
    assert !r.validate()

    then: 'the right validation error is found'
    r.errors['sequence'].codes.contains(error)

    where:
    sequence | error
    0        | 'min.notmet.sequence'
    -1       | 'min.notmet.sequence'
  }

  // TODO: Restore @Rollback
  def "test compareTo works"() {
    expect: 'compareTo works'
    def r1 = new RoutingOperation(sequence: sequenceA)
    def r2 = new RoutingOperation(sequence: sequenceB)
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
    def r1 = new RoutingOperation(sequence: 100, title: 'XYZ')

    when: 'the copy constructor is used'
    def r2 = new RoutingOperation(r1)

    then: 'the fields are copied'
    r1.sequence == r2.sequence
    r1.title == r2.title
    r1 <=> r2 == 0
  }

  // TODO: Enable when custom fields supported.
/*
  def "copy constructor works with custom fields"() {
    given: 'custom field on the routing operation'
    new FieldExtension(domainClassName: RoutingOperation.name, fieldName: 'color').save()
    FieldExtensionHelper.setupFieldExtensionsInDomains(????.application)

    and: 'a routing operation with a custom field value'
    def r1 = new RoutingOperation(sequence: 100, title: 'XYZ')
    r1.color = 'green'


    when: 'the copy constructor is used'
    def r2 = new RoutingOperation(r1)

    then: 'the custom field is copied'
    r2.color == 'green'
  }
*/


}
