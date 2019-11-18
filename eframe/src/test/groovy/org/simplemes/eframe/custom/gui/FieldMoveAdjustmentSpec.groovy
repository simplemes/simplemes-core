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
class FieldMoveAdjustmentSpec extends BaseSpecification {
  def "verify that adjustment works - move a field"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldMoveAdjustment(fieldName: 'fieldX', afterFieldName: 'fieldZ').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['fieldZ', 'fieldX', 'fieldY']
  }

  def "verify that adjustment works - move to start"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldMoveAdjustment(fieldName: 'fieldX', afterFieldName: '-').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['fieldX', 'fieldZ', 'fieldY']
  }

  def "verify that adjustment works - move field that does not exist"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldMoveAdjustment(fieldName: 'gibberish', afterFieldName: '-').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['fieldZ', 'fieldY', 'fieldX']
  }

  def "verify that adjustment works - move field to end"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldMoveAdjustment(fieldName: 'fieldY', afterFieldName: 'fieldX').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['fieldZ', 'fieldX', 'fieldY']
  }

  def "verify that adjustment works - move from start to middle"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldMoveAdjustment(fieldName: 'fieldZ', afterFieldName: 'fieldY').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['fieldY', 'fieldZ', 'fieldX']
  }

  def "verify that adjustment works - does nothing"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldMoveAdjustment(fieldName: 'fieldY', afterFieldName: 'fieldZ').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['fieldZ', 'fieldY', 'fieldX']
  }

  def "verify that adjustment works - move to start does nothing"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    when: 'the adjustment is applied'
    new FieldMoveAdjustment(fieldName: 'fieldZ', afterFieldName: '-').apply(fieldOrder)

    then: 'the field order is correct'
    fieldOrder == ['fieldZ', 'fieldY', 'fieldX']
  }

  def "verify that removeField works"() {
    expect: 'the method returns the correct value'
    result == new FieldMoveAdjustment(fieldName: fieldName, afterFieldName: 'fieldX').removeField(removeFieldName)

    where:
    fieldName | removeFieldName | result
    'custom1' | 'custom1'       | true
    'custom1' | 'dummy'         | false
    ''        | 'custom1'       | false
    null      | 'custom1'       | false
    null      | null            | true
  }


}
