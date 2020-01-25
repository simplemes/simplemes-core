/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain


import org.simplemes.eframe.custom.gui.FieldInsertAdjustment
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.annotation.Rollback

/**
 * Tests.
 */
class FieldExtensionSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [JSON, SERVER]

  @SuppressWarnings("unused")
  static dirtyDomains = [FieldGUIExtension, FieldExtension]

  def "verify that domain enforces constraints"() {
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain FieldExtension
      requiredValues fieldName: 'ABC', domainClassName: FieldExtension.name
      maxSize 'fieldName', FieldSizes.MAX_CODE_LENGTH
      maxSize 'fieldLabel', FieldSizes.MAX_LABEL_LENGTH
      maxSize 'domainClassName', FieldSizes.MAX_CLASS_NAME_LENGTH
      maxSize 'valueClassName', FieldSizes.MAX_CLASS_NAME_LENGTH

      notNullCheck 'fieldName'
      notNullCheck 'domainClassName'

      notInFieldOrder(['domainClassName'])
    }
  }

  @Rollback
  def "verify that validation on fieldName prevents illegal values - non-java names"() {
    given: 'a field extension with invalid fieldName'
    def fieldExtension = new FieldExtension(fieldName: 'abc=2', domainClassName: FieldExtension.name)

    expect: 'invalid field name check is enforced'
    !fieldExtension.validate()
    fieldExtension.errors.errorCount == 1
    def error = fieldExtension.errors.allErrors[0]
    error.codes.contains('invalidFieldName.fieldName')
  }

  def "verify that validation on fieldName prevents duplicate fieldName on same domain class"() {
    given: 'an existing field extension'
    FieldExtension.withTransaction {
      new FieldExtension(fieldName: 'abc', domainClassName: FieldExtension.name).save()
    }

    when: 'a duplicate field extension is validated'
    FieldExtension fieldExtension = null
    FieldExtension.withTransaction {
      fieldExtension = new FieldExtension(fieldName: 'abc', domainClassName: FieldExtension.name)
      assert !fieldExtension.validate()
    }
    then: 'invalid field name check is enforced'
    fieldExtension.errors.errorCount == 1
    def error = fieldExtension.errors.allErrors[0]
    error.codes.contains('unique.domainClassName')
  }

  @Rollback
  def "verify that the constraint enforces valueClassName is required for Enumeration format types"() {
    given: 'a field extension with no valueClassName '
    def fieldExtension = new FieldExtension(fieldName: 'abc', domainClassName: FieldExtension.name, fieldFormat: EnumFieldFormat.instance)

    expect: 'missing valueClassName check is enforced'
    !fieldExtension.validate()
    fieldExtension.errors.errorCount == 1
    def error = fieldExtension.errors.allErrors[0]
    error.codes.contains('missingValueClassName.valueClassName')
  }

  @Rollback
  def "verify that the constraint enforces valueClassName is an Enumeration type"() {
    given: 'a field extension with a non-enum valueClassName '
    def fieldExtension = new FieldExtension(fieldName: 'abc', domainClassName: FieldExtension.name,
                                            fieldFormat: EnumFieldFormat.instance, valueClassName: String.name)

    expect: 'invalid valueClassName check is enforced'
    !fieldExtension.validate()
    fieldExtension.errors.errorCount == 1
    def error = fieldExtension.errors.allErrors[0]
    error.codes.contains('invalidValueClassName.valueClassName')
    error.toString().contains(String.name)
  }

  @Rollback
  def "verify that the constraint enforces valueClassName is a valid class name"() {
    given: 'a field extension with bad valueClassName '
    def fieldExtension = new FieldExtension(fieldName: 'abc', domainClassName: FieldExtension.name,
                                            fieldFormat: EnumFieldFormat.instance, valueClassName: 'gibberish')

    expect: 'invalid valueClassName check is enforced'
    !fieldExtension.validate()
    fieldExtension.errors.errorCount == 1
    def error = fieldExtension.errors.allErrors[0]
    error.codes.contains('classNotFound.valueClassName')
    error.toString().contains('gibberish')
  }

  def "verify that delete removes any FieldGUIExtension hints - entire field gui extension record"() {
    given: 'a field extension with a GUI hint'
    FieldGUIExtension.withTransaction {
      new FieldExtension(fieldName: 'abc', domainClassName: FieldExtension.name).save()
      def fg = new FieldGUIExtension(domainName: FieldExtension.name)
      fg.adjustments = [new FieldInsertAdjustment(fieldName: 'abc', afterFieldName: 'code')]
      fg.save()
    }

    when: 'the field extension is deleted'
    FieldExtension.withTransaction {
      def fieldExtension = FieldExtension.findByFieldName('abc')
      fieldExtension.delete()
    }

    then: 'the FieldGUIExtension is cleaned up too'
    FieldExtension.withTransaction {
      assert FieldGUIExtension.list().size() == 0
      true
    }
  }

  def "test toString"() {
    given: 'a field extension'
    def fieldExtension = new FieldExtension(fieldName: 'XYZ', domainClassName: FieldExtension.name)

    expect: 'toString works'
    assert fieldExtension.toString()
  }
}
