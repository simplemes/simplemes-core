/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.reports


import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.SimpleStringPreference
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.reports.page.ReportFilterPage
import org.simplemes.eframe.reports.page.ReportPDFPage
import org.simplemes.eframe.reports.page.ReportPage
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.page.DeniedPage
import sample.domain.AllFieldsDomain
import spock.lang.IgnoreIf

/**
 * Tests the report engine/controller in a GUI for full testing.
 */
@IgnoreIf({ !sys['geb.env'] })
@SuppressWarnings("GroovyAssignabilityCheck")
class ReportEngineGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain, User]

  /**
   * The sample report location.
   */
  static final String SAMPLE_REPORT = "reports/sample/SampleReport.jrxml"

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

  def "verify that the basic Sample report works in HTML"() {
    given: 'some test data'
    buildRecords()

    when: 'the report page is displayed'
    login()
    to ReportPage, loc: SAMPLE_REPORT

    then: 'the data is displayed'
    reportTable.text().contains('record_7')

    and: 'the default parameter value is displayed in the filter panel section'
    filterValues.text().contains("${lookup('name.label', currentLocale)}: %")
  }

  def "verify that parameter passed on URL works"() {
    given: 'some test data'
    buildRecords()

    when: 'the report page is displayed'
    login()
    to ReportPage, name: 'record_6', loc: SAMPLE_REPORT

    then: 'only the single matching record is shown'
    reportTable.text().contains('record_6')
    !reportTable.text().contains('record_5')
    !reportTable.text().contains('record_7')

    and: 'the parameter value is displayed in the filter panel section'
    filterValues.text().contains("${lookup('name.label', currentLocale)}: record_6")
  }

  def "verify that the basic Sample report uses the preference setting if a parameter is not passed in on the URL"() {
    given: 'some test data'
    buildRecords()

    and: 'the user preference values are saved'
    UserPreference.withTransaction {
      PreferenceHolder preference = PreferenceHolder.find {
        page ReportHelper.REPORT_PAGE
        user 'admin'
        element "reports/sample/SampleReport.jrxml"   // Must use string literal here
      }
      preference.setPreference(new SimpleStringPreference(key: 'name', value: 'record_6'))
      preference.save()
    }

    when: 'the report page is displayed'
    login()
    to ReportPage, loc: SAMPLE_REPORT

    then: 'the user preference is used'
    reportTable.text().contains('record_6')

    and: 'the correct value is used in the display header'
    filterValues.text().contains("${lookup('name.label', currentLocale)}: record_6")
  }

  def "verify that the basic Sample report ignores the preference setting if a parameter is passed in on the URL"() {
    given: 'some test data'
    buildRecords()

    and: 'the user preference values are saved'
    UserPreference.withTransaction {
      PreferenceHolder preference = PreferenceHolder.find {
        page ReportHelper.REPORT_PAGE
        user 'admin'
        element "reports/sample/SampleReport.jrxml"   // Must use string literal here
      }
      preference.setPreference(new SimpleStringPreference(key: 'name', value: 'record_6'))
      preference.save()
    }

    when: 'the report page is displayed'
    login()
    to ReportPage, loc: SAMPLE_REPORT, name: 'record_7'

    then: 'the URL parameter is used'
    reportTable.text().contains('record_7')
    !reportTable.text().contains('record_6')

    and: 'the default parameter value is displayed in the filter panel section'
    filterValues.text().contains("${lookup('name.label', currentLocale)}: record_7")
  }

  // The title on PDF pages in Chrome is not testable.
  @IgnoreIf({ System.getProperty("geb.env")?.contains("chrome") })
  def "verify that the basic Sample report works in PDF"() {
    given: 'some test data'
    buildRecords()

    when: 'the report page is displayed'
    login()
    to ReportPDFPage, loc: SAMPLE_REPORT, format: 'pdf'

    then: 'the data is displayed as a PDF'
    title.equalsIgnoreCase('report')
  }

  def "verify that roles are enforced with reports"() {
    given: 'a user without admin role'
    DataGenerator.buildTestUser('none')

    expect: 'the report page re-directs to the denied page'
    login('none', 'none')
    drive {
      go "${baseUrl}/report?loc=${SAMPLE_REPORT}"
      at DeniedPage
    }

    and: 'the missing role is displayed'
    $('div.error-message').text().contains('ADMIN')

    and: 'the report location is displayed'
    $('div.error-message').text().contains(SAMPLE_REPORT)
  }

  def "verify that the change filter link works"() {
    given: 'some test data'
    buildRecords()

    when: 'the report page is displayed'
    login()
    to ReportPage, name: 'record_6', loc: SAMPLE_REPORT

    and: 'the change filter link is clicked'
    filterLink.click()

    then: 'we are at the filter page'
    at ReportFilterPage
    name.input.value() == 'record_6'
  }

  // The title on PDF pages in Chrome is not testable.
  @IgnoreIf({ System.getProperty("geb.env")?.contains("chrome") })
  def "verify that the PDF link works"() {
    given: 'some test data'
    buildRecords()

    when: 'the report page is displayed'
    login()
    to ReportPage, loc: SAMPLE_REPORT

    and: 'display the PDF report'
    pdfLink.click()

    then: 'we are at the filter page'
    at ReportPDFPage
  }

  def "verify that the pager works"() {
    given: 'some test data'
    buildRecords(100)

    when: 'the report page is displayed'
    login()
    to ReportPage, loc: SAMPLE_REPORT

    and: 'the first page is shown'
    reportTable.text().contains('record_10')
    !reportTable.text().contains('record_50')

    and: 'display the PDF report'
    nextPageLink.click()

    then: 'we are still on the report page'
    at ReportPage

    and: 'the second page is shown'
    !reportTable.text().contains('record_10')
    reportTable.text().contains('record_50')
  }


}
