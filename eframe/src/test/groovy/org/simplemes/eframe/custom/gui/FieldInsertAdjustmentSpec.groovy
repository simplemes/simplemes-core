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
class FieldInsertAdjustmentSpec extends BaseSpecification {

  def "verify that basic insert works - insert at end"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'fieldY').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['fieldZ', 'fieldY', 'custom1', 'fieldX']
  }

  def "verify that basic insert works - insert at start"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: '-').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['custom1', 'fieldZ', 'fieldY', 'fieldX']
  }

  def "verify that basic insert works - insert after one that does not exist"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'gibberish').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['fieldZ', 'fieldY', 'fieldX', 'custom1']
  }

  def "verify that basic insert works - insert after last field"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'fieldX').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['fieldZ', 'fieldY', 'fieldX', 'custom1']
  }

  def "verify that removeField works"() {
    expect: 'the method returns the correct value'
    result == new FieldInsertAdjustment(fieldName: fieldName, afterFieldName: 'fieldX').removeField(removeFieldName)

    where:
    fieldName | removeFieldName | result
    'custom1' | 'custom1'       | true
    'custom1' | 'dummy'         | false
    ''        | 'custom1'       | false
    null      | 'custom1'       | false
    null      | null            | true
  }

}
