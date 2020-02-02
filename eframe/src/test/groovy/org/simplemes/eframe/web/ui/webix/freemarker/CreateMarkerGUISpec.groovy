/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.custom.gui.FieldInsertAdjustment
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.UnitTestUtils
import sample.domain.AllFieldsDomain
import sample.page.AllFieldsDomainCreatePage
import sample.page.AllFieldsDomainListPage
import sample.page.AllFieldsDomainShowPage
import spock.lang.IgnoreIf

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
class CreateMarkerGUISpec extends BaseGUISpecification {
  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain, FieldExtension, FieldGUIExtension]

  def "verify that basic create works"() {
    when: 'the create page is displayed'
    // TODO: Replace with correct waitFor....
    sleep(1000)
    login()
    sleep(1000)
    to AllFieldsDomainCreatePage
    sleep(1000)

    then: 'the key field has focus and can accept input'
    sendKey('ABC')
    mainPanel.click() // Make sure the main panel is displayed

    and: 'the key field is correct'
    name.label == lookupRequired('name.label')
    name.input.value() == 'ABC'

    when: 'the data field is set and the record saved in the db'
    titleField.label == lookup('title.label')
    titleField.input.value('abc')
    createButton.click()
    waitFor {
      nonZeroRecordCount(AllFieldsDomain)
    }

    then: 'the record in the database is correct and the correct show page is displayed'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName('ABC')
      assert record.name == 'ABC'
      assert record.title == 'abc'
      assert driver.currentUrl.endsWith("/allFieldsDomain/show/${record.id}")
      true
    }

    and: 'the show page is displayed for the record'
    at AllFieldsDomainShowPage
  }

  def "verify that basic create works with the supported field type"() {
    given: 'some values for the record'
    def dueDateValue = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def dateTimeValue = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)

    when: 'the create page is displayed'
    login()
    to AllFieldsDomainCreatePage
    mainPanel.click() // Make sure the main panel is displayed

    then: 'the field labels are correct'
    qty.label == lookup('qty.label')
    count.label == lookup('count.label')
    enabled.label == lookup('enabled.label')

    when: 'the fields are filled in'
    name.input.value('ABC')
    titleField.input.value('abc')
    qty.input.value(NumberUtils.formatNumber(237.2, currentLocale))
    count.input.value('437')
    dueDate.input.value(DateUtils.formatDate(dueDateValue, currentLocale))
    dateTime.input.value(DateUtils.formatDate(dateTimeValue, currentLocale))
    enabled.input.click()

    and: 'the data field is set and the record saved in the db'
    createBottomButton.click()
    waitFor {
      nonZeroRecordCount(AllFieldsDomain)
    }

    then: 'the record in the database is correct and the correct show page is displayed'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName('ABC')
      assert record.name == 'ABC'
      assert record.title == 'abc'
      assert record.qty == 237.2
      assert record.count == 437
      assert record.enabled
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
    when: 'the create page is displayed'
    login()
    to AllFieldsDomainCreatePage
    mainPanel.click() // Make sure the main panel is displayed

    then: 'the key field has focus and can accept input'
    name.input.value('ABC')

    when: 'the data field is set and the record saved in the db'
    count.input.value('-237')
    createButton.click()
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
    when: 'the page is displayed'
    login()
    to AllFieldsDomainCreatePage

    and: 'the details panel is displayed'
    detailsPanel.click()

    and: 'the page is redisplayed'
    to AllFieldsDomainCreatePage

    then: 'the details tab fields are visible'
    transientField.label == lookup('transientField.label')
  }

  def "verify that list toolbar button works"() {
    when: 'the page is displayed'
    login()
    to AllFieldsDomainCreatePage

    and: 'the list toolbar button is clicked'
    listButtonOnCreate.click()

    then: 'the list page is shown'
    at AllFieldsDomainListPage
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

    when: 'the page is displayed'
    login()
    to AllFieldsDomainCreatePage
    mainPanel.click() // Make sure the main panel is displayed

    then: 'the page has the custom field'
    getFieldLabel('custom1') == 'custom1'
    getInputField('custom1').value(NumberUtils.formatNumber(237.2, currentLocale))

    when: 'the record is saved'
    name.input.value('ABC')
    createButton.click()
    waitFor {
      nonZeroRecordCount(AllFieldsDomain)
    }

    then: 'the record is correct'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName('ABC')
      assert record.getFieldValue('custom1') == 237.2
      true
    }

    and: 'the show page is displayed for the record'
    at AllFieldsDomainShowPage
  }


  // GUI Tests
  // CRUDGUITester Test all fields tabbed/non-tabbed
  // test enum and simple link, link list, child list fields on save


}
