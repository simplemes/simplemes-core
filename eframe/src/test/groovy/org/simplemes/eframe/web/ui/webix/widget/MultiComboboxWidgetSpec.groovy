/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget


import org.simplemes.eframe.data.format.DomainRefListFieldFormat
import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.AllFieldsDomain

/**
 * Tests.
 */
class MultiComboboxWidgetSpec extends BaseWidgetSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain]

  @Rollback
  def "verify that the field is generated correctly - editable case"() {
    when: 'the UI element is built'
    def afd1 = new AllFieldsDomain(name: 'ABC1', title: 'xyz').save()
    def afd3 = new AllFieldsDomain(name: 'ABC3', title: 'xyz').save()
    def afd2 = new AllFieldsDomain(name: 'ABC2', title: 'xyz').save()
    def widgetContext = buildWidgetContext(value: [afd1, afd3], format: DomainRefListFieldFormat.instance,
                                           referenceType: AllFieldsDomain)
    def page = new MultiComboboxWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the current value is used'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    JavascriptTestUtils.extractProperty(fieldLine, 'value') == "${afd1.uuid},${afd3.uuid}"

    and: 'the input width is the minimum width'
    def width = TextFieldWidget.adjustFieldCharacterWidth((int) (ComboboxWidget.MINIMUM_WIDTH * 1.5))
    JavascriptTestUtils.extractProperty(fieldLine, 'inputWidth') == """tk.pw("${width}em")"""

    and: 'the valid values are in the correct order'
    def optionsBlock = JavascriptTestUtils.extractBlock(page, 'options: [')
    optionsBlock.indexOf('ABC1') < optionsBlock.indexOf('ABC2')
    optionsBlock.indexOf('ABC2') < optionsBlock.indexOf('ABC3')

    and: 'all values are in the list'
    for (record in [afd1, afd2, afd3]) {
      optionsBlock.contains("""id: "${record.uuid}""")
      optionsBlock.contains("""value: "${record.name}""")
    }

    and: 'the view is the right toolkit view - a multi-select combo'
    page.contains('view: "multiComboEF"')
  }

  @Rollback
  def "verify that the field is generated correctly - readOnly case"() {
    when: 'the UI element is built'
    def afd1 = new AllFieldsDomain(name: 'ABC1', title: 'xyz').save()
    def afd3 = new AllFieldsDomain(name: 'ABC3', title: 'xyz').save()
    def list = [afd1, afd3]
    def widgetContext = buildWidgetContext(readOnly: true, value: list,
                                           format: DomainRefListFieldFormat.instance,
                                           referenceType: AllFieldsDomain)
    def page = new MultiComboboxWidget(widgetContext).build().toString()

    then: 'the page is valid'
    JavascriptTestUtils.checkScriptFragment(page)

    and: 'the display value is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "aField"')
    def s = list*.name.join(', ')
    fieldLine.contains(JavascriptUtils.escapeForJavascript(s, true))
  }
}
