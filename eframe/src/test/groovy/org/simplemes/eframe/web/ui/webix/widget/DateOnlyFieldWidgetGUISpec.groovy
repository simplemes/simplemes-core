/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.date.ISODate
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.UnitTestUtils
import sample.domain.AllFieldsDomain
import sample.page.AllFieldsDomainEditPage
import spock.lang.IgnoreIf

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
class DateOnlyFieldWidgetGUISpec extends BaseGUISpecification {
  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain]

  // TODO: Convert to edit page tests to verify the initial value and test popup actions.
  def "verify that the date field can be populated and saved"() {
    given: 'a value to edit'
    def dateOnlyValue = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def originalDate = new DateOnly()
    def (AllFieldsDomain afd) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', dueDate: originalDate
    }

    when: 'a page is displayed with the field'
    login()
    to AllFieldsDomainEditPage, afd
    mainPanel.click() // Make sure the main panel is displayed

    then: 'the display value is correct'
    dueDate.input.value() == DateUtils.formatDate(originalDate)

    when: 'the data field is changed and the record saved in the db'
    dueDate.input.value(DateUtils.formatDate(dateOnlyValue))
    updateButton.click()
    waitForRecordChange(afd)

    then: 'the record in the database is correct and the correct show page is displayed'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName(afd.name)
      assert record.dueDate == dateOnlyValue
      true
    }
  }

  def "verify that the dateOnly field can be changed by the mouse"() {
    given: 'a value to edit'
    def dateOnlyValue = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def (AllFieldsDomain afd) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', dueDate: dateOnlyValue
    }

    when: 'a page is displayed with the field'
    login()
    to AllFieldsDomainEditPage, afd
    mainPanel.click() // Make sure the main panel is displayed

    and: 'the calendar is opened and the date is changed'
    $('div.webix_el_datepicker', view_id: "dueDate").find('span').click()
    standardGUISleep()
    $('div.webix_calendar').find('div', 'aria-label': '21 June 2010').click()

    and: 'the record is saved'
    updateButton.click()
    waitForRecordChange(afd)

    then: 'the record in the database is correct and the correct show page is displayed'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName(afd.name)
      assert record.dueDate == ISODate.parseDateOnly('2010-06-21')
      true
    }
  }


}
