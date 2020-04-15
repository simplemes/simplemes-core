/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.annotation

import com.fasterxml.jackson.annotation.JsonProperty
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.AllFieldsDomain
import sample.domain.RMA

/**
 * Tests.
 */
class ExtensibleFieldHolderTransformationSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  /**
   * Convenience method that compiles a class for testing. The class is called TestClass.
   * This method builds the boiler plate around the class contents passed in.
   * @param classContents The groovy source of the class contents.  Everything inside of the class XYZ { . . .}.
   * @return The compiled class.
   */
  static Class compileSimpleClass(String classContents) {
    def src = """
      package sample

      import org.simplemes.eframe.data.annotation.*
      import org.simplemes.eframe.domain.annotation.*
      import org.simplemes.eframe.custom.domain.*
      
      @DomainEntity
      class TestClass {
        $classContents
        UUID uuid
      }
    """

    return CompilerTestUtils.compileSource(src)
  }

  @Rollback
  def "verify that the annotation with real compiled class"() {
    given: "an instance with custom fields and a configurable type - rmaType"
    def flexType = DataGenerator.buildFlexType(fieldCount: 2)
    def rma = new RMA()
    rma.rmaType = flexType

    when: 'the values are set'
    rma.setRmaTypeValue('FIELD1', 'VALUE1')
    rma.setRmaTypeValue('FIELD2', 'VALUE2')

    then: 'the values can be read'
    rma.getRmaTypeValue('FIELD1') == 'VALUE1'
    rma.getRmaTypeValue('FIELD2') == 'VALUE2'

    and: 'the custom field accessor methods work'
    rma.setFieldValue('xyz', 'pdq')
    rma.getFieldValue('xyz') == 'pdq'
  }

  def "verify that the annotation creates correct fields and methods"() {
    given: "compile a class with the annotation"
    def clazz = compileSimpleClass("@ExtensibleFieldHolder String customFields")

    when: "an instance is made and values are stored in the added field"
    def instance = clazz.newInstance()

    then: "the storage field works."
    instance.setFieldValue('xyz', 'pdq')
    instance.getFieldValue('xyz') == 'pdq'

    and: "the custom field getter has the correct Jackson annotation to rename the field"
    def getter = clazz.getDeclaredMethod('getCustomFields')
    def annotation = getter.annotations.find { it.annotationType() == JsonProperty }
    annotation.value() == '_customFields'

    and: "the custom field name is created as a static field"
    instance[ExtensibleFieldHolder.HOLDER_FIELD_NAME] == 'customFields'

    and: 'the complex custom field holder is present'
    def complexHolderMap = instance[ExtensibleFieldHolder.COMPLEX_CUSTOM_FIELD_NAME]
    complexHolderMap instanceof Map
    complexHolderMap[ExtensibleFieldHolder.COMPLEX_THIS_NAME] == instance
  }

  def "verify that field size and field name can be overridden in the annotation in a real domain"() {
    expect: 'the field size constraint is set correctly'
    def property = DomainUtils.instance.getPersistentField(AllFieldsDomain, 'otherCustomFields')
    property.maxLength == 513

    and: "the new field is defined"
    def o = new AllFieldsDomain()
    o.setOtherCustomFields('XYZ')
    o.otherCustomFields == 'XYZ'
  }

  def "verify that the annotation detects when the getFieldValue method already exists"() {
    setup: 'disable printing source'
    CompilerTestUtils.printCompileFailureSource = false

    when: "compile a class with the incorrect usage"
    compileSimpleClass("@ExtensibleFieldHolder String customFields\n void getFieldValue(String x) {}")

    then: "an exception is thrown with the key info"
    Exception ex = thrown()
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['getFieldValue', 'exist'])

    cleanup:
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that the annotation detects when the setFieldValue method already exists"() {
    setup: 'disable printing source'
    CompilerTestUtils.printCompileFailureSource = false

    when: "compile a class with the incorrect usage"
    compileSimpleClass("@ExtensibleFieldHolder String customFields\n void setFieldValue(String x, Object o) {}")

    then: "an exception is thrown with the key info"
    Exception ex = thrown()
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['setFieldValue', 'exist'])

    cleanup:
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that the annotation detects attached to non-field"() {
    setup: 'disable printing source'
    CompilerTestUtils.printCompileFailureSource = false

    when: "compile a class with the annotation attached to a method"
    compileSimpleClass("@ExtensibleFieldHolder void method() {}")

    then: "an exception is thrown with the key info"
    Exception ex = thrown()
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['line', '@ExtensibleFieldHolder'])
  }

  @Rollback
  def "verify that the annotation creates configurable type field accessor methods"() {
    given: 'a clazz with the Configurable Type field'
    def clazz = compileSimpleClass('@ExtensibleFieldHolder String customFields\nFlexType rmaType')

    and: 'a flex type'
    def flexType = DataGenerator.buildFlexType(fieldCount: 2)

    and: 'a domain using the flex type'
    def object = clazz.newInstance()
    object.rmaType = flexType

    when: 'the values are set'
    object.setRmaTypeValue('FIELD1', 'VALUE1')
    object.setRmaTypeValue('FIELD2', 'VALUE2')

    then: 'the values can be read'
    object.getRmaTypeValue('FIELD1') == 'VALUE1'
    object.getRmaTypeValue('FIELD2') == 'VALUE2'
  }


}
