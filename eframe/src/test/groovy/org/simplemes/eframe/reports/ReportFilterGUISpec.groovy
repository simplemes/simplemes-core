/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.reports

import org.openqa.selenium.Keys
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.date.ISODate
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.reports.page.ReportFilterPage
import org.simplemes.eframe.reports.page.ReportPage
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import sample.domain.AllFieldsDomain
import spock.lang.IgnoreIf

/**
 * Tests the report engine/controller filter page in a GUI for full testing.
 */
@IgnoreIf({ !sys['geb.env'] })
@SuppressWarnings("GroovyAssignabilityCheck")
class ReportFilterGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain]

  /**
   * The sample report location.
   */
  public static final String SAMPLE_REPORT = "reports/sample/SampleReport.jrxml"

  /**
   * Build some test records.
   * @param count The number of records (default: 10)
   */
  private void buildRecords(Integer count = 10) {
    def now = new DateOnly()
    AllFieldsDomain.withTransaction {
      for (i in 1..count) {
        def name = "record_$i"
        new AllFieldsDomain(name: name, dueDate: now, qty: 12.2 + i, count: 20 + 1).save()
      }
    }
  }

  def "verify basic filter page operation - re-displays the report when done"() {
    given: 'some test data'
    buildRecords()

    and: 'the parameter inputs'
    def start = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
    def end = new Date()

    when: 'the report page is displayed'
    login()
    to ReportFilterPage, loc: SAMPLE_REPORT,
       name: 'record_6', reportTimeInterval: ReportTimeIntervalEnum.CUSTOM_RANGE.toString(),
       startDateTime: ISODate.format(start), endDateTime: ISODate.format(end)

    then: 'the filter fields are shown correctly'
    name.input.value() == 'record_6'
    reportTimeInterval.input.value() == ReportTimeIntervalEnum.CUSTOM_RANGE.toStringLocalized()

    and: 'the date is correct'
    startDateTime.input.value() == DateUtils.formatDate(start, currentLocale)

    when: 'new filter values are entered'
    name.input.value('record_5')
    def newStart = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS) + 1
    startDateTime.input.value(DateUtils.formatDate(newStart, currentLocale))

    and: 'the report is displayed'
    updateButton.click()

    then: 'the report is shown'
    at ReportPage

    and: 'the filter parameters are used'
    reportTable.text().contains('record_5')
    !reportTable.text().contains('record_6')
    def dateText = DateUtils.formatDate(newStart, currentLocale)
    reportHeader.text().contains("${GlobalUtils.lookup('startDateTime.label')}: $dateText")

    and: 'the user preference values are saved'
    UserPreference.withTransaction {
      PreferenceHolder preference = PreferenceHolder.find {
        page ReportHelper.REPORT_PAGE
        user 'admin'
        element ReportFilterGUISpec.SAMPLE_REPORT
      }
      assert preference['name'].value == 'record_5'
      assert preference['reportTimeInterval'].value == 'CUSTOM_RANGE'
      return true
    }
  }

  def "verify changing to custom date range enables the date fields for input"() {
    when: 'the report page is displayed'
    login()
    to ReportFilterPage, loc: SAMPLE_REPORT,
       reportTimeInterval: ReportTimeIntervalEnum.TODAY.toString()

    then: 'the filter fields are shown correctly'
    waitFor {
      startDateTime.input.@disabled
    }
    startDateTime.input.@disabled

    when: 'the report interval is changed to custom'
    reportTimeInterval.input.value(ReportTimeIntervalEnum.CUSTOM_RANGE.toStringLocalized())
    waitFor {  // Waits for the input to finish before TAB out.
      reportTimeInterval.input.focused
    }
    sendKey(Keys.TAB)

    then: 'we wait for the field to change to editable'
    waitFor {
      !startDateTime.input.@disabled
    }

  }

}
