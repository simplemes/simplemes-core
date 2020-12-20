/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.annotation.Rollback

/**
 * Tests.
 */
class FlexFieldSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [SERVER]

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
    FlexField field = new FlexField(flexType: flexType, fieldName: ' 2BAD')
    assertValidationFails(field, 201, 'fieldName', ['legal', ' 2BAD'])
  }

  def "verify that invalid guiHints are detected"() {
    expect: 'the right defaults are used'
    def flexType = new FlexType(flexType: 'XYZ')
    FlexField field = new FlexField(flexType: flexType, fieldName: 'FIELD1', guiHints: 'asd; xyz')
    //error.209.message="{1}" is not a valid GUI Hint for field {2}.  The hint must be in the format ''name1="value" name2="value"'' format.  Error: {3}
    assertValidationFails(field, 209, 'guiHints', ['asd', 'xyz', 'name1="value"', 'near(0)', 'FIELD1'])
  }

  @Rollback
  def "verify that equals and hash code work"() {
    given: 'a flex type'
    def flexType = new FlexType(flexType: 'XYZ')

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

  @Rollback
  def "verify that the constraint enforces valueClassName is a valid class name"() {
    given: 'a field extension with bad valueClassName '
    def flexField = new FlexField(fieldName: 'abc', flexType: new FlexType(),
                                  fieldFormat: EnumFieldFormat.instance, valueClassName: 'gibberish')

    expect: 'invalid valueClassName check is enforced'
    //error.4.message=Invalid "{0}" class.  Class "{1}" not found.
    assertValidationFails(flexField, 4, 'valueClassName', ['valueClassName', 'gibberish', 'invalid', 'class'])
  }

  @Rollback
  def "verify that the constraint enforces non-null valueClassName on enum fields"() {
    given: 'a field extension with bad valueClassName '
    def flexField = new FlexField(fieldName: 'abc', flexType: new FlexType(),
                                  fieldFormat: EnumFieldFormat.instance)

    expect: 'invalid valueClassName check is enforced'
    //error.1.message=Required value is missing "{0}" ({1}).
    assertValidationFails(flexField, 1, 'valueClassName', ['valueClassName', 'missing', FlexField.simpleName])
  }

  @Rollback
  def "verify that findAllByFieldNameILike works"() {
    given: 'a flex type with some fields'
    def flexType = new FlexType(flexType: 'XYZ')

    and: 'some flex fields to compare'
    flexType.fields << new FlexField(flexType: flexType, fieldName: 'ABC')
    flexType.fields << new FlexField(flexType: flexType, fieldName: 'aBc')
    flexType.fields << new FlexField(flexType: flexType, fieldName: 'xyzAbc')
    flexType.save()

    when: 'the query is executed'
    def list = FlexField.findAllByFieldNameIlike('abc')

    then: 'the right elements are found'
    list.size() == 2
    for (field in list) {
      assert field.fieldName.toLowerCase() == 'abc'
    }
  }

}
