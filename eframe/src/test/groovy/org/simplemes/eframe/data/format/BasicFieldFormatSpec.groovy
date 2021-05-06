/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format

import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils

/**
 * Tests of the abstract base class.  Uses StringFieldFormat for most tests.
 */
class BasicFieldFormatSpec extends BaseSpecification {
  def "verify that the core values are present"() {
    expect: 'the core values contains important fields'
    BasicFieldFormat.coreValues.contains(StringFieldFormat)
    BasicFieldFormat.coreValues.contains(IntegerFieldFormat)
    BasicFieldFormat.coreValues.contains(EncodedTypeFieldFormat)
    // Note, not all core values are tested here.  They are tested in the concrete class's tests
    // (e.g. in StringFieldFormatSpec, etc).

    and: 'the number of core values is expected'
    // If this fails, then adjust the number here and change the two tests below with 'where:' clauses
    // to add the missing format.
    BasicFieldFormat.coreValues.size() == 14
  }

  def "verify that valueOf works"() {
    expect: 'the valueOf finds the right format'
    BasicFieldFormat.valueOf(value.instance.id) == value.instance

    where:
    value                      | _
    StringFieldFormat          | _
    IntegerFieldFormat         | _
    LongFieldFormat            | _
    BigDecimalFieldFormat      | _
    BooleanFieldFormat         | _
    DateOnlyFieldFormat        | _
    DateFieldFormat            | _
    DomainReferenceFieldFormat | _
    EnumFieldFormat            | _
    EncodedTypeFieldFormat     | _
    ChildListFieldFormat       | _
    CustomChildListFieldFormat | _
  }

  def "verify that findByType works"() {
    expect: 'the findByType finds the right format'
    BasicFieldFormat.findByType(type) == result?.instance

    where:
    type       | result
    String     | StringFieldFormat
    Integer    | IntegerFieldFormat
    Long       | LongFieldFormat
    BigDecimal | BigDecimalFieldFormat
    Boolean    | BooleanFieldFormat
    DateOnly   | DateOnlyFieldFormat
    Date       | DateFieldFormat
    null       | null
  }

  def "verify that all IDs in the field formats are unique"() {
    expect: 'the IDs are unique'
    def ids = []
    def formats = BasicFieldFormat.coreValues
    for (format in (formats)) {
      //println "format = $format"
      def id = format.instance.id
      def duplicates = formats.findAll { it.instance.id == id }
      assert !ids.contains(id), "Duplicate ID ($id) found in BasicFieldFormat.coreValues list.  Duplicate formats: $duplicates"
      ids << id
    }
  }

  def "verify that all known instances have a valid toStringLocalized value in the messages.properties"() {
    expect: 'the valueOf finds the right format'
    def s = value.instance.toStringLocalized()
    s == GlobalUtils.lookup("basicFieldFormat.${value.instance.id}.label")
    !s.contains(".label")

    where:
    value                        | _
    StringFieldFormat            | _
    IntegerFieldFormat           | _
    LongFieldFormat              | _
    BigDecimalFieldFormat        | _
    BooleanFieldFormat           | _
    DateOnlyFieldFormat          | _
    DateFieldFormat              | _
    DomainReferenceFieldFormat   | _
    EnumFieldFormat              | _
    EncodedTypeFieldFormat       | _
    ChildListFieldFormat         | _
    CustomChildListFieldFormat   | _
    ConfigurableTypeDomainFormat | _
  }

  def "verify that the getGridEditor returns a standard text editor for this base class"() {
    expect: 'the right editor - uses String format to test'
    StringFieldFormat.instance.getGridEditor() == 'text'
  }

  def "verify that convertTo and convertFromJsonFormat calls encode for base class"() {
    given: 'a format with minimal methods implemented'
    def src = """
      package sample.test
      import org.simplemes.eframe.data.format.BasicFieldFormat
      import org.simplemes.eframe.data.FieldDefinitionInterface
      
      class TestFormat extends BasicFieldFormat {
        String encode(Object value, FieldDefinitionInterface fieldDefinition) {
          return 'xyzzy'
        }
        Object decode(String encodedString, FieldDefinitionInterface fieldDefinition) {
          return new Integer('237')
        }
        String format(Object value, Locale locale, FieldDefinitionInterface fieldDefinition) {}
        Object parse(String value, Locale locale, FieldDefinitionInterface fieldDefinition) {}

      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()

    expect: 'the convert methods call the corresponding encode/decode methods'
    object.convertToJsonFormat(437, null) == 'xyzzy'
    object.convertFromJsonFormat('437', null) == 237
  }

  def "verify that getDisplayValue works"() {
    expect: 'the method works'
    value.instance.getDisplayValue() == result

    where:
    value                      | result
    StringFieldFormat          | 'label.stringFieldFormat'
    IntegerFieldFormat         | 'label.integerFieldFormat'
    LongFieldFormat            | 'label.longFieldFormat'
    BigDecimalFieldFormat      | 'label.bigDecimalFieldFormat'
    BooleanFieldFormat         | 'label.booleanFieldFormat'
    DateOnlyFieldFormat        | 'label.dateOnlyFieldFormat'
    DateFieldFormat            | 'label.dateFieldFormat'
    DomainReferenceFieldFormat | 'label.domainReferenceFieldFormat'
    EnumFieldFormat            | 'label.enumFieldFormat'
    EncodedTypeFieldFormat     | 'label.encodedTypeFieldFormat'
    ChildListFieldFormat       | 'label.childListFieldFormat'
    CustomChildListFieldFormat | 'label.customChildListFieldFormat'
  }

}
