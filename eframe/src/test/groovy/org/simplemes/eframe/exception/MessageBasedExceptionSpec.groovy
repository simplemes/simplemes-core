package org.simplemes.eframe.exception

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class MessageBasedExceptionSpec extends BaseSpecification {

  def "verify that basic toString works with parameters"() {
    when: 'an exception is converted to a string'
    //error.100.message=Missing parameter {0}.
    def e = new MessageBasedException(100, ['Order'])
    def s = e.toStringLocalized(Locale.ENGLISH)

    then: 'the text is correct'
    UnitTestUtils.assertContainsAllIgnoreCase(s, ['Order', 'missing', '100'])
    e.code == 100
  }

  def "verify that toStringLocalized works with parameters"() {
    when: 'an exception is converted to a string'
    //error.100.message=Fehlender Parameter {0}
    def e = new MessageBasedException(100, ['Order'])
    def s = e.toStringLocalized(Locale.GERMAN)

    then: 'the text is correct'
    UnitTestUtils.assertContainsAllIgnoreCase(s, ['Order', 'Fehlender', '100'])
    e.code == 100
  }

  def "verify that toString works with missing error code"() {
    when: 'an exception is converted to a string'
    //error.0.message=Missing Error Code.
    def e = new MessageBasedException()
    def s = e.toStringLocalized(Locale.ENGLISH)

    then: 'the text is correct'
    UnitTestUtils.assertContainsAllIgnoreCase(s, ['Code', '0'])
    e.code == 0
  }

  def "verify that toString works with missing parameters"() {
    when: 'an exception is converted to a string'
    //error.100.message=Missing parameter {0}.
    def e = new MessageBasedException(100, [])

    then: 'the text is correct'
    !UnitTestUtils.allParamsHaveValues(e)
  }

  def "verify that the map constructor works"() {
    when: 'an exception is converted to a string'
    //error.100.message=Missing parameter {0}.
    def e = new MessageBasedException(code: 100, params: ['Order'])
    def s = e.toStringLocalized(Locale.ENGLISH)

    then: 'the text is correct'
    UnitTestUtils.assertContainsAllIgnoreCase(s, ['Order', 'missing', '100'])
    e.code == 100
  }

}
