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
class MultiComboboxWidgetGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [SampleParent, AllFieldsDomain]

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that the multi-select combobox field can be populated and saved"() {
    given: 'a value to edit'
    List<AllFieldsDomain> listOfDomains = DataGenerator.generate {
      domain AllFieldsDomain
      count 10
    }
    def sampleParent = null
    def list = [listOfDomains[0], listOfDomains[1]]
    SampleParent.withTransaction {
      sampleParent = new SampleParent(name: 'ABC', allFieldsDomains: list).save()
    }

    when: 'a page is displayed with the field'
    login()
    to SampleParentEditPage, sampleParent

    then: 'the initial value is filled in'
    def afd1 = TypeUtils.toShortString(listOfDomains[0])
    def afd2 = TypeUtils.toShortString(listOfDomains[1])
    allFieldsDomains.input.value() == "${afd1},${afd2}"

    when: 'the value is changed and saved'
    setMultiCombobox(allFieldsDomains, [listOfDomains[0], listOfDomains[9], listOfDomains[1]])
    updateButton.click()
    waitForRecordChange(sampleParent)

    then: 'the record in the database is correct'
    at SampleParentShowPage
    SampleParent.withTransaction {
      def record = SampleParent.findByName('ABC')
      assert record.allFieldsDomains.size() == 3
      assert record.allFieldsDomains[0] == listOfDomains[0]
      assert record.allFieldsDomains[1] == listOfDomains[9]
      assert record.allFieldsDomains[2] == listOfDomains[1]
      true
    }
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that the combobox field can operated by the mouse"() {
    given: 'a value to edit'
    def (AllFieldsDomain afd1, AllFieldsDomain afd2) = DataGenerator.generate {
      domain AllFieldsDomain
      count 2
      values name: 'ABC-$i', title: 'abc-$r'
    }
    def (SampleParent sampleParent) = DataGenerator.generate {
      domain SampleParent
      values name: 'XYZ', allFieldsDomains: [afd1]
    }

    when: 'a page is displayed with the field'
    login()
    to SampleParentEditPage, sampleParent

    then: 'the initial value is filled in'
    allFieldsDomains.input.value() == TypeUtils.toShortString(afd1)

    when: 'the combobox is opened'
    $('div.webix_el_combo', view_id: "allFieldsDomains").find('span').click()

    and: 'the second item in the popup list is clicked'
    // NOTE: This relies on the ordering of the combo-boxes.  The first one is a single-select.
    //       The one we use here is the multi-select.
    $('div.webix_list', 1).find('div.webix_list_item', 1).click()

    and: 'the record is saved'
    updateButton.click()
    waitForRecordChange(sampleParent)

    then: 'the record in the database is correct'
    at SampleParentShowPage
    AllFieldsDomain.withTransaction {
      def record = SampleParent.findByName('XYZ')
      assert record.allFieldsDomains.size() == 2
      assert record.allFieldsDomains[0] == afd1
      assert record.allFieldsDomains[1] == afd2
      true
      true
    }
  }
}
