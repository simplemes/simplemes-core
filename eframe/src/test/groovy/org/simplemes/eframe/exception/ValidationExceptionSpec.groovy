/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.exception

import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

/**
 *
 */
class ValidationExceptionSpec extends BaseSpecification {

  def "verify that the constructor and toString works"() {
    given: 'some errors'
    def error1 = new ValidationError(104, 'XYZ', 'ABC')
    def error2 = new ValidationError(100, 'PDQ')

    when: 'the exception is created'
    def exception = new ValidationException([error1, error2], "A Value")

    then: 'the exception has the correct values'
    exception.code == 3

    and: 'the toString contains the errors'
    //error.3.message=Validation Failed: {0} on {2} [{1}].
    UnitTestUtils.assertContainsAllIgnoreCase(exception.toStringLocalized(locale),
                                              [error1.toString(locale), error2.toString(locale),
                                               'failed', 'A Value', String.class.simpleName])

    where:
    locale        | _
    Locale.US     | _
    Locale.GERMAN | _
    null          | _
  }

}
