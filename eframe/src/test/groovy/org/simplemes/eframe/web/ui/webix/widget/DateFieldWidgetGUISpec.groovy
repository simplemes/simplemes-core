/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.date.DateUtils
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
class DateFieldWidgetGUISpec extends BaseGUISpecification {
  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain]

  def "verify that the date field can be populated and saved"() {
    given: 'a value to edit'
    def dateTimeValue = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
    def originalDateTime = new Date() - 1
    def (AllFieldsDomain afd) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', dateTime: originalDateTime
    }

    when: 'a page is displayed with the field'
    login()
    to AllFieldsDomainEditPage, afd
    mainPanel.click() // Make sure the main panel is displayed

    then: 'the display value is correct'
    dateTime.input.value() == DateUtils.formatDate(originalDateTime, currentLocale)

    when: 'the data field is changed and the record saved in the db'
    dateTime.input.value(DateUtils.formatDate(dateTimeValue, currentLocale))
    updateButton.click()
    waitForRecordChange(afd)

    then: 'the record in the database is correct and the correct show page is displayed'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName(afd.name)
      assert record.dateTime == dateTimeValue
      true
    }
  }

  def "verify that the date field can be changed by the mouse"() {
    given: 'a value to edit'
    def dateTimeValue = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
    def (AllFieldsDomain afd) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', dateTime: dateTimeValue
    }

    when: 'a page is displayed with the field'
    login()
    to AllFieldsDomainEditPage, afd
    mainPanel.click() // Make sure the main panel is displayed

    and: 'the calendar is opened and the date is changed to jun 21'
    $('div.webix_el_datepicker', view_id: "dateTime").find('span').click()
    standardGUISleep()
    $('div.webix_calendar').find('div.webix_cal_row', 3).find('div', day: '1').click()

    and: 'the record is saved'
    updateButton.click()
    waitForRecordChange(afd)

    then: 'the record in the database is correct and the correct show page is displayed'
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName(afd.name)
      assert UnitTestUtils.compareDates(record.dateTime, dateTimeValue + 7, 60000)
      true
    }
  }

}
