package org.simplemes.eframe.client.web

import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
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
 * Tests the base CRUDTable component for Edit tests.  Uses real production pages for the tests.
 */
@IgnoreIf({ !sys['geb.env'] })
class CrudTableEditGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain, FieldExtension, FieldGUIExtension]

  def setup() {
    WebClientLookup.addLocaleFolder("src/client/sample/src/locales")
  }

  /**
   * Opens the Create/Add dialog, optionally makes sure the main panel is displayed.
   */
  def openEditDialog(int row, boolean showMainPanel = true) {
    login()
    to AllFieldsDomainCrudPage

    crudList.editRowButton(row).click()
    waitFor { dialog0.exists }
    if (showMainPanel) {
      mainPanel.click() // Make sure the main panel is displayed
    }
  }

  def "verify that basic edit works"() {
    given: 'a record to edit'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true
    }

    when: 'the edit dialog is displayed'
    openEditDialog(0)

    then: 'the fields are correct'
    name.label == lookupRequired('label.name', currentLocale)
    name.input.value() == 'ABC-001'
    titleField.label == lookup('label.title', currentLocale)
    titleField.input.value() == 'abc-001'

    when: 'the data field is changed and the record saved in the db'
    titleField.input.value('new title')
    saveButton.click()
    waitForRecordChange(allFieldsDomain)

    then: 'the record in the database is correct and the correct show page is displayed'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName('ABC-001')
      assert record.title == 'new title'
      true
    }

    and: 'the crud list is updated to display the record'
    waitFor {
      crudList.cell(0, 1).text() == 'new title'
    }
  }

  def "verify that the edit dialog cancel button works"() {
    given: 'a record to edit'
    DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true
    }

    when: 'the edit dialog is displayed'
    openEditDialog(0)

    and: 'the values are changed and cancelled'
    titleField.input.value('new title')
    cancelButton.click()

    then: 'the record in the database is not changed'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName('ABC-001')
      assert record.title != 'new title'
      true
    }
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

    when: 'the edit dialog is displayed'
    openEditDialog(0)

    and: 'the fields are filled in'
    titleField.input.value('xyz')
    qty.input.value(NumberUtils.formatNumber(237.2, currentLocale))
    count.input.value('437')
    dueDate.input.value(DateUtils.formatDate(dueDateValue, currentLocale))
    dateTime.input.value(DateUtils.formatDate(dateTimeValue, currentLocale))
    enabled.click()

    and: 'the data field is set and the record saved in the db'
    saveButton.click()
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
      true
    }
  }

  def "verify that HTML values are handled correctly - round-trip"() {
    given: 'a record to edit'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', title: '<script>abc</script>'
    }

    when: 'the edit dialog is displayed'
    openEditDialog(0)

    then: 'the field displays the value correctly - non-escaped'
    titleField.input.value() == '<script>abc</script>'

    when: 'the data is saved'
    titleField.input.value('<script>xyz</script>')
    saveButton.click()
    waitForRecordChange(allFieldsDomain)

    then: 'the record in the database is correct and the correct show page is displayed'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName('ABC-001')
      assert record.title == '<script>xyz</script>'
      true
    }
  }


}
