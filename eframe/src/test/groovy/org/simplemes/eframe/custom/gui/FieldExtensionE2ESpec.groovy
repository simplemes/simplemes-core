package org.simplemes.eframe.custom.gui

import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.system.DisabledStatus
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.page.BooleanFieldModule
import org.simplemes.eframe.test.page.ComboboxModule
import org.simplemes.eframe.test.page.DateFieldModule
import org.simplemes.eframe.test.page.ReadOnlyFieldModule
import org.simplemes.eframe.test.page.TextFieldModule
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent
import sample.page.SampleParentCreatePage
import sample.page.SampleParentEditPage
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * End to End tests of custom fields added to definition GUIs.
 */
@IgnoreIf({ !sys['geb.env'] })
class FieldExtensionE2ESpec extends BaseDefinitionEditorSpecification {


  @SuppressWarnings("unused")
  static dirtyDomains = [FieldGUIExtension, FieldExtension, SampleParent, AllFieldsDomain]

  /**
   * Convenience method to set a field value in the GUI for a given custom field.
   * The exact method used to set the value depends on the value type.
   * @param fieldName The field to set.
   * @param value The field value.
   */
  void setFieldValue(String fieldName, Object value) {
    switch (value.getClass()) {
      case Boolean:
      case boolean:
        def customField = $("body").module(new BooleanFieldModule(field: fieldName))
        customField.setValue(value)
        break
      case Date:
      case DateOnly:
        def customField = $("body").module(new DateFieldModule(field: fieldName))
        customField.input.value(DateUtils.formatDate((Date) value))
        break
      case ReportTimeIntervalEnum:
        def customField = $("body").module(new ComboboxModule(field: fieldName))
        setCombobox(customField, value.toString())
        break
      case BasicStatus:
        def customField = $("body").module(new ComboboxModule(field: fieldName))
        setCombobox(customField, (String) value.id)
        break
      case AllFieldsDomain:
        def customField = $("body").module(new ComboboxModule(field: fieldName))
        setCombobox(customField, value.id.toString())
        break
      default:
        def customField = $("body").module(new TextFieldModule(field: fieldName))
        customField.input.value(value.toString())
    }
  }

  /**
   * Convenience method to build a record for domain reference tests.
   * @return
   */
  AllFieldsDomain buildAllFieldsDomain() {
    def allFieldsDomain = null
    AllFieldsDomain.withTransaction {
      allFieldsDomain = new AllFieldsDomain(name: 'ABC').save()
    }
    return allFieldsDomain
  }

  def "verify that the edit page can update a custom field value"() {
    given: 'a custom field for the domain'
    buildCustomField(fieldName: 'custom1', domainClass: SampleParent)

    and: 'a domain record is available to edit'
    def sampleParent = null
    SampleParent.withTransaction {
      sampleParent = new SampleParent(name: 'abc')
      sampleParent.setFieldValue('custom1', 'c1')
      sampleParent.save()
    }

    when: 'the edit page is shown'
    login()
    to SampleParentEditPage, sampleParent

    then: 'the custom field is correct'
    def customField = $("body").module(new TextFieldModule(field: 'custom1'))
    customField.input.value() == 'c1'

    when: 'the custom field value is changed'
    customField.input.value('c2')

    and: 'the record is saved'
    updateButton.click()
    waitForRecordChange(sampleParent)

    then: 'the record is updated'
    SampleParent.withTransaction {
      def sp = SampleParent.findByName('abc')
      assert sp.getFieldValue('custom1') == 'c2'
      true
    }
  }

  def "verify that the create page can set a custom field value"() {
    given: 'a custom field for the domain'
    buildCustomField(fieldName: 'custom1', domainClass: SampleParent)

    when: 'the edit page is shown'
    login()
    to SampleParentCreatePage

    and: 'the custom field value is set'
    def customField = $("body").module(new TextFieldModule(field: 'custom1'))
    customField.input.value('c2')
    name.input.value('ABC')

    and: 'the record is saved'
    createButton.click()
    waitForNonZeroRecordCount(SampleParent)

    then: 'the record is updated'
    SampleParent.withTransaction {
      def sp = SampleParent.findByName('ABC')
      assert sp.getFieldValue('custom1') == 'c2'
      true
    }
  }


  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that the create page can set a custom field using all supported field types"() {
    given: 'a custom field for the domain'
    buildCustomField(fieldName: 'custom1', domainClass: SampleParent,
                     fieldFormat: format.instance, valueClassName: valueClass?.name)
    if (value == AllFieldsDomain.name) {
      // Special case to initialize the value to a domain class
      value = buildAllFieldsDomain()
    }

    when: 'the edit page is shown'
    login()
    to SampleParentCreatePage

    and: 'the custom field value is set'
    setFieldValue('custom1', value)

    and: 'the record is saved'
    name.input.value('ABC')
    createButton.click()
    waitForNonZeroRecordCount(SampleParent)

    then: 'the record is updated'
    def sampleParent = null
    SampleParent.withTransaction {
      sampleParent = SampleParent.findByName('ABC')
      assert sampleParent.getFieldValue('custom1') == value
      true
    }

    and: 'the show page is displayed'
    def nameShowField = $("body").module(new ReadOnlyFieldModule(field: 'name'))
    nameShowField.value == 'ABC'
    //sleep(1000)


    where:
    format                     | valueClass             | value
    StringFieldFormat          | null                   | 'abc'
    IntegerFieldFormat         | null                   | 437
    LongFieldFormat            | null                   | 1337L
    BigDecimalFieldFormat      | null                   | 12.2
    BooleanFieldFormat         | null                   | true
    DateOnlyFieldFormat        | null                   | new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    DateFieldFormat            | null                   | new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
    EnumFieldFormat            | ReportTimeIntervalEnum | ReportTimeIntervalEnum.YESTERDAY
    EncodedTypeFieldFormat     | BasicStatus            | DisabledStatus.instance
    DomainReferenceFieldFormat | AllFieldsDomain        | AllFieldsDomain.name
  }


}
