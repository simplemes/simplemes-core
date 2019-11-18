package org.simplemes.eframe.web.ui.webix.widget


import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import sample.domain.SampleParent
import sample.page.SampleParentEditPage
import sample.page.SampleParentShowPage
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
class TextFieldWidgetGUISpec extends BaseGUISpecification {

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

}
