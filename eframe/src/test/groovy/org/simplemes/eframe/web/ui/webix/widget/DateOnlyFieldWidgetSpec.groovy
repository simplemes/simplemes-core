package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.JavascriptTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DateOnlyFieldWidgetSpec extends BaseWidgetSpecification {

  def "verify that the field is correct - basic readOnly case - multiple locales"() {
    given: 'a locale'
    GlobalUtils.defaultLocale = locale

    when: 'the UI element is built'
    def date = new DateOnly()
    def widgetContext = buildWidgetContext(readOnly: true, value: date, format: DateOnlyFieldFormat.instance)
    def page = new DateOnlyFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field value is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'label') == DateOnlyFieldFormat.instance.format(date, null, null)

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that the field is correct - editable case - multiple locales"() {
    given: 'a locale'
    GlobalUtils.defaultLocale = locale

    when: 'the UI element is built'
    def date = new DateOnly()
    def widgetContext = buildWidgetContext(value: date, format: DateOnlyFieldFormat.instance)
    def page = new DateOnlyFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field value is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'name') == 'aField'
    JavascriptTestUtils.extractProperty(fieldLine, 'view') == 'datepicker'
    JavascriptTestUtils.extractProperty(fieldLine, 'editable') == 'true'
    JavascriptTestUtils.extractProperty(fieldLine, 'timepicker') == 'false'
    JavascriptTestUtils.extractProperty(fieldLine, 'stringResult') == 'true'

    and: 'the value is correct'
    JavascriptTestUtils.extractProperty(fieldLine, 'value') == DateOnlyFieldFormat.instance.formatForm(date, null, null)

    and: 'the input width is correct'
    def width = TextFieldWidget.calculateFieldWidth(10)
    JavascriptTestUtils.extractProperty(fieldLine, 'inputWidth') == """tk.pw("${width}em")"""


    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that the field widget handles null values"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(readOnly: true, format: DateOnlyFieldFormat.instance)
    def page = new DateOnlyFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field value is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'label') == ''
  }

  // test field non-readOnly
  // test required flag

}
