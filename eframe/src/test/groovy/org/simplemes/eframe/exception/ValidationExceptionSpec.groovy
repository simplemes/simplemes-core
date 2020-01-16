package org.simplemes.eframe.exception

import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
class ValidationExceptionSpec extends BaseSpecification {

  def "verify that the constructor and toString works"() {
    given: 'some errors'
    def error1 = new ValidationError(104, 'XYZ', 'ABC')
    def error2 = new ValidationError(100, 'PDQ')

    when: 'the exception is created'
    def exception = new ValidationException([error1, error2])

    then: 'the exception has the correct values'
    exception.code == 3

    and: 'the toString contains the errors'
    UnitTestUtils.assertContainsAllIgnoreCase(exception.toStringLocalized(locale),
                                              [error1.toString(locale), error2.toString(locale)])

    where:
    locale        | _
    Locale.US     | _
    Locale.GERMAN | _
    null          | _

  }
}
