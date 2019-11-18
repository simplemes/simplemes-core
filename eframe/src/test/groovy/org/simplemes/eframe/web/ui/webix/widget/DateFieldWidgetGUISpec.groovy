package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.UnitTestUtils
import sample.domain.AllFieldsDomain
import sample.page.AllFieldsDomainEditPage
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
class DateFieldWidgetGUISpec extends BaseGUISpecification {
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
    dateTime.input.value() == DateUtils.formatDate(originalDateTime)

    when: 'the data field is changed and the record saved in the db'
    dateTime.input.value(DateUtils.formatDate(dateTimeValue))
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

    and: 'the calendar is opened and the date is changed'
    $('div.webix_el_datepicker', view_id: "dateTime").find('span').click()
    standardGUISleep()
    $('div.webix_calendar').find('div', 'aria-label': '21 June 2010').click()

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
