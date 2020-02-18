/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.eframe.web.ui.JSPageOptions
import org.simplemes.eframe.web.ui.UIDefaults
import sample.domain.SampleParent

/**
 * Tests.
 */
class BaseLabeledFieldWidgetSpec extends BaseWidgetSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "verify that the label is generated correctly"() {
    when: 'the UI element is built'
    def page = new BaseLabeledFieldWidget(buildWidgetContext(readOnly: true, value: 'ABC')).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the spacing between the label and field is correct'
    def marginLine = TextUtils.findLine(page, "margin:")
    JavascriptTestUtils.extractProperty(marginLine, 'margin') == "${UIDefaults.FIELD_LABEL_GAP}"

    and: 'the field label is defined correctly'
    def labelLine = TextUtils.findLine(page, 'id: "aFieldLabel"')
    JavascriptTestUtils.extractProperty(labelLine, 'view') == "label"
    JavascriptTestUtils.extractProperty(labelLine, 'id') == "aFieldLabel"
    JavascriptTestUtils.extractProperty(labelLine, 'label') == "aField.label"
    JavascriptTestUtils.extractProperty(labelLine, 'align') == "right"
    labelLine.contains("width: tk.pw(ef.getPageOption('${JSPageOptions.LABEL_WIDTH_NAME}','${JSPageOptions.LABEL_WIDTH_DEFAULT}'))")
  }

  def "verify that the label is blank when a blank label is passed in"() {
    when: 'the UI element is built'
    def page = new BaseLabeledFieldWidget(buildWidgetContext(value: 'ABC', parameters: [label: ''])).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field label is not in the output'
    !TextUtils.findLine(page, 'id: "aFieldLabel"')
  }

  def "verify that display width is calculated correctly for various field widths"() {
    expect: 'the calculated width is correct'
    BaseLabeledFieldWidget.adjustFieldCharacterWidth(maxLength) == results

    where:
    maxLength | results
    1         | 2
    10        | 7
    20        | 15
    30        | 22
    40        | 22
    80        | 30
    100       | 45
  }

  @Rollback
  def "verify that field is generated correctly when a custom field is used"() {
    given: 'a custom field'
    DataGenerator.buildCustomField([domainClass: SampleParent, fieldName: 'custom1', afterFieldName: 'title'])

    when: 'the UI element is built'
    def page = new BaseLabeledFieldWidget(buildWidgetContext(custom: true, readOnly: true,
                                                             label: 'Custom Label',
                                                             domainObject: new SampleParent(),
                                                             value: 'ABC')).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the custom field is defined correctly'
    def labelLine = TextUtils.findLine(page, 'id: "aFieldLabel"')
    JavascriptTestUtils.extractProperty(labelLine, 'view') == "label"
    JavascriptTestUtils.extractProperty(labelLine, 'id') == "aFieldLabel"
    def valueLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(valueLine, 'label') == "ABC"

    and: 'the custom label is correct'
    def customFieldLine = TextUtils.findLine(page, 'id: "aFieldLabel"')
    JavascriptTestUtils.extractProperty(customFieldLine, 'label') == "Custom Label"
  }


  // test required flag

}
