package org.simplemes.eframe.domain

import org.simplemes.eframe.test.BaseSpecification
import sample.domain.Order

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class PersistentPropertySpec extends BaseSpecification {

  def "verify that the field constructor works"() {
    given: 'a field'
    def field = Order.getDeclaredField('uuid')

    when: 'the constructor is used'
    def prop = new PersistentProperty(field)

    then: 'the values are correct'
    prop.name == 'uuid'
    prop.type == UUID
    !prop.nullable
    prop.field == field
  }

  def "verify that the field constructor works - column maxLength"() {
    given: 'a field'
    def field = Order.getDeclaredField('order')

    when: 'the constructor is used'
    def prop = new PersistentProperty(field)

    then: 'the values are correct'
    prop.name == 'order'
    prop.maxLength == 30
  }

  def "verify that the field constructor works - nullable field"() {
    given: 'a field'
    def field = Order.getDeclaredField('product')

    when: 'the constructor is used'
    def prop = new PersistentProperty(field)

    then: 'the values are correct'
    prop.type == String
    prop.nullable
  }

  def "verify that the map constructor works"() {
    when: 'the constructor is used'
    def prop = new PersistentProperty(name: 'ABC')

    then: 'the values are correct'
    prop.name == 'ABC'
  }
}
