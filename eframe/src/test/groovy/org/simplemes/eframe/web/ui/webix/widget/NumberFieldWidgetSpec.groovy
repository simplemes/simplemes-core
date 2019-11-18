package org.simplemes.eframe.web.ui.webix.widget


import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.NumberUtils
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
class NumberFieldWidgetSpec extends BaseWidgetSpecification {

  def "verify that the field is correct for the numberFieldWidget - basic readOnly case - multiple locales"() {
    given: 'a mocked domain utils'
    new MockDomainUtils(this, SampleParent).install()

    and: 'a locale'
    GlobalUtils.defaultLocale = locale

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(readOnly: true, value: 12.2, format: BigDecimalFieldFormat.instance)
    def page = new NumberFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field value is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'label') == NumberUtils.formatNumber(12.2)

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that the field field widget handles null values"() {
    given: 'a mocked domain utils'
    new MockDomainUtils(this, SampleParent).install()

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(readOnly: true, format: BigDecimalFieldFormat.instance)
    def page = new NumberFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the field value is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'label') == ''

  }

  def "verify that the field field widget works with non-readOnly case"() {
    given: 'a mocked domain utils'
    new MockDomainUtils(this, SampleParent).install()

    and: 'a locale'
    GlobalUtils.defaultLocale = locale

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(readOnly: false, value: 12.2, format: BigDecimalFieldFormat.instance)
    def page = new NumberFieldWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the input field is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'view') == "text"
    JavascriptTestUtils.extractProperty(fieldLine, 'value') == NumberUtils.formatNumber(12.2)
    JavascriptTestUtils.extractProperty(fieldLine, 'inputWidth') == """tk.pw("${
      TextFieldWidget.calculateFieldWidth(10)
    }em")"""

    and: 'there is no max length on the field'
    !fieldLine.contains('attributes:')

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  // test field non-readOnly
  // test required flag

}
