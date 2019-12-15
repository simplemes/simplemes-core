package org.simplemes.eframe.data.annotation

import com.fasterxml.jackson.annotation.JsonProperty
import org.simplemes.eframe.domain.ConstraintUtils
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.UnitTestUtils
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

import java.lang.reflect.Modifier

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ExtensibleFieldsTransformationSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [JSON, SERVER]

  /**
   * Convenience method that compiles a class for testing. The class is called TestClass.
   * This method builds the boiler plate around the class contents passed in.
   * Also sets any class level annotations.
   * @param classContents The groovy source of the class contents.  Everything inside of the class XYZ { . . .}.
   * @param classAnnotations The annotations insert before the class.
   * @return The compiled class.
   */
  static Class compileSimpleClass(String classContents, String classAnnotations) {
    def src = """
      package sample

      import org.simplemes.eframe.data.annotation.*

      $classAnnotations
      class TestClass {
        $classContents
      }
    """

    return CompilerTestUtils.compileSource(src)

  }

  def "test basic annotation with real compiled class"() {
    given: "an instance is made and values are stored in the added field"
    def s = new SampleParent()

    when: "values are stored in the storage"
    s._customFields = '{"xyz": "abc"}'

    then: "the values can be accessed"
    s._customFields == '{"xyz": "abc"}'

    and: 'the field size constraint is set correctly'
    def property = DomainUtils.instance.getPersistentField(SampleParent, ExtensibleFields.DEFAULT_FIELD_NAME)
    ConstraintUtils.instance.getPropertyMaxSize(property) == ExtensibleFields.DEFAULT_MAX_SIZE

    and: 'the accessor methods work'
    s.setFieldValue('xyz', 'pdq')
    s.getFieldValue('xyz') == 'pdq'
  }

  def "test basic annotation creates correct fields and methods"() {
    given: "compile a class with the annotation"
    def clazz = compileSimpleClass("String code", "@ExtensibleFields")

    when: "an instance is made and values are stored in the added field"
    def instance = clazz.newInstance()
    instance[ExtensibleFields.DEFAULT_FIELD_NAME] = "ABC"

    then: "the storage field works."
    instance[ExtensibleFields.DEFAULT_FIELD_NAME] == "ABC"

    and: "the custom field getter has the correct Jackson annotation to rename the field"
    def getter = clazz.getDeclaredMethod('get' + NameUtils.lowercaseFirstLetter(ExtensibleFields.DEFAULT_FIELD_NAME))
    def annotation = getter.annotations.find { it.annotationType() == JsonProperty }
    annotation.value() == '_' + ExtensibleFields.DEFAULT_FIELD_NAME

    and: "the complex field holder was created as a transient"
    def field = clazz.getDeclaredField(ExtensibleFields.COMPLEX_CUSTOM_FIELD_NAME)
    field != null
    field.type == Map
    field.modifiers | Modifier.TRANSIENT

    and: "the complex field holder was initialized correctly"
    def object = clazz.newInstance()
    object."$ExtensibleFields.COMPLEX_CUSTOM_FIELD_NAME" instanceof Map

    and: "the complex field holder is listed in the transients"
    clazz.transients.contains(ExtensibleFields.COMPLEX_CUSTOM_FIELD_NAME)
  }

  def "test field size and field name can be overridden in the annotation in a real domain"() {
    expect: 'the field size constraint is set correctly'
    def property = DomainUtils.instance.getPersistentField(AllFieldsDomain, 'anotherField')
    ConstraintUtils.instance.getPropertyMaxSize(property) == 513

    and: "the new field is defined"
    def o = new AllFieldsDomain()
    o.setAnotherField('XYZ')
    o.anotherField == 'XYZ'
  }

  def "verify that the annotation detects when the storage field already exists"() {
    setup: 'disable printing source'
    CompilerTestUtils.printCompileFailureSource = false

    when: "compile a class with the incorrect usage"
    compileSimpleClass("String _customFields", "@ExtensibleFields")

    then: "an exception is thrown with the key info"
    Exception ex = thrown()
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['_customFields', 'exist'])

    cleanup:
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "test when attached to non-class"() {
    setup: 'disable printing source'
    CompilerTestUtils.printCompileFailureSource = false

    when: "compile a class with the annotation attached to a field"
    compileSimpleClass('', "@ExtensibleFields\nString code")

    then: "an exception is thrown with the key info"
    Exception ex = thrown()
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['line', '@ExtensibleFields'])
  }

  def "test basic annotation with existing transients field"() {
    given: "compile a class with the annotation"
    def clazz = compileSimpleClass("String code\nstatic transients=['code'] ", "@ExtensibleFields")

    //when: "an instance is created"
    //clazz.newInstance()

    expect: "the complex field holder is listed in the transients"
    clazz.transients.contains(ExtensibleFields.COMPLEX_CUSTOM_FIELD_NAME)

    and: "the original field is still in the transients"
    clazz.transients.contains('code')
  }

  //TODO: Find alternative to @Rollback
  def "test basic annotation creates configurable type field accessor methods"() {
    given: 'a clazz with the Configurable Type field'
    def clazz = CompilerTestUtils.compileSimpleClass(annotation: '@ExtensibleFields', contents: 'FlexType rmaType')

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
