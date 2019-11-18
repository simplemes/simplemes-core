package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.custom.gui.FieldInsertAdjustment
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.UnitTestUtils
import sample.domain.AllFieldsDomain
import sample.page.AllFieldsDomainEditPage
import sample.page.AllFieldsDomainListPage
import sample.page.AllFieldsDomainShowPage
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
class EditMarkerGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain, FieldExtension, FieldGUIExtension]

  def "verify that basic edit works"() {
    given: 'a record to edit'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true
    }


    when: 'the edit page is displayed'
    login()
    to AllFieldsDomainEditPage, allFieldsDomain
    mainPanel.click() // Make sure the main panel is displayed

    then: 'the fields are correct'
    name.label == lookupRequired('name.label')
    name.input.value() == 'ABC-001'
    titleField.label == lookup('title.label')
    titleField.input.value() == 'abc-001'

    when: 'the data field is changed and the record saved in the db'
    titleField.input.value('new title')
    updateButton.click()
    waitForRecordChange(allFieldsDomain)

    then: 'the record in the database is correct and the correct show page is displayed'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName('ABC-001')
      assert record.title == 'new title'
      assert driver.currentUrl.endsWith("/allFieldsDomain/show/${record.id}")
      true
    }

    and: 'the show page is displayed for the record'
    at AllFieldsDomainShowPage
  }

  def "verify that edit puts initial focus in key field"() {
    given: 'a record to edit'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true
    }

    when: 'the edit page is displayed'
    login()
    to AllFieldsDomainEditPage, allFieldsDomain

    and: 'some keys are pressed'
    sendKey('XYZ')

    then: 'the key field has the sent value'
    name.input.value().contains('XYZ')
  }

  def "verify that basic edit works with the supported field type"() {
    given: 'a record to edit'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true
    }

    and: 'some values for the record'
    def dueDateValue = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def dateTimeValue = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)

    when: 'the edit page is displayed'
    login()
    to AllFieldsDomainEditPage, allFieldsDomain
    mainPanel.click() // Make sure the main panel is displayed

    then: 'the field labels are correct'
    qty.label == lookup('qty.label')
    count.label == lookup('count.label')
    enabled.label == lookup('enabled.label')
    dueDate.label == lookup('dueDate.label')
    dateTime.label == lookup('dateTime.label')

    when: 'the fields are filled in'
    titleField.input.value('xyz')
    qty.input.value(NumberUtils.formatNumber(237.2, currentLocale))
    count.input.value('437')
    dueDate.input.value(DateUtils.formatDate(dueDateValue, currentLocale))
    dateTime.input.value(DateUtils.formatDate(dateTimeValue, currentLocale))
    enabled.input.click()

    and: 'the data field is set and the record saved in the db'
    updateBottomButton.click()
    waitForRecordChange(allFieldsDomain)

    then: 'the record in the database is correct and the correct show page is displayed'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName('ABC-001')
      assert record.title == 'xyz'
      assert record.qty == 237.2
      assert record.count == 437
      assert !record.enabled
      assert record.dueDate == dueDateValue
      assert record.dateTime == dateTimeValue

      assert driver.currentUrl.endsWith("/allFieldsDomain/show/${record.id}")
      true
    }

    and: 'the show page is displayed for the record'
    at AllFieldsDomainShowPage
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that save validation errors are handled correctly"() {
    given: 'a record to edit'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true
    }

    when: 'the edit page is displayed'
    login()
    to AllFieldsDomainEditPage, allFieldsDomain
    mainPanel.click() // Make sure the main panel is displayed

    and: 'the data field is set and the record saved in the db'
    count.input.value('-237')
    updateButton.click()
    waitFor {
      messages
    }

    then: 'the correct message is shown'
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), ['count', '-237'])

    and: 'the message is displayed with the right class'
    messages.find('div').classes().contains('error-message')

    and: 'the bad field has the focus'
    sendKey('888')
    count.input.value().contains('888')

    and: 'the field is highlighted as an error'
    count.invalid == true
  }

  def "verify that tab selection is retained when page is re-displayed"() {
    given: 'a record to edit'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true
    }

    when: 'the page is displayed'
    login()
    to AllFieldsDomainEditPage, allFieldsDomain

    and: 'the details panel is displayed'
    detailsPanel.click()

    and: 'the page is redisplayed'
    to AllFieldsDomainEditPage, allFieldsDomain

    then: 'the details tab fields are visible'
    transientField.label == lookup('transientField.label')
  }

  def "verify that list toolbar button works"() {
    given: 'a record to edit'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true
    }

    when: 'the page is displayed'
    login()
    to AllFieldsDomainEditPage, allFieldsDomain

    and: 'the list toolbar button is clicked'
    listButtonOnEdit.click()

    then: 'the list page is shown'
    at AllFieldsDomainListPage
  }

  def "verify that HTML values are handled correctly - round-trip"() {
    given: 'a record to edit'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', title: '<script>abc</script>'
    }

    when: 'the page is displayed'
    login()
    to AllFieldsDomainEditPage, allFieldsDomain
    mainPanel.click() // Make sure the main panel is displayed

    then: 'the field displays the value correctly - non-escaped'
    titleField.input.value() == '<script>abc</script>'

    when: 'the data is saved'
    titleField.input.value('<script>xyz</script>')
    updateButton.click()
    waitForRecordChange(allFieldsDomain)

    then: 'the record in the database is correct and the correct show page is displayed'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName('ABC-001')
      assert record.title == '<script>xyz</script>'
      true
    }
  }

  def "verify that basic page display works - custom field added"() {
    given: 'a custom field'
    FieldExtension.withTransaction {
      new FieldExtension(fieldName: 'custom1', domainClassName: AllFieldsDomain.name,
                         fieldFormat: BigDecimalFieldFormat.instance).save()
      def fg = new FieldGUIExtension(domainName: AllFieldsDomain.name)
      fg.adjustments = [new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'title')]
      fg.save()
    }

    and: 'a record to edit'
    def allFieldsDomain = null
    AllFieldsDomain.withTransaction {
      allFieldsDomain = new AllFieldsDomain(name: 'ABC1')
      allFieldsDomain.setFieldValue('custom1', 1.2)
      allFieldsDomain.save()
    }

    when: 'the page is displayed'
    login()
    to AllFieldsDomainEditPage, allFieldsDomain
    mainPanel.click() // Make sure the main panel is displayed

    then: 'the page has the custom field'
    getFieldLabel('custom1') == 'custom1'
    getInputField('custom1').value() == NumberUtils.formatNumber(1.2, currentLocale)

    when: 'the field is changed'
    getInputField('custom1').value('237')

    and: 'the record is saved'
    updateButton.click()
    waitForRecordChange(allFieldsDomain)

    then: 'the record is updated'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName((String) allFieldsDomain.name)
      assert record.getFieldValue('custom1') == 237
      true
    }

    and: 'the show page is displayed for the record'
    at AllFieldsDomainShowPage
  }


  // test enum and simple link, link list, child list fields on save


}
