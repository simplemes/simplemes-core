/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data

import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.test.BaseSpecification
import sample.domain.Order
import sample.pogo.FindComponentResponseDetail

/**
 * Tests.
 */
class FieldDefinitionFactorySpec extends BaseSpecification {

  def "verify that buildFieldDefinition works - PersistentProperty case"() {
    when: 'the constructor is used'
    def field = FieldDefinitionFactory.buildFieldDefinition(DomainUtils.instance.getPersistentField(Order, 'order'))

    then: 'the correct field is created'
    field.name == 'order'
    field.type == String
    field.label == 'label.order'
    field.columnName == 'ordr'
  }

  def "verify that buildFieldDefinition works - field case"() {
    when: 'the constructor is used'
    def field = FieldDefinitionFactory.buildFieldDefinition(FindComponentResponseDetail.getDeclaredField('assemblyData'))

    then: 'the correct field is created'
    field.name == 'assemblyData'
    field.type == FlexType
    field.reference
  }

  def "verify that copy works - field case"() {
    given: 'an existing field definition'
    def original = new SimpleFieldDefinition(name: 'abc', type: String)

    when: 'the constructor is used'
    def fieldDef = FieldDefinitionFactory.copy(original, [format: DomainReferenceFieldFormat.instance])

    then: 'the correct field is created with the adjustments'
    fieldDef.name == 'abc'
    fieldDef.type == String
    fieldDef.format == DomainReferenceFieldFormat.instance
  }

}
