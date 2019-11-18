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
class StringFieldFormatSpec extends BaseSpecification {

  def "verify that id and toString work and the format is registered in the BasicFieldFormat class"() {
    expect:
    StringFieldFormat.instance.id == StringFieldFormat.ID
    StringFieldFormat.instance.toString() == 'String'
    StringFieldFormat.instance.type == String
    BasicFieldFormat.coreValues.contains(StringFieldFormat)
  }

  def "verify that basic formatting works"() {
    expect:
    StringFieldFormat.instance.format(value, Locale.US, null) == result

    where:
    value | result
    null  | ''
    ''    | ''
    'abc' | 'abc'
  }

  def "verify that basic parsing works"() {
    expect:
    StringFieldFormat.instance.parse(value, Locale.US, null) == result

    where:
    value | result
    null  | null
    ''    | ''
    'abc' | 'abc'
  }

  def "verify that formatting for forms works"() {
    expect:
    StringFieldFormat.instance.formatForm(value, Locale.US, null) == result

    where:
    value | result
    null  | ''
    ''    | ''
    'abc' | 'abc'
  }

  def "verify that parsing for forms works"() {
    expect:
    StringFieldFormat.instance.parseForm(value, Locale.US, null) == result

    where:
    value | result
    null  | null
    ''    | ''
    'abc' | 'abc'
  }

  def "verify that basic decoding works"() {
    expect:
    StringFieldFormat.instance.decode(value, null) == result

    where:
    value | result
    null  | null
    ''    | ''
    'abc' | 'abc'
  }

  def "verify that basic encoding works"() {
    expect:
    StringFieldFormat.instance.encode(value, null) == result

    where:
    value | result
    null  | null
    ''    | ''
    'abc' | 'abc'
  }
}
