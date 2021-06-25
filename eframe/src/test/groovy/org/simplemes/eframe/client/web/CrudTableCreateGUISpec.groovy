package org.simplemes.eframe.client.web


import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.custom.gui.FieldInsertAdjustment
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.WebClientLookup
import sample.domain.AllFieldsDomain
import sample.page.AllFieldsDomainCrudPage
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the base CRUDTable component for Create tests.  Uses real production pages for the tests.
 */
@IgnoreIf({ !sys['geb.env'] })
class CrudTableCreateGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain, FieldExtension, FieldGUIExtension]

  def setup() {
    WebClientLookup.addLocaleFolder("src/client/sample/src/locales")
  }

  /**
   * Opens the Create/Add dialog, optionally makes sure the main panel is displayed.
   */
  def openCreateDialog(boolean showMainPanel = true) {
    login()
    to AllFieldsDomainCrudPage

    crudList.addRecordButton.click()
    waitFor { dialog0.exists }
    if (showMainPanel) {
      mainPanel.click() // Make sure the main panel is displayed
    }
  }

  def "verify that basic create works"() {
    when: 'the create dialog is displayed'
    openCreateDialog(false)

    and: 'a value is entered in the key field'
    sendKey('ABC')

    then: 'the key field is correct'
    name.input.value() == 'ABC'
    name.label == lookupRequired('label.name')

    when: 'the data field is set and the record saved in the db'
    mainPanel.click() // Make sure the main panel is displayed
    titleField.label == lookup('title.label')
    titleField.input.value('abc')
    saveButton.click()

    then: 'the correct message is displayed'
    waitFor {
      messages
    }

    then: 'the correct message is shown'
    messages.text().contains('ABC')

    and: 'the message is displayed with the right class'
    messages.isSuccess()

    then: 'the record in the database is correct and the correct show page is displayed'
    waitFor {
      nonZeroRecordCount(AllFieldsDomain)
    }
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName('ABC')
      assert record.name == 'ABC'
      assert record.title == 'abc'
      true
    }

    and: 'the crud list is updated to display the record'
    waitFor {
      crudList.cell(0, 0).text() == 'ABC'
    }
    crudList.cell(0, 1).text() == 'abc'
  }

  def "verify that create with tabbed panels works"() {
    when: 'the create dialog is displayed'
    openCreateDialog(false)

    and: 'the key field is correct'
    name.input.value('ABC')

    and: 'the data field is set and the record saved in the db'
    detailsPanel.click() // Make sure the details panel is displayed
    notes.label == lookup('label.title')
    notes.input.value('xyz')
    saveButton.click()

    then: 'the record in the database is correct and the correct show page is displayed'
    waitFor {
      nonZeroRecordCount(AllFieldsDomain)
    }
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName('ABC')
      assert record.name == 'ABC'
      assert record.notes == 'xyz'
      true
    }
  }

  def "verify that basic create works with the supported field type"() {
    given: 'some values for the record'
    def dueDateValue = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def dateTimeValue = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)

    when: 'the create dialog is displayed'
    openCreateDialog()

    then: 'the field labels are correct'
    qty.label == lookup('label.qty')
    count.label == lookup('label.count')
    enabled.label == lookup('label.enabled')
    dueDate.label == lookup('label.dueDate')
    dateTime.label == lookup('label.dateTime')

    when: 'the fields are filled in'
    name.input.value('ABC')
    titleField.input.value('abc')
    qty.input.value(NumberUtils.formatNumber(237.2, currentLocale))
    count.input.value('437')
    dueDate.input.value(DateUtils.formatDate(dueDateValue, currentLocale))
    dateTime.input.value(DateUtils.formatDate(dateTimeValue, currentLocale))
    enabled.click()

    and: 'the data field is set and the record saved in the db'
    saveButton.click()
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
      true
    }
  }

  def "verify that save validation errors are handled correctly"() {
    when: 'the create dialog is displayed'
    openCreateDialog()

    then: 'the key field has focus and can accept input'
    name.input.value('ABC')

    when: 'the data field is set and the record saved in the db'
    count.input.value('-237')
    saveButton.click()
    waitFor {
      messages
    }

    then: 'the correct message is shown'
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), ['count', '-237'])

    and: 'the message is displayed with the right class'
    messages.isError()
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

    when: 'the create dialog is displayed'
    openCreateDialog()

    then: 'the page has the custom field'
    getFieldLabel('custom1') == 'custom1'
    getInputField('custom1').value(NumberUtils.formatNumber(237.2, currentLocale))

    when: 'the record is saved'
    name.input.value('ABC')
    saveButton.click()
    waitFor {
      nonZeroRecordCount(AllFieldsDomain)
    }

    then: 'the record is correct'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName('ABC')
      assert record.getFieldValue('custom1') == 237.2
      true
    }

    and: 'the crud list is updated to display the record'
    waitFor {
      crudList.cell(0, 0).text() == 'ABC'
    }
  }

}
