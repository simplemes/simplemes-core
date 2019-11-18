package org.simplemes.eframe.custom.gui

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *  Tests.
 */
class FieldReplaceAllAdjustmentSpec extends BaseSpecification {

  def "verify that basic replace works"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    expect: 'the field order is replaced with the new order'
    new FieldReplaceAllAdjustment(fieldNames: ['fieldA', 'fieldB']).apply(fieldOrder) == ['fieldA', 'fieldB']
  }

  def "verify that basic replace works with empty original order"() {
    given: 'an empty field order'
    def fieldOrder = []

    expect: 'the field order is replaced with the new order'
    new FieldReplaceAllAdjustment(fieldNames: ['fieldA', 'fieldB']).apply(fieldOrder) == ['fieldA', 'fieldB']
  }

  def "verify that basic replace works with empty new field order"() {
    given: 'a field order'
    def fieldOrder = ['fieldZ', 'fieldY', 'fieldX']

    expect: 'the field order is replaced with the new order'
    new FieldReplaceAllAdjustment(fieldNames: []).apply(fieldOrder) == []
  }

  def "verify that removeField works - remove from list of other fields"() {
    given: 'a field adjustment with some fields'
    def adjustment = new FieldReplaceAllAdjustment(fieldNames: ['fieldA', 'fieldB'])

    when: 'the a single field is removed from the list'
    def res = adjustment.removeField('fieldA')

    then: 'the field is removed'
    adjustment.fieldNames == ['fieldB']

    and: 'the result indicates the entire adjustment should not be removed'
    !res
  }

  def "verify that removeField works - remove from last field in the list"() {
    given: 'a field adjustment with some fields'
    def adjustment = new FieldReplaceAllAdjustment(fieldNames: ['fieldA'])

    when: 'the a single field is removed from the list'
    def res = adjustment.removeField('fieldA')

    then: 'the field is removed'
    adjustment.fieldNames == []

    and: 'the result indicates the entire adjustment should be removed'
    res
  }

}
