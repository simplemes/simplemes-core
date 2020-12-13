/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.web.ui.webix.ToolkitConstants
import sample.controller.OrderController
import sample.domain.SampleParent
import sample.page.OrderCreatePage
import sample.page.SampleParentEditPage
import sample.page.SampleParentShowPage
import spock.lang.IgnoreIf

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
class TextFieldWidgetGUISpec extends BaseGUISpecification {

  @SuppressWarnings('unused')
  static dirtyDomains = [SampleParent]

  def "verify that the field value is escaped and visible - readOnly mode"() {
    given: 'a value to edit'
    def (SampleParent sampleParent) = DataGenerator.generate {
      domain SampleParent
      values name: 'XYZ', notes: '<script>alert()</script>'
    }

    when: 'a page is displayed with the field'
    login()
    to SampleParentShowPage, sampleParent

    then: 'the value is displayed as the user expects to see it'
    notes.value == sampleParent.notes
  }

  def "verify that the field value is escaped and visible - edit mode"() {
    given: 'a value to edit'
    def (SampleParent sampleParent) = DataGenerator.generate {
      domain SampleParent
      values name: 'XYZ', notes: '<script>alert()</script>'
    }

    when: 'a page is displayed with the field'
    login()
    to SampleParentEditPage, sampleParent

    then: 'the value is displayed as the user expects to see it'
    notes.input.value() == sampleParent.notes
  }

  def "verify that the suggest option works"() {
    when: 'a page is displayed with the field'
    login()
    to OrderCreatePage

    and: 'the counters are reset for this run'
    OrderController.suggestCount = 0
    OrderController.suggestLatestParameters = null

    and: 'a value is started'
    product.input.click()
    sleep(1000)
    sendKey('J')
    waitFor {
      $('div.webix_popup', view_id: '$suggest1').displayed
    }

    then: 'suggest works'
    $('div.webix_popup', view_id: '$suggest1').text().contains('J1002')

    and: 'the controller was called with the right URI and only once'
    OrderController.suggestCount == 1
    OrderController.suggestLatestParameters.workCenter == 'WC_XYZZY'
    OrderController.suggestLatestParameters[ToolkitConstants.SUGGEST_FILTER_PARAMETER_NAME] == 'J'
  }

}
