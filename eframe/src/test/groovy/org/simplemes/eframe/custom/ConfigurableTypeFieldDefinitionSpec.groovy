/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom


import org.simplemes.eframe.custom.domain.FlexField
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import sample.domain.SampleParent

/**
 * Tests.
 */
class ConfigurableTypeFieldDefinitionSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [JSON, SERVER]

  def "verify that the field constructor works"() {
    given: 'a flex field'
    def flexField = new FlexField(fieldName: 'rmaID', fieldFormat: IntegerFieldFormat.instance,
                                  fieldLabel: 'RMA ID', maxLength: 237, sequence: 437,
                                  valueClassName: String.name)

    when: 'the field definition is created'
    def fieldDefinition = new ConfigurableTypeFieldDefinition(flexField, 'rmaType')

    then: 'the fields are correct'
    fieldDefinition.configTypeFieldName == 'rmaType'
    fieldDefinition.name == 'rmaType_rmaID'
    fieldDefinition.sequence == 437
    fieldDefinition.format == IntegerFieldFormat.instance
    fieldDefinition.label == 'RMA ID'
    fieldDefinition.maxLength == 237
    fieldDefinition.type == String
    fieldDefinition.guiHints == TextUtils.parseNameValuePairs(flexField.guiHints)
  }

  def 'verify that constructor fails if flexField is missing'() {
    when: 'the constructor is called'
    new ConfigurableTypeFieldDefinition(name: 'ack1').toString()

    then: 'a valid exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['configTypeFieldName', 'null'])
  }

  def "verify that get and setFieldValue works with a domain with ExtensibleFields annotation"() {
    given: 'a configurable field on a domain'
    def fieldDef = new ConfigurableTypeFieldDefinition(configTypeFieldName: 'rmaType', name: 'field1')

    and: 'a domain object'
    def sampleParent = new SampleParent()

    when: 'the value is set'
    fieldDef.setFieldValue(sampleParent, 'ABC')

    then: 'the values can be read'
    fieldDef.getFieldValue(sampleParent) == 'ABC'

    and: 'the value is stored in the domains custom fields holder with the prefix'
    sampleParent[ExtensibleFieldHolder.DEFAULT_FIELD_NAME].contains('"rmaType_field1"')
    !sampleParent[ExtensibleFieldHolder.DEFAULT_FIELD_NAME].contains('"field1"')
  }


}
