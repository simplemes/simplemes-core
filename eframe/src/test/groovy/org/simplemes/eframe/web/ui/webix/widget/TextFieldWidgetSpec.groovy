package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.web.ui.JSPageOptions
import org.simplemes.eframe.web.ui.UIDefaults

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class TextFieldWidgetSpec extends BaseWidgetSpecification {

  def "verify that the field is generated correctly - basic readOnly case"() {
    when: 'the UI element is built'
    def page = new TextFieldWidget(buildWidgetContext(readOnly: true, value: 'ABC')).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the spacing between the label and field is correct'
    def marginLine = TextUtils.findLine(page, "margin:")
    JavascriptTestUtils.extractProperty(marginLine, 'margin') == "${UIDefaults.FIELD_LABEL_GAP}"
    //marginLine.contains("margin: ${UIDefaults.FIELD_LABEL_GAP},")

    and: 'the field label is defined correctly'
    def labelLine = TextUtils.findLine(page, 'id: "aFieldLabel"')
    JavascriptTestUtils.extractProperty(labelLine, 'view') == "label"
    JavascriptTestUtils.extractProperty(labelLine, 'id') == "aFieldLabel"
    JavascriptTestUtils.extractProperty(labelLine, 'label') == "aField.label"
    JavascriptTestUtils.extractProperty(labelLine, 'align') == "right"
    labelLine.contains("width: tk.pw(ef.getPageOption('${JSPageOptions.LABEL_WIDTH_NAME}','${JSPageOptions.LABEL_WIDTH_DEFAULT}'))")

    and: 'the field value is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'view') == "label"
    JavascriptTestUtils.extractProperty(fieldLine, 'id') == "aField"
    JavascriptTestUtils.extractProperty(fieldLine, 'label') == "ABC"
  }


  def "verify that the HTML values are escaped correctly - readOnly case"() {
    when: 'the UI element is built'
    def page = new TextFieldWidget(buildWidgetContext(readOnly: true, value: '<script>abc</script>')).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the value has the HTML escaped'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'label') == JavascriptUtils.escapeForJavascript('<script>abc</script>', true)
  }

  def "verify that the the labelWidth can be overridden"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(readOnly: true, value: '<script>abc</script>', parameters: [labelWidth: '35%'])
    def page = new TextFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field label width uses the override'
    def labelLine = TextUtils.findLine(page, 'id: "aFieldLabel"')
    labelLine.contains("width: tk.pw('35%')")
  }

  def "verify that the the ID for the label and field can be overridden"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(readOnly: false, value: '<script>abc</script>', parameters: [id: 'custom'])
    def page = new TextFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the ID is used for both label and field'
    page.contains('id: "customLabel"')
    page.contains('id: "custom"')

    and: 'the attribute is set for the input HTML field itself'
    def fieldLine = TextUtils.findLine(page, 'id: "custom"')
    def fieldAttributes = JavascriptTestUtils.extractBlock(fieldLine, 'attributes:')
    fieldAttributes.contains('id: "custom"')
  }

  def "verify that the field is generated correctly - basic editable case"() {
    when: 'the UI element is built'
    def page = new TextFieldWidget(buildWidgetContext(readOnly: false, value: 'ABC', maxLength: 40)).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the input field is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'view') == "text"
    JavascriptTestUtils.extractProperty(fieldLine, 'value') == "ABC"
    def width = TextFieldWidget.calculateFieldWidth(40)
    JavascriptTestUtils.extractProperty(fieldLine, 'inputWidth') == """tk.pw("${width}em")"""

    def fieldAttributes = JavascriptTestUtils.extractBlock(fieldLine, 'attributes:')
    fieldAttributes.contains('maxlength: 40')
  }


  def "verify that the field is highlighted as an error"() {
    when: 'the UI element is built'
    def page = new TextFieldWidget(buildWidgetContext(value: 'ABC', error: true)).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the input field is highlighted as an error field'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'css') == "webix_invalid"
  }

  def "verify that the javascript problem values are escaped correctly - editable case"() {
    when: 'the UI element is built'
    def page = new TextFieldWidget(buildWidgetContext(value: '<script>"abc</script>')).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the value has the string escaped for use as a javascript string'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    def value1 = fieldLine[fieldLine.indexOf('value: "')..fieldLine.lastIndexOf('attributes')]
    def value2 = value1[value1.indexOf('value: "')..value1.lastIndexOf('"')]
    value2 == 'value: "<script>\\"abc<\\/script>"'
  }

  def "verify that the type of password can be passed in"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [type: 'password'])
    def page = new TextFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field type is used'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'type') == "password"
  }

  def "verify that the type password-no-auto can be passed in"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: 'ABC', maxLength: 40, parameters: [type: 'password-no-auto'])
    def page = new TextFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field type is used'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    def attributesBlock = JavascriptTestUtils.extractBlock(fieldLine, 'attributes: {')
    JavascriptTestUtils.extractProperty(attributesBlock, 'autocomplete').contains('new-password')
  }

  def "verify that the width can be passed in"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [width: '20'])
    def page = new TextFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the width is used'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    def width = TextFieldWidget.calculateFieldWidth(20)
    JavascriptTestUtils.extractProperty(fieldLine, 'inputWidth') == """tk.pw("${width}em")"""
  }

  def "verify that the field is flagged as required correctly"() {
    when: 'the UI element is built'
    def page = new TextFieldWidget(buildWidgetContext(readOnly: false, value: 'ABC',
                                                      parameters: [required: required])).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field label is flagged as required'
    def labelLine = TextUtils.findLine(page, 'id: "aFieldLabel"')
    JavascriptTestUtils.extractProperty(labelLine, 'label') == label

    and: 'the field value is flagged as required'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'required') == fieldFlag

    where:
    required | label           | fieldFlag
    'true'   | '*aField.label' | "true"
    true     | '*aField.label' | "true"
    'false'  | 'aField.label'  | null
    false    | 'aField.label'  | null
    null     | 'aField.label'  | null
  }


  // GUI tests
  //  maxLength is enforced.

}
