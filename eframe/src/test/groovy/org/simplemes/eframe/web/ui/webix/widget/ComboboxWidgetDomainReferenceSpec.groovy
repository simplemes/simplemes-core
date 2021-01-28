/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.JavascriptTestUtils
import sample.domain.AllFieldsDomain

/**
 * Tests Combobox widget use for Simple domain reference field.
 */
class ComboboxWidgetDomainReferenceSpec extends BaseWidgetSpecification {

  static dirtyDomains = [AllFieldsDomain]

  def "verify that the field is generated correctly - basic readOnly case"() {
    when: 'the UI element is built'
    def afd = new AllFieldsDomain(name: 'ABC', title: 'xyz')
    def page = new ComboboxWidget(buildWidgetContext(readOnly: true, value: afd)).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the short value is used'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'label') == GlobalUtils.toStringLocalized(afd)
  }

  def "verify that the current field value is escaped for javascript correctly - readOnly case"() {
    when: 'the UI element is built'
    def afd = new AllFieldsDomain(name: 'ABC', title: '<script>"x</script>')
    def page = new ComboboxWidget(buildWidgetContext(readOnly: true, value: afd)).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the display value is escaped correctly.'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    fieldLine.contains(JavascriptUtils.escapeForJavascript(GlobalUtils.toStringLocalized(afd), true))
  }

  def "verify that the field is generated correctly - editable case"() {
    when: 'the UI element is built'
    def afd = new AllFieldsDomain(name: 'ABC', title: 'xyz', uuid: UUID.randomUUID())
    def widgetContext = buildWidgetContext(value: afd, format: DomainReferenceFieldFormat.instance,
                                           referenceType: AllFieldsDomain)
    def page = new ComboboxWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the current value is used'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'value') == afd.uuid.toString()

    and: 'the input width is the minimum width'
    def width = TextFieldWidget.adjustFieldCharacterWidth(ComboboxWidget.MINIMUM_WIDTH)
    JavascriptTestUtils.extractProperty(fieldLine, 'inputWidth') == """tk.pw("${width}em")"""
  }

  def "verify that the field is generated correctly - editable with null value case"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: null, format: DomainReferenceFieldFormat.instance,
                                           referenceType: AllFieldsDomain)
    def page = new ComboboxWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the current value is used'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'value') == ''
  }

  def "verify that the field width is correct for larger values"() {
    given: 'a domain record for the list'
    def afd = null
    AllFieldsDomain.withTransaction {
      afd = new AllFieldsDomain(name: '1234567890123456789012345678901234567890', title: '12345678901234567890').save()
    }

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: afd, format: DomainReferenceFieldFormat.instance,
                                           referenceType: AllFieldsDomain)
    def page = new ComboboxWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the input width is the minimum width'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    def width = TextFieldWidget.adjustFieldCharacterWidth(40)
    JavascriptTestUtils.extractProperty(fieldLine, 'inputWidth') == """tk.pw("${width}em")"""
  }

  def "verify that the input width calculation works for the expected sizes"() {
    given: 'a widget'
    def widgetContext = buildWidgetContext(format: DomainReferenceFieldFormat.instance,
                                           referenceType: AllFieldsDomain)
    def widget = new ComboboxWidget(widgetContext)

    and: 'the mock maxValueWidth is set'
    widget.maxValueWidth = maxValueWidth

    expect: 'the calculated value is correct'
    widget.inputWidth == results

    where:
    maxValueWidth | results
    10            | 10
    20            | 20
    30            | 30
    40            | 40
    50            | 40
  }

  def "verify that the display values are escaped for javascript correctly - editable case"() {
    given: 'a domain record for the list'
    def afd = null
    AllFieldsDomain.withTransaction {
      afd = new AllFieldsDomain(name: 'ABC', title: '<script>"x</script>').save()
    }

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(value: afd, format: DomainReferenceFieldFormat.instance,
                                           referenceType: AllFieldsDomain)
    def page = new ComboboxWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the list of valid values is escaped correctly'
    def optionsBlock = JavascriptTestUtils.extractBlock(page, 'options: [')
    optionsBlock.contains(JavascriptUtils.escapeForJavascript(TypeUtils.toShortString(afd, true)))
  }

  def "verify that the available values are sorted correctly"() {
    given: 'some records'
    def records = DataGenerator.generate {
      domain AllFieldsDomain
      count 10
      values name: 'ABC-${r}'
    } as List<AllFieldsDomain>

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(format: DomainReferenceFieldFormat.instance,
                                           referenceType: AllFieldsDomain)
    def page = new ComboboxWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the valid values are in the correct order'
    def optionsBlock = JavascriptTestUtils.extractBlock(page, 'options: [')
    optionsBlock.indexOf('ABC-001') < optionsBlock.indexOf('ABC-002')
    optionsBlock.indexOf('ABC-004') < optionsBlock.indexOf('ABC-005')
    optionsBlock.indexOf('ABC-006') < optionsBlock.indexOf('ABC-007')
    optionsBlock.indexOf('ABC-008') < optionsBlock.indexOf('ABC-009')

    and: 'all values are in the list'
    for (record in records) {
      optionsBlock.contains("""id: "${record.uuid}""")
      optionsBlock.contains("""value: "${record.name}""")
    }
  }

  def "verify that the onChange javascript can be passed in"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [onChange: 'someChangeLogic'])
    def page = new ComboboxWidget(widgetContext).build().toString()
    //println "page = $page"

    then: 'the onChange script is used'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    fieldLine.contains("""on:{onChange(newValue, oldValue){someChangeLogic}}""")
  }


  // GUI Test drop down works.
  // test row limits/ dynamic retrieval

}
