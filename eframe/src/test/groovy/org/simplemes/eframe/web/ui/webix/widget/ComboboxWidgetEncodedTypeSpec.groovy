package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.data.EncodedTypeListUtils
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.system.EnabledStatus
import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.JavascriptTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests for a combobox widget used with an EncodedType object.
 */
class ComboboxWidgetEncodedTypeSpec extends BaseWidgetSpecification {

  def "verify that the field is generated correctly - basic readOnly case"() {
    when: 'the UI element is built'
    def page = new ComboboxWidget(buildWidgetContext(readOnly: true, value: EnabledStatus.instance)).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the correct display value is used'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'label') == EnabledStatus.instance.toStringLocalized()
  }

  def "verify that the field is generated correctly - editable case"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: EnabledStatus.instance,
                                           format: EncodedTypeFieldFormat.instance, type: BasicStatus)
    def page = new ComboboxWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the correct display value is used'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'value') == EnabledStatus.instance.id

    and: 'the input width is the minimum width'
    def width = TextFieldWidget.adjustFieldCharacterWidth(ComboboxWidget.MINIMUM_WIDTH)
    JavascriptTestUtils.extractProperty(fieldLine, 'inputWidth') == """tk.pw("${width}em")"""
  }

  def "verify that the available values are in the correct order - order from the BasicStatus"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: EnabledStatus.instance,
                                           format: EncodedTypeFieldFormat.instance, type: BasicStatus)
    def page = new ComboboxWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the valid values are in the correct order'
    def optionsBlock = JavascriptTestUtils.extractBlock(page, 'options: [')
    optionsBlock.indexOf('ENABLED') < optionsBlock.indexOf('DISABLED')

    and: 'all values are in the list'
    for (e in EncodedTypeListUtils.instance.getAllValues(BasicStatus)) {
      optionsBlock.contains("""id: "${e.id.toString()}""")
      optionsBlock.contains("""value: "${e.toStringLocalized()}""")
    }
  }

  def "verify that the widget handles the null value case gracefully"() {
    when: 'the UI element is built'
    def page = new ComboboxWidget(buildWidgetContext(readOnly: true, value: null)).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the correct display value is used'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'label') == ''
  }

}
