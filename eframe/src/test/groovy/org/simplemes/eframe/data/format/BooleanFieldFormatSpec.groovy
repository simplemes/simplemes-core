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
@SuppressWarnings("GroovyAssignabilityCheck")
class BooleanFieldFormatSpec extends BaseSpecification {

  def "verify that id and toString work and the format is registered in the BasicFieldFormat class"() {
    expect:
    BooleanFieldFormat.instance.id == BooleanFieldFormat.ID
    BooleanFieldFormat.instance.toString() == 'Boolean'
    BooleanFieldFormat.instance.type == Boolean
    BasicFieldFormat.coreValues.contains(BooleanFieldFormat)
  }

  def "test format"() {
    expect: 'the right value is returned'
    BooleanFieldFormat.instance.format(value, locale, null) == result

    where:
    value | locale         | result
    true  | Locale.US      | 'true'
    false | Locale.GERMANY | 'false'
    null  | Locale.GERMANY | null
  }

  def "test encode"() {
    expect: 'the right value is returned'
    BooleanFieldFormat.instance.encode(value, null) == result

    where:
    value | result
    true  | 'true'
    false | 'false'
  }

  def "test decode"() {
    expect: 'the right value is returned'
    BooleanFieldFormat.instance.decode(value, null) == result

    where:
    value | result
    'T'   | true
    't'   | true
    'F'   | false
    ''    | null
    null  | null
  }

  def "test parse"() {
    expect: 'the right value is returned'
    BooleanFieldFormat.instance.parse(value, null, null) == result

    where:
    value | result
    'T'   | true
    't'   | true
    'F'   | false
    '1'   | true
    'on'  | true
    '0'   | false
    'off' | false
    ''    | null
    null  | null
  }

  def "verify that the getGridEditor returns a standard date editor for this class"() {
    expect: 'the right editor is returned'
    BooleanFieldFormat.instance.getGridEditor() == 'checkbox'
  }

}
