package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.reports.ReportTimeIntervalEnum
import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.JavascriptTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests for Combobox field with Enumeration list.
 */
class ComboboxWidgetEnumSpec extends BaseWidgetSpecification {

  def "verify that the field is generated correctly - basic readOnly case"() {
    when: 'the UI element is built'
    def page = new ComboboxWidget(buildWidgetContext(readOnly: true, value: ReportTimeIntervalEnum.LAST_6_MONTHS)).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the correct display value is used'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'label') == ReportTimeIntervalEnum.LAST_6_MONTHS.toStringLocalized()
  }

  def "verify that the field is generated correctly - editable case"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: ReportTimeIntervalEnum.LAST_7_DAYS,
                                           format: EnumFieldFormat.instance, type: ReportTimeIntervalEnum)
    def page = new ComboboxWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the correct display value is used'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'value') == ReportTimeIntervalEnum.LAST_7_DAYS.toString()

    and: 'the input width is the minimum width'
    def width = TextFieldWidget.adjustFieldCharacterWidth(ComboboxWidget.MINIMUM_WIDTH)
    JavascriptTestUtils.extractProperty(fieldLine, 'inputWidth') == """tk.pw("${width}em")"""
  }

  def "verify that the available values are in the correct order - order in the enum"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: ReportTimeIntervalEnum.LAST_7_DAYS,
                                           format: EnumFieldFormat.instance, type: ReportTimeIntervalEnum)
    def page = new ComboboxWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the valid values are in the correct order'
    def optionsBlock = JavascriptTestUtils.extractBlock(page, 'options: [')
    optionsBlock.indexOf('TODAY') < optionsBlock.indexOf('YESTERDAY')
    optionsBlock.indexOf('YESTERDAY') < optionsBlock.indexOf('LAST_7_DAYS')
    optionsBlock.indexOf('LAST_7_DAYS') < optionsBlock.indexOf('CUSTOM_RANGE')

    and: 'all values are in the list'
    for (e in ReportTimeIntervalEnum.enumConstants) {
      optionsBlock.contains("""id: "${e.toString()}""")
      optionsBlock.contains("""value: "${e.toStringLocalized()}""")
    }
  }

}
