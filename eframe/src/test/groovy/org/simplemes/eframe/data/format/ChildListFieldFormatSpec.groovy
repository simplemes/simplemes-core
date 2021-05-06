package org.simplemes.eframe.data.format

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ChildListFieldFormatSpec extends BaseSpecification {

  def "verify that id and toString work and the format is registered in the BasicFieldFormat class"() {
    expect:
    ChildListFieldFormat.instance.id == ChildListFieldFormat.ID
    ChildListFieldFormat.instance.toString() == 'ChildList'
    BasicFieldFormat.coreValues.contains(ChildListFieldFormat)
  }

  def "verify that the format methods fail"() {
    when: 'the method is called'
    ChildListFieldFormat.instance.format('', null, null)

    then: 'the right exception is thrown'
    thrown(UnsupportedOperationException)
  }

  def "verify that the parse methods fail"() {
    when: 'the method is called'
    ChildListFieldFormat.instance.parse('', null, null)

    then: 'the right exception is thrown'
    thrown(UnsupportedOperationException)
  }

  def "verify that the encode methods fail"() {
    when: 'the method is called'
    ChildListFieldFormat.instance.encode('', null)

    then: 'the right exception is thrown'
    thrown(UnsupportedOperationException)
  }

  def "verify that the decode methods fail"() {
    when: 'the method is called'
    ChildListFieldFormat.instance.decode('', null)

    then: 'the right exception is thrown'
    thrown(UnsupportedOperationException)
  }

  def "verify that the client ID is correct - mapped to enum list format on client"() {
    expect: 'the value is correct'
    ChildListFieldFormat.instance.clientFormatType == EnumFieldFormat.instance.clientFormatType
  }

}
