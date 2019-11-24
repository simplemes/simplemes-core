package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.i18n.GlobalUtils
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
class DateFieldWidgetSpec extends BaseWidgetSpecification {

  def "verify that the field is correct - basic readOnly case - multiple locales"() {
    given: 'a locale'
    GlobalUtils.defaultLocale = locale

    when: 'the UI element is built'
    def date = new Date()
    def widgetContext = buildWidgetContext(readOnly: true, value: date, format: DateFieldFormat.instance)
    def page = new DateFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field value is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'label') == DateFieldFormat.instance.format(date, null, null)

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that the field is correct - editable - multiple locales"() {
    given: 'a locale'
    GlobalUtils.defaultLocale = locale

    when: 'the UI element is built'
    def date = new Date()
    def widgetContext = buildWidgetContext(value: date, format: DateFieldFormat.instance)
    def page = new DateFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field value is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'name') == 'aField'
    JavascriptTestUtils.extractProperty(fieldLine, 'view') == 'datepicker'
    JavascriptTestUtils.extractProperty(fieldLine, 'editable') == 'true'
    JavascriptTestUtils.extractProperty(fieldLine, 'timepicker') == 'true'
    JavascriptTestUtils.extractProperty(fieldLine, 'stringResult') == 'true'

    and: 'the value is correct'
    JavascriptTestUtils.extractProperty(fieldLine, 'value') == DateUtils.formatForm(date)

    and: 'the input width is correct'
    def width = TextFieldWidget.adjustFieldCharacterWidth(20)
    JavascriptTestUtils.extractProperty(fieldLine, 'inputWidth') == """tk.pw("${width}em")"""

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that the field widget handles null values"() {
    given: 'a mocked domain utils'
    new MockDomainUtils(this, SampleParent).install()

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(readOnly: true, format: BigDecimalFieldFormat.instance)
    def page = new DateFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field value is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'label') == ''
  }

  def "verify that the field is highlighted as an error"() {
    when: 'the UI element is built'
    def page = new DateFieldWidget(buildWidgetContext(value: new Date(), error: true)).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the input field is highlighted as an error field'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'css') == "webix_invalid"
  }

  // test required flag

}
