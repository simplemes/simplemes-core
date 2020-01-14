package org.simplemes.eframe.domain.validate

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ValidationErrorSpec extends BaseSpecification {

  def "verify that simple constructor works - no arguments"() {
    when: 'the constructor is used'
    def error = new ValidationError(1, 'XYZ')

    then: 'the fields are correct'
    error.code == 1
    error.fieldName == 'XYZ'

    and: 'the toString works'
    //error.1.message=Required value is missing {0}.
    error.toString() == lookup('1.message', null, 'XYZ')
  }

  def "verify that simple constructor works - arguments"() {
    when: 'the constructor is used'
    def error = new ValidationError(104, 'XYZ', 'ABC')

    then: 'the fields are correct'
    error.code == 104
    error.fieldName == 'XYZ'
    //error.args == 'XYZ'

    and: 'the toString works'
    //error.104.message=Could not create {0} due to error {1}
    error.toString() == lookup('104.message', null, 'XYZ', 'ABC')
  }


}
