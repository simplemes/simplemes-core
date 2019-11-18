package org.simplemes.eframe.custom.gui

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class FieldRemoveAdjustmentSpec extends BaseSpecification {
  def "verify that adjustment works - basic remove"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldRemoveAdjustment(fieldName: 'fieldY').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['fieldZ', 'fieldX']
  }

  def "verify that adjustment works - remove from start"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldRemoveAdjustment(fieldName: 'fieldZ').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['fieldY', 'fieldX']
  }

  def "verify that adjustment works - remove field not in list"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldRemoveAdjustment(fieldName: 'gibberish').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['fieldZ', 'fieldY', 'fieldX']
  }

  def "verify that adjustment works - remove from end"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldRemoveAdjustment(fieldName: 'fieldX').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['fieldZ', 'fieldY']
  }

  def "verify that removeField works"() {
    expect: 'the method returns the correct value'
    result == new FieldRemoveAdjustment(fieldName: fieldName).removeField(removeFieldName)

    where:
    fieldName | removeFieldName | result
    'custom1' | 'custom1'       | true
    'custom1' | 'dummy'         | false
    ''        | 'custom1'       | false
    null      | 'custom1'       | false
    null      | null            | true
  }

}
