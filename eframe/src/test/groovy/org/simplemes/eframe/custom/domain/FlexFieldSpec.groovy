package org.simplemes.eframe.custom.domain

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.DomainTester

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class FlexFieldSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [HIBERNATE]

  def "verify that default values are set correctly"() {
    expect: 'the right defaults are used'
    FlexField field = new FlexField()
    field.fieldFormat == StringFieldFormat.instance
  }

  def "verify that the constraints work"() {
    def flexType = new FlexType(flexType: 'XYZ')
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain FlexField
      requiredValues flexType: flexType, fieldName: 'ABC'
      maxSize 'fieldName', FieldSizes.MAX_CODE_LENGTH
      notNullCheck 'flexType'
      notNullCheck 'fieldName'
      notInFieldOrder(['flexType'])
    }
  }

  @Rollback
  def "verify that invalid field names are detected"() {
    expect: 'the right defaults are used'
    def flexType = new FlexType(flexType: 'XYZ')
    FlexField field = new FlexField(flexType: flexType, fieldName: ' BAD')
    !field.validate()
    def error = field.errors["fieldName"]
    error.codes.contains('invalidFieldName')
    def errorsByField = GlobalUtils.lookupValidationErrors(field)
    errorsByField['fieldName'][0].contains('BAD')
  }

  @Rollback
  def "verify that equals and hash code work"() {
    given: 'a flex type'
    def flexType = DataGenerator.buildFlexType()

    and: 'some flex fields to compare'
    def field1a = new FlexField(flexType: flexType, fieldName: 'ABC')
    def field1b = new FlexField(flexType: flexType, fieldName: 'ABC')
    def field2 = new FlexField(flexType: flexType, fieldName: 'XYZ')

    expect: 'the identical fields match'
    field1a == field1b
    field1a.hashCode() == field1b.hashCode()

    and: 'non-identical objects do not match'
    field1a != field2
    field1a.hashCode() != field2.hashCode()
  }
}
