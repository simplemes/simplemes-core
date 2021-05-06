package org.simplemes.eframe.data.format

import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.reports.ReportTimeIntervalEnum
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
class EnumFieldFormatSpec extends BaseSpecification {

  def "verify that id and toString work and the format is registered in the BasicFieldFormat class"() {
    expect:
    EnumFieldFormat.instance.id == EnumFieldFormat.ID
    EnumFieldFormat.instance.toString() == 'Enum'
    EnumFieldFormat.instance.type == Object
    BasicFieldFormat.coreValues.contains(EnumFieldFormat)
  }

  def "verify that basic formatting works"() {
    expect:
    EnumFieldFormat.instance.format(value, Locale.US, null) == result

    where:
    value                                | result
    null                                 | ''
    ReportTimeIntervalEnum.LAST_6_MONTHS | 'LAST_6_MONTHS'
  }

  def "verify that the parseForm method works"() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([aField: ReportTimeIntervalEnum])
    def fieldDef = fieldDefinitions.aField

    expect: 'the value can be parsed'
    EnumFieldFormat.instance.parseForm('LAST_7_DAYS', Locale.US, fieldDef) == ReportTimeIntervalEnum.LAST_7_DAYS
  }

  def "verify that the parseForm method works within null value"() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([aField: ReportTimeIntervalEnum])
    def fieldDef = fieldDefinitions.aField

    expect: 'the value can be parsed'
    EnumFieldFormat.instance.parseForm(null, Locale.US, fieldDef) == null
  }

  def "verify that the encode methods fail"() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([aField: ReportTimeIntervalEnum])
    def fieldDef = fieldDefinitions.aField

    expect: 'the value can be encoded'
    EnumFieldFormat.instance.encode(ReportTimeIntervalEnum.LAST_7_DAYS, fieldDef) == ReportTimeIntervalEnum.LAST_7_DAYS.toString()
  }

  def "verify that the decode method works"() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([aField: ReportTimeIntervalEnum])
    def fieldDef = fieldDefinitions.aField

    expect: 'the value can be decoded'
    EnumFieldFormat.instance.decode('LAST_7_DAYS', fieldDef) == ReportTimeIntervalEnum.LAST_7_DAYS
  }

  def "verify that the getEditor method works "() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([aField: ReportTimeIntervalEnum])
    def fieldDef = fieldDefinitions.aField

    expect: 'the value can be parsed'
    EnumFieldFormat.instance.getGridEditor(fieldDef) == 'combo'
  }

  def "verify that the getValidValues method works "() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([aField: ReportTimeIntervalEnum])
    def fieldDef = fieldDefinitions.aField

    when: ' the list of values is retrieved'
    def list = EnumFieldFormat.instance.getValidValues(fieldDef)

    then: 'they contain the right values'
    list.size() > 2
    list.find { it.id == ReportTimeIntervalEnum.LAST_7_DAYS.toString() }
    list.find { it.id == ReportTimeIntervalEnum.YESTERDAY.toString() }

  }

  def "verify that the convertToJsonFormat works"() {
    expect: 'the conversion works'
    def fieldDefinition = new SimpleFieldDefinition(format: EnumFieldFormat.instance, type: ReportTimeIntervalEnum)
    def yesterday = ReportTimeIntervalEnum.YESTERDAY
    EnumFieldFormat.instance.convertToJsonFormat(yesterday, fieldDefinition) == yesterday.toString()
  }

  def "verify that the convertFromJsonFormat works"() {
    expect: 'the conversion works'
    def fieldDefinition = new SimpleFieldDefinition(format: EnumFieldFormat.instance, type: ReportTimeIntervalEnum)
    EnumFieldFormat.instance.convertFromJsonFormat(value, fieldDefinition) == result

    where:
    value                                       | result
    ReportTimeIntervalEnum.YESTERDAY.toString() | ReportTimeIntervalEnum.YESTERDAY
    ''                                          | null
    null                                        | null
  }


}
