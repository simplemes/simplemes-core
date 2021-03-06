/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget


import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent
import sample.page.SampleParentEditPage
import sample.page.SampleParentShowPage
import spock.lang.IgnoreIf

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
class ComboboxWidgetGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [SampleParent, AllFieldsDomain]

  def "verify that the combobox field can be populated and saved"() {
    given: 'a value to edit'
    def (AllFieldsDomain afd1, AllFieldsDomain afd2) = DataGenerator.generate {
      domain AllFieldsDomain
      count 2
      values name: 'ABC-$i', title: 'abc-$r'
    }
    def (SampleParent sampleParent) = DataGenerator.generate {
      domain SampleParent
      values name: 'XYZ', allFieldsDomain: afd1
    }

    when: 'a page is displayed with the field'
    login()
    to SampleParentEditPage, sampleParent

    then: 'the initial value is filled in'
    allFieldsDomain.input.value() == TypeUtils.toShortString(afd1, true)

    when: 'the value is changed and saved'
    setCombobox(allFieldsDomain, afd2.uuid.toString())
    updateButton.click()
    waitForRecordChange(sampleParent)

    then: 'the record in the database is correct'
    at SampleParentShowPage
    AllFieldsDomain.withTransaction {
      def record = SampleParent.findByName('XYZ')
      assert record.allFieldsDomain == afd2
      true
    }
  }

  def "verify that the combobox field can operated by the mouse"() {
    given: 'a value to edit'
    def (AllFieldsDomain afd1, AllFieldsDomain afd2) = DataGenerator.generate {
      domain AllFieldsDomain
      count 2
      values name: 'ABC-$i', title: 'abc-$r'
    }
    def (SampleParent sampleParent) = DataGenerator.generate {
      domain SampleParent
      values name: 'XYZ', allFieldsDomain: afd1
    }

    when: 'a page is displayed with the field'
    login()
    to SampleParentEditPage, sampleParent

    then: 'the initial value is filled in'
    allFieldsDomain.input.value() == TypeUtils.toShortString(afd1, true)

    when: 'the combobox is opened'
    $('div.webix_el_combo', view_id: "allFieldsDomain").find('span').click()

    and: 'the second item is visible in the popup list'
    //standardGUISleep()
    $('div.webix_list').find('div.webix_list_item', 1).click()

    then: 'the onChange handler for the combobox is called'
    messages.text() == 'allFieldsDomain changed'

    when: 'the record is saved'
    updateButton.click()
    waitForRecordChange(sampleParent)

    then: 'the record in the database is correct'
    at SampleParentShowPage
    AllFieldsDomain.withTransaction {
      def record = SampleParent.findByName('XYZ')
      assert record.allFieldsDomain == afd2
      true
    }
  }

}
