/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain.validate

import org.simplemes.eframe.test.BaseSpecification

/**
 * Tests.
 */
class ValidationErrorSpec extends BaseSpecification {

  def "verify that simple constructor works - no arguments"() {
    when: 'the constructor is used'
    def error = new ValidationError(1, 'XYZ', 'ABC')

    then: 'the fields are correct'
    error.code == 1
    error.fieldName == 'XYZ'

    and: 'the toString works'
    //error.1.message=Required value is missing "{0}" ({1}).
    error.toString() == lookup('error.1.message', null, 'XYZ', 'ABC')
    error.toString().contains('XYZ')
    error.toString().contains('ABC')
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
    error.toString() == lookup('error.104.message', null, 'XYZ', 'ABC')
    error.toString().contains('ABC')
    error.toString().contains('XYZ')
  }

  def "verify that toString with locale works"() {
    expect: 'the german message does not match the english message'
    def error = new ValidationError(100, 'PDQ')
    error.toString(Locale.GERMAN) != error.toString(Locale.US)
  }


}
