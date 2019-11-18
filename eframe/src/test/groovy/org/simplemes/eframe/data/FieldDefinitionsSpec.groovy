package org.simplemes.eframe.data

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
class FieldDefinitionsSpec extends BaseSpecification {

  def "verify that fields can be accessed easily by name"() {
    given: 'a field definitions with a simple field'
    def fieldDefs = new FieldDefinitions()
    fieldDefs.add(new SimpleFieldDefinition(name: 'count', type: Integer))

    expect: 'the type can be easily accessed'
    fieldDefs['count'].type == Integer
  }

  def "verify that leftShift will add a field"() {
    given: 'a field definitions'
    def fieldDefs = new FieldDefinitions()

    when: 'the left shift is used to add a field'
    fieldDefs << new SimpleFieldDefinition(name: 'count', type: Integer)

    then: 'the field is in the list'
    fieldDefs['count'].type == Integer
  }

  def "verify that clone creates a new list of fields"() {
    given: 'a field definitions with a simple field'
    def originalFieldDefs = new FieldDefinitions()
    originalFieldDefs.add(new SimpleFieldDefinition(name: 'count', type: Integer))

    when: 'the field definitions are cloned'
    def newFieldDefs = (FieldDefinitions) originalFieldDefs.clone()

    and: 'the new lists is modified'
    newFieldDefs.add(new SimpleFieldDefinition(name: 'qty', type: Integer))

    then: 'the original list is unchanged'
    originalFieldDefs.size() == 1
  }


}
