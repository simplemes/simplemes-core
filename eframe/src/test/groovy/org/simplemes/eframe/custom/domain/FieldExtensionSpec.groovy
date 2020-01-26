/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import org.simplemes.eframe.custom.gui.FieldInsertAdjustment
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.exception.SimplifiedSQLException
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.UnitTestUtils
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
    //error.201.message="{1}" is not a legal custom field name.  Must be a legal Java variable name.
    assertValidationFails(fieldExtension, 201, 'fieldName', ['legal', 'abc=2'])
  }

  @Rollback
  def "verify that save prevents duplicate fieldName on same domain class"() {
    given: 'an existing field extension'
    new FieldExtension(fieldName: 'abc', domainClassName: FieldExtension.name).save()

    when: 'a duplicate field extension is validated'
    new FieldExtension(fieldName: 'abc', domainClassName: FieldExtension.name).save()

    then: 'the right exception is thrown'
    def ex = thrown(SimplifiedSQLException)
    UnitTestUtils.assertExceptionIsValid(ex, ['Unique'])
  }

  @Rollback
  def "verify that the constraint enforces valueClassName is required for Enumeration format types"() {
    given: 'a field extension with no valueClassName '
    def fieldExtension = new FieldExtension(fieldName: 'abc', domainClassName: FieldExtension.name,
                                            fieldFormat: EnumFieldFormat.instance)

    expect: 'missing valueClassName check is enforced'
    //error.1.message=Required value is missing "{0}".
    assertValidationFails(fieldExtension, 1, 'valueClassName', ['valueClassName', 'missing'])
  }

  @Rollback
  def "verify that the constraint enforces valueClassName is an Enumeration type"() {
    given: 'a field extension with a non-enum valueClassName '
    def fieldExtension = new FieldExtension(fieldName: 'abc', domainClassName: FieldExtension.name,
                                            fieldFormat: EnumFieldFormat.instance, valueClassName: String.name)

    expect: 'invalid valueClassName check is enforced'
    //error.202.message={0} ({1}) is not an enumeration.
    assertValidationFails(fieldExtension, 202, 'valueClassName', ['valueClassName', String.name, 'enum'])
  }

  @Rollback
  def "verify that the constraint enforces valueClassName is a valid class name"() {
    given: 'a field extension with bad valueClassName '
    def fieldExtension = new FieldExtension(fieldName: 'abc', domainClassName: FieldExtension.name,
                                            fieldFormat: EnumFieldFormat.instance, valueClassName: 'gibberish')

    expect: 'invalid valueClassName check is enforced'
    //error.4.message=Invalid "{0}" class.  Class "{1}" not found.
    assertValidationFails(fieldExtension, 4, 'valueClassName', ['valueClassName', 'gibberish', 'invalid', 'class'])
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
