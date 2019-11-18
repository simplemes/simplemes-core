package org.simplemes.eframe.data.format

import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.system.DisabledStatus
import org.simplemes.eframe.system.EnabledStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockFieldDefinitions

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class EncodedTypeFieldFormatSpec extends BaseSpecification {

  def "verify that id and toString work and the format is registered in the BasicFieldFormat class"() {
    expect:
    EncodedTypeFieldFormat.instance.id == EncodedTypeFieldFormat.ID
    EncodedTypeFieldFormat.instance.toString() == 'EncodedType'
    BasicFieldFormat.coreValues.contains(EncodedTypeFieldFormat)
  }

  def "verify that basic formatting works"() {
    expect:
    EncodedTypeFieldFormat.instance.format(value, null, null) == result

    where:
    value                  | result
    null                   | ''
    EnabledStatus.instance | EnabledStatus.instance.toStringLocalized()
    'gibberish'            | 'gibberish'
  }

  def "verify that the parseForm method works within a transaction"() {
    given: 'a mock field definition'
    def fieldDefinitions = new MockFieldDefinitions([status: BasicStatus])
    def fieldDef = fieldDefinitions.status

    expect: 'the db value can be parsed to find the encoded type'
    EncodedTypeFieldFormat.instance.parseForm(EnabledStatus.instance.id, null, fieldDef) == EnabledStatus.instance
  }

  def "verify that the parseForm method works within null and unknown IDs"() {
    given: 'a mock field definition'
    def fieldDefinitions = new MockFieldDefinitions([status: BasicStatus])
    def fieldDef = fieldDefinitions.status

    expect: 'the null is handled gracefully'
    EncodedTypeFieldFormat.instance.parseForm(null, null, fieldDef) == null
    EncodedTypeFieldFormat.instance.parseForm('bad', null, fieldDef) == null
  }

  def "verify that the encode methods fail"() {
    expect: 'the encode passes'
    EncodedTypeFieldFormat.instance.encode(EnabledStatus.instance, null) == EnabledStatus.instance.id
  }

  def "verify that the decode methods fail"() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([aField: BasicStatus])
    def fieldDef = fieldDefinitions.aField

    expect: 'the encode passes'
    EncodedTypeFieldFormat.instance.decode(EnabledStatus.instance.id, fieldDef) == EnabledStatus.instance
  }

  def "verify that the getEditor method works "() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([aField: BasicStatus])
    def fieldDef = fieldDefinitions.aField

    expect:
    EncodedTypeFieldFormat.instance.getGridEditor(fieldDef) == 'combo'
  }

  def "verify that the getValidValues method works "() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([aField: BasicStatus])
    def fieldDef = fieldDefinitions.aField

    when: ' the list of values is retrieved'
    def list = EncodedTypeFieldFormat.instance.getValidValues(fieldDef)

    then: 'they contain the right values'
    list.size() == 2
    list[0] == EnabledStatus.instance
    list[1] == DisabledStatus.instance
  }

  def "verify that the convertToJsonFormat works"() {
    expect: 'the convert works'
    def fieldDefinition = new SimpleFieldDefinition(format: EncodedTypeFieldFormat.instance)
    EncodedTypeFieldFormat.instance.convertToJsonFormat(value, fieldDefinition) == value?.id

    where:
    value                   | _
    EnabledStatus.instance  | _
    DisabledStatus.instance | _
    null                    | _
  }

  def "verify that the convertFromJsonFormat works"() {
    expect: 'the convert works'
    def fieldDefinition = new SimpleFieldDefinition(format: EncodedTypeFieldFormat.instance, type: BasicStatus)
    EncodedTypeFieldFormat.instance.convertFromJsonFormat(value?.id, fieldDefinition) == value

    where:
    value                   | _
    EnabledStatus.instance  | _
    DisabledStatus.instance | _
    null                    | _
  }
}
