package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.system.DisabledStatus
import org.simplemes.eframe.system.EnabledStatus
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
class ComboboxWidgetEncodedTypeGUISpec extends BaseGUISpecification {

  static dirtyDomains = [AllFieldsDomain]

  @SuppressWarnings("GrEqualsBetweenInconvertibleTypes")
  def "verify that the combobox field can be populated and saved"() {
    given: 'a value to edit'
    def (AllFieldsDomain afd) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC', status: DisabledStatus.instance
    }
    assert afd.status == DisabledStatus.instance

    when: 'a page is displayed with the field'
    login()
    to AllFieldsDomainEditPage, afd
    detailsPanel.click()

    then: 'the initial value is filled in'
    status.input.value() == afd.status.toStringLocalized()

    when: 'the value is changed and saved'
    setCombobox(status, EnabledStatus.instance.id)
    updateButton.click()
    waitForRecordChange(afd)

    then: 'the record in the database is correct'
    at AllFieldsDomainShowPage
    AllFieldsDomain.withTransaction {
      def record = AllFieldsDomain.findByName('ABC')
      assert record.status == EnabledStatus.instance
      true
    }
  }

}
