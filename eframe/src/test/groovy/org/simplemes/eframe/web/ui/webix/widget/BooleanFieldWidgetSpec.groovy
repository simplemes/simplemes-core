package org.simplemes.eframe.web.ui.webix.widget


import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockDomainUtils
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class BooleanFieldWidgetSpec extends BaseWidgetSpecification {

  def "verify that the field is correct for the numberFieldWidget - basic readOnly case"() {
    given: 'a mocked domain utils'
    new MockDomainUtils(this, SampleParent).install()

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(readOnly: true, value: value, format: BooleanFieldFormat.instance)
    def page = new BooleanFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field value is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'view') == 'checkbox'
    JavascriptTestUtils.extractProperty(fieldLine, 'value') == displayValue
    JavascriptTestUtils.extractProperty(fieldLine, 'disabled') == 'true'

    where:
    displayValue | value
    'false'      | null
    'false'      | false
    'true'       | true
  }

  def "verify that the field is correct for the numberFieldWidget - basic editable case"() {
    given: 'a mocked domain utils'
    new MockDomainUtils(this, SampleParent).install()

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: value, format: BooleanFieldFormat.instance)
    def page = new BooleanFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field value is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'view') == "checkbox"
    JavascriptTestUtils.extractProperty(fieldLine, 'name') == "aField"
    JavascriptTestUtils.extractProperty(fieldLine, 'value') == displayValue
    JavascriptTestUtils.extractProperty(fieldLine, 'disabled') == 'false'

    where:
    displayValue | value
    'false'      | null
    'false'      | false
    'true'       | true
  }


}
