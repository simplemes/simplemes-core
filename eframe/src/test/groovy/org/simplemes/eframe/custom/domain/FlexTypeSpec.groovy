/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain


import groovy.json.JsonSlurper
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback

/**
 * Tests.
 */
class FlexTypeSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [SERVER]

  @Rollback
  def "verify that flex type can be serialized"() {
    given: 'a saved flex type'
    def flexType = new FlexType(flexType: 'ABC')
    flexType.fields << new FlexField(fieldName: 'TEST')
    flexType.save()

    expect: 'the JSON is correct'
    def s = Holders.objectMapper.writeValueAsString(flexType)
    def json = new JsonSlurper().parseText(s)
    json.flexType == 'ABC'

    and: 'toString works'
    flexType.toString()
    flexType.toStringLocalized()

    and: 'getValue works'
    flexType.value.is(flexType)
  }

  def "verify that the constraints work"() {
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain FlexType
      def flexField = new FlexField(fieldName: 'field')
      requiredValues flexType: 'ABC', fields: [flexField]
      maxSize 'flexType', FieldSizes.MAX_CODE_LENGTH
      maxSize 'category', FieldSizes.MAX_CODE_LENGTH
      maxSize 'title', FieldSizes.MAX_TITLE_LENGTH
      notNullCheck 'flexType'
      notNullCheck 'category'
    }
  }

  @Rollback
  def "verify that the minimum fields size constraint works"() {
    expect: 'the constraint is enforced'
    def flexType2 = new FlexType(flexType: 'PDQ')
    !flexType2.validate()
    def error = flexType2.errors["fields"]
    error.codes.contains('minSize.notmet.fields')
  }

  @Rollback
  def "verify that Unicode works"() {
    given: 'an object with unicode values'
    def flexType = new FlexType()
    flexType.flexType = UnitTestUtils.UNICODE_KEY_TEST_STRING
    flexType.title = UnitTestUtils.UNICODE_KEY_TEST_STRING
    flexType.addToFields(new FlexField(fieldName: 'TEST'))

    expect: 'the save works'
    flexType.save()
  }

  @Rollback
  def "verify that auto-assignment of sequence works - create case"() {
    given: 'a domain with some fields'
    def flexType = new FlexType(flexType: 'ABC')
    flexType.addToFields(new FlexField(fieldName: 'FIELD1'))
    flexType.addToFields(new FlexField(fieldName: 'FIELD2'))
    flexType.addToFields(new FlexField(fieldName: 'FIELD3'))

    when: 'the record is saved'
    flexType.save()

    then: 'the sequence is assigned'
    flexType.fields[0].fieldName == 'FIELD1'
    flexType.fields[0].sequence == 1
    flexType.fields[1].fieldName == 'FIELD2'
    flexType.fields[1].sequence == 2
    flexType.fields[2].fieldName == 'FIELD3'
    flexType.fields[2].sequence == 3
  }

  @Rollback
  def "verify that auto-assignment of sequence works - update case"() {
    given: 'a domain with some fields'
    FlexField field0 = new FlexField(fieldName: 'FIELD0')
    FlexField field4 = new FlexField(fieldName: 'FIELD4')
    def flexType = new FlexType(flexType: 'ABC')
    flexType.addToFields(new FlexField(fieldName: 'FIELD1'))
    flexType.addToFields(new FlexField(fieldName: 'FIELD2'))
    flexType.addToFields(new FlexField(fieldName: 'FIELD3'))

    when: 'the record is initially saved'
    flexType.save()

    then: 'the initial values are correct'
    flexType.fields[0].fieldName == 'FIELD1'
    flexType.fields[0].sequence == 1
    flexType.fields[1].fieldName == 'FIELD2'
    flexType.fields[1].sequence == 2
    flexType.fields[2].fieldName == 'FIELD3'
    flexType.fields[2].sequence == 3

    when: 'a new field is added at the end and beginning'
    field0.flexType = flexType
    flexType.fields.add(0, field0)
    flexType.addToFields(field4)
    flexType.save()

    then: 'the new records have a sequence'
    flexType.fields[0].fieldName == 'FIELD0'
    flexType.fields[0].sequence != null
    flexType.fields[4].fieldName == 'FIELD4'
    flexType.fields[4].sequence != null
  }

  def "verify that equals works"() {
    expect: 'the right result'
    (flexTypeA == flexTypeB) == results

    where:
    flexTypeA                                      | flexTypeB                                      | results
    new FlexType(flexType: 'ABC')                  | new FlexType(flexType: 'XYZ')                  | false
    new FlexType(flexType: 'ABC', version: 2)      | new FlexType(flexType: 'ABC', version: 3)      | true
    new FlexType(flexType: 'ABC', title: 'XYZ')    | new FlexType(flexType: 'ABC', title: 'PDQ')    | true
    new FlexType(flexType: null)                   | new FlexType(flexType: null)                   | true
    new FlexType(flexType: 'ABC', category: 'XYZ') | new FlexType(flexType: 'ABC', category: 'PDQ') | true
  }

  @Rollback
  def "verify that determineInputFields works for basic types"() {
    given: 'a flex type'
    def flexType = new FlexType(flexType: 'ABC')
    flexType.addToFields(new FlexField(fieldName: 'NUMBER', fieldFormat: BigDecimalFieldFormat.instance))
    flexType.addToFields(new FlexField(fieldName: 'Boolean', fieldFormat: BooleanFieldFormat.instance))
    flexType.save()

    when: 'the field definitions are extracted'
    List<FieldDefinitionInterface> fieldDefinitions = flexType.determineInputFields('rmaType')

    then: 'the fields are correct'
    fieldDefinitions.size() == 2
    fieldDefinitions[0].name == 'rmaType_NUMBER'
    fieldDefinitions[0].configTypeFieldName == 'rmaType'
    fieldDefinitions[0].format == BigDecimalFieldFormat.instance
    fieldDefinitions[1].name == 'rmaType_Boolean'
    fieldDefinitions[1].configTypeFieldName == 'rmaType'
    fieldDefinitions[1].format == BooleanFieldFormat.instance
  }

  @Rollback
  def "verify that getFieldSummary works first and later calles"() {
    given: 'a flex type'
    def flexType = new FlexType(flexType: 'ABC')
    flexType.addToFields(new FlexField(fieldName: 'NUMBER', fieldFormat: BigDecimalFieldFormat.instance))
    flexType.addToFields(new FlexField(fieldName: 'Boolean', fieldFormat: BooleanFieldFormat.instance))

    expect: 'the getFieldSummary works'
    flexType.getFieldSummary().contains('NUMBER')
    flexType.getFieldSummary().contains('Boolean')
  }
}
