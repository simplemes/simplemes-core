package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.reports.ReportTimeIntervalEnum
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import sample.domain.AllFieldsDomain
import sample.page.AllFieldsDomainEditPage
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
class ComboboxWidgetEnumGUISpec extends BaseGUISpecification {

  static dirtyDomains = [AllFieldsDomain]

  def "verify that the combobox field can be populated and saved"() {
    given: 'a value to edit'
    def (AllFieldsDomain afd) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC', reportTimeInterval: ReportTimeIntervalEnum.LAST_24_HOURS
    }

    when: 'a page is displayed with the field'
    login()
    to AllFieldsDomainEditPage, afd
    detailsPanel.click()

    then: 'the initial value is filled in'
    reportTimeInterval.input.value() == afd.reportTimeInterval.toStringLocalized()

    when: 'the value is changed and saved'
    setCombobox(reportTimeInterval, ReportTimeIntervalEnum.LAST_7_DAYS.toString())
    updateButton.click()
    waitForRecordChange(afd)

    then: 'the record in the database is correct'
    at AllFieldsDomainShowPage
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName('ABC')
      assert record.reportTimeInterval == ReportTimeIntervalEnum.LAST_7_DAYS
      true
    }
  }

}
