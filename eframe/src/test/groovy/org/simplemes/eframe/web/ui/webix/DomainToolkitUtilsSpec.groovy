package org.simplemes.eframe.web.ui.webix

import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.ChildListFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.DomainRefListFieldFormat
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockDomainUtils
import org.simplemes.eframe.test.MockFieldDefinitions
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import org.simplemes.eframe.web.ui.webix.widget.TextFieldWidget
import org.simplemes.eframe.web.ui.webix.widget.WidgetContext
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

import java.text.SimpleDateFormat

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DomainToolkitUtilsSpec extends BaseSpecification {

  def "verify that buildTableColumns basic column creation works"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, AllFieldsDomain.fieldOrder)

    then: 'the string primary key is generated correctly'
    def name = TextUtils.findLine(page, 'id: "name"')
    name.contains("""header: {text: "${lookup('name.label')}"}""")
    name.contains("""adjust: false""")
    name.contains("""format: webix.template.escape""")
    name.contains("""template: "<a href='/allFieldsDomain/show/#id#'>#name#</a>\"""")

    and: 'the non-key string column is generated correctly'
    def title = TextUtils.findLine(page, 'id: "title"')
    title.contains("""header: {text: "${lookup('title.label')}"}""")
    title.contains("""adjust: false""")
    title.contains("""format: webix.template.escape""")
    !title.contains("""href""")

    and: 'the BigDecimal type is correct'
    def qty = TextUtils.findLine(page, 'id: "qty"')
    !qty.contains("""webix.template.escape""")

    and: 'the integer type is correct'
    def count = TextUtils.findLine(page, 'id: "count"')
    !count.contains("""webix.template.escape""")

    and: 'the Boolean type is correct'
    def enabled = TextUtils.findLine(page, 'id: "enabled"')
    enabled.contains("""template: "{common.checkbox()}\"""")
    !enabled.contains("""webix.template.escape""")
  }

  def "verify that buildTableColumns builds formatters for special columns"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, AllFieldsDomain.fieldOrder)

    then: 'the dateOnly field has the correct formatter '
    def dueDate = TextUtils.findLine(page, 'id: "dueDate"')
    dueDate.contains("""format: webix.i18n.dateFormatStr""")

    and: 'the date field has the correct formatter '
    def dateTime = TextUtils.findLine(page, 'id: "dateTime"')
    dateTime.contains("""format: webix.i18n.fullDateFormatStr""")

    and: 'the BigDecimal type is correct'
    def qty = TextUtils.findLine(page, 'id: "qty"')
    qty.contains("""format: webix.i18n.numberFormat""")
  }

  def "verify that buildTableColumns controller path passed in as option works"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain,
                                                             AllFieldsDomain.fieldOrder,
                                                             [_controllerRoot: '/XYZZY'])

    then: 'the string primary key is generated correctly'
    def name = TextUtils.findLine(page, 'id: "name"')
    name.contains("""template: "<a href='/XYZZY/show""")
  }

  def "verify that buildTableColumns builds formatters for simple domain references"() {
    given: 'mocked domains'
    //new MockDomainUtils(this, [AllFieldsDomain, SampleParent],new MockFieldDefinitions(['name', 'allFieldsDomain'])).install()
    def fieldDefinitions = new MockFieldDefinitions([name: String, allFieldsDomain: AllFieldsDomain])
    new MockDomainUtils(this, [AllFieldsDomain, SampleParent], fieldDefinitions).install()

    and: 'a mocked field format for the domain reference'
    DomainReferenceFieldFormat.instance = Mock(DomainReferenceFieldFormat)
    DomainReferenceFieldFormat.instance.getGridEditor(*_) >> 'combo'

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(SampleParent, ['name', 'allFieldsDomain'], [displayValueForCombo: true])

    then: 'the reference has code to extract the key field'
    def afdText = TextUtils.findLine(page, 'id: "allFieldsDomain"')
    afdText.contains(""",template:function(obj){return ef._getMemberSafely(obj,"allFieldsDomain","name");}""")

    cleanup:
    DomainReferenceFieldFormat.instance = new DomainReferenceFieldFormat()
  }

  def "verify that buildTableColumns builds the column definition for encoded types"() {
    given: 'mocked domains'
    def fieldDefinitions = new MockFieldDefinitions([name: String, status: BasicStatus])
    new MockDomainUtils(this, [AllFieldsDomain], fieldDefinitions).install()

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, ['name', 'status'])

    then: 'the editor is a combo type'
    def columnText = TextUtils.findLine(page, 'id: "status"')
    JavascriptTestUtils.extractProperty(columnText, 'editor') == 'combo'

    and: 'the encoded type has no template'
    // No need for a template when used with a 'combo' editor.
    !columnText.contains("template")

    and: 'the max width is correct'
    def suggestText = JavascriptTestUtils.extractBlock(columnText, 'suggest: {')
    JavascriptTestUtils.extractProperty(suggestText, 'fitMaster') == 'false'
    def width = TextFieldWidget.calculateFieldWidth(15)
    JavascriptTestUtils.extractProperty(suggestText, 'width') == """tk.pw("${width}em")"""
  }

  def "verify that buildTableColumns builds column definition for enum values"() {
    given: 'mocked domains'
    def fieldDefinitions = new MockFieldDefinitions([name: String, reportTimeInterval: ReportTimeIntervalEnum])
    new MockDomainUtils(this, [AllFieldsDomain], fieldDefinitions).install()

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, ['name', 'reportTimeInterval'])

    then: 'the enum has no template'
    // No need for a template when used with a 'combo' editor.
    def rtiText = TextUtils.findLine(page, 'id: "reportTimeInterval"')
    !rtiText.contains("template")

    and: 'the max width is correct'
    def suggestText = JavascriptTestUtils.extractBlock(rtiText, 'suggest: {')
    JavascriptTestUtils.extractProperty(suggestText, 'fitMaster') == 'false'
    def width = TextFieldWidget.calculateFieldWidth(15)
    JavascriptTestUtils.extractProperty(suggestText, 'width') == """tk.pw("${width}em")"""

    and: 'the list of values is correct'
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

  def "verify that buildTableColumns detects readOnly-true scenarios adjusts the grid as needed"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    and: 'options with the readOnly widgetContext'
    def widgetContext = new WidgetContext(readOnly: true)
    def options = [_widgetContext: widgetContext]

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, AllFieldsDomain.fieldOrder, options)

    then: 'there is no editor defined'
    !page.contains('editor:')
  }

  def "verify that buildTableColumns detects readOnly-true for boolean checkbox template"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    and: 'options with the readOnly widgetContext'
    def widgetContext = new WidgetContext(readOnly: true)
    def options = [_widgetContext: widgetContext]

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, ['enabled'], options)

    then: 'the boolean field has the correct checkbox renderer'
    def enabled = TextUtils.findLine(page, 'id: "enabled"')
    enabled.contains("""template: "{common.readOnlyCheckbox()}\"""")
  }

  def "verify that buildTableColumns detects readOnly-false scenarios and writes the editor for each column"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    and: 'options with the readOnly widgetContext'
    def widgetContext = new WidgetContext(readOnly: false)
    def options = [_widgetContext: widgetContext]

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, AllFieldsDomain.fieldOrder, options)

    then: 'the string primary key is generated correctly'
    def name = TextUtils.findLine(page, 'id: "name"')
    JavascriptTestUtils.extractProperty(name, 'editor') == 'text'

    then: 'the boolean field is generated correctly with the right editor'
    def enabled = TextUtils.findLine(page, 'id: "enabled"')
    JavascriptTestUtils.extractProperty(enabled, 'editor') == 'checkbox'
  }

  def "verify that buildTableDataParser parsing with dates works"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the parsing scheme is created'
    def page = DomainToolkitUtils.instance.buildTableDataParser(AllFieldsDomain, AllFieldsDomain.fieldOrder)

    then: 'the scheme is defined'
    page.contains('scheme:{')

    and: 'the dateOnly field is parsed'
    def dueDate = JavascriptTestUtils.extractBlock(page, "if (typeof obj.dueDate == 'string') {")
    dueDate.contains("""obj.dueDate = tk._parseISODate(obj.dueDate,true);""")

    and: 'the date field is parsed'
    def dateTime = JavascriptTestUtils.extractBlock(page, "if (typeof obj.dateTime == 'string') {")
    dateTime.contains("""obj.dateTime = tk._parseISODate(obj.dateTime,true);""")
  }

  def "verify that buildTableDataParser does not generate a scheme when no dates are used"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the parsing scheme is created'
    def page = DomainToolkitUtils.instance.buildTableDataParser(AllFieldsDomain, ['title', 'name'])

    then: 'no scheme is defined'
    !page.contains('scheme:{')
  }

  def "verify that convertDateFormatToToolkit converts date correctly"() {
    expect: 'the format is converted correctly'
    def s1 = DomainToolkitUtils.instance.convertDateFormatToToolkit((SimpleDateFormat) DateUtils.getDateOnlyFormat(locale))
    s1 == dateOnlyFormat
    def s2 = DomainToolkitUtils.instance.convertDateFormatToToolkit((SimpleDateFormat) DateUtils.getDateFormat(locale))
    s2 == dateFormat

    where:
    locale                         | dateOnlyFormat | dateFormat
    Locale.US                      | '%n/%j/%y'     | '%n/%j/%y %g:%i:%s %A'
    Locale.GERMANY                 | '%d.%m.%y'     | '%d.%m.%y %H:%i:%s'
    Locale.forLanguageTag('es-ES') | '%j/%m/%y'     | '%j/%m/%y %G:%i:%s'
    Locale.forLanguageTag('be-BY') | '%j.%n.%y'     | '%j.%n.%y %G.%i.%s'
    Locale.forLanguageTag('fr-FR') | '%d/%m/%y'     | '%d/%m/%y %H:%i:%s'
    Locale.forLanguageTag('en-GB') | '%d/%m/%y'     | '%d/%m/%y %H:%i:%s'
    Locale.forLanguageTag('it-IT') | '%d/%m/%y'     | '%d/%m/%y %G.%i.%s'
    Locale.forLanguageTag('ja-JP') | '%y/%m/%d'     | '%y/%m/%d %G:%i:%s'
    Locale.forLanguageTag('pt-BR') | '%d/%m/%y'     | '%d/%m/%y %H:%i:%s'
    Locale.forLanguageTag('ru-RU') | '%d.%m.%y'     | '%d.%m.%y %G:%i:%s'
    Locale.forLanguageTag('zh-CN') | '%y-%n-%j'     | '%y-%n-%j %G:%i:%s'
  }

  // verify that column width options can be passed in to buildTableColumns
  def "verify that buildTableColumns supports column widths as an option"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the columns are created for a domain'
    def widths = [title: 23.2, name: 14.23]
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, ['title', 'name'], [widths: widths])

    then: 'the title width is used'
    def titleLine = TextUtils.findLine(page, 'id: "title"')
    titleLine.contains('width: tk.pw("23.2%")')

    and: 'the name width is used'
    def nameLine = TextUtils.findLine(page, 'id: "name"')
    nameLine.contains('width: tk.pw("14.23%")')
  }

  def "verify that buildTableColumns supports option to spread the full default width over all columns"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, ['title', 'name'])

    then: 'the title width is used'
    def titleLine = TextUtils.findLine(page, 'id: "title"')
    titleLine.contains('width: tk.pw("47.5%")')

    and: 'the name width is used'
    def nameLine = TextUtils.findLine(page, 'id: "name"')
    nameLine.contains('width: tk.pw("47.5%")')
  }

  def "verify that buildTableColumns supports the totalWidth option - spreads over all columns"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, ['title', 'name'], [totalWidth: 60.0])

    then: 'the title width is used'
    def titleLine = TextUtils.findLine(page, 'id: "title"')
    titleLine.contains('width: tk.pw("30.0%")')

    and: 'the name width is used'
    def nameLine = TextUtils.findLine(page, 'id: "name"')
    nameLine.contains('width: tk.pw("30.0%")')
  }

  def "verify that buildTableColumns supports the keyHyperlink option"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, ['title', 'name'], [keyHyperlink: false])

    then: 'the hyperlink is not created'
    def name = TextUtils.findLine(page, 'id: "name"')
    !name.contains("""<a href=""")
  }

  def "verify that buildTableColumns supports the sort option - server"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, ['title', 'name'], [sort: 'server'])

    then: 'the columns are defined to sort on the server'
    def name = TextUtils.findLine(page, 'id: "name"')
    JavascriptTestUtils.extractProperty(name, 'sort') == 'server'
  }

  def "verify that buildTableColumns supports the sort option - true"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, ['title', 'name'], [sort: true])

    then: 'the columns are defined to sort on the server'
    def name = TextUtils.findLine(page, 'id: "name"')
    JavascriptTestUtils.extractProperty(name, 'sort') == 'text'
  }

  def "verify that buildTableColumns supports the sort option - false"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, ['title', 'name'], [sort: false])

    then: 'the columns are defined to sort on the server'
    def name = TextUtils.findLine(page, 'id: "name"')
    JavascriptTestUtils.extractProperty(name, 'sort') == null
  }

  def "verify that buildTableColumns supports the displayValueForCombo option - true"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the columns are created for a domain'
    def page = DomainToolkitUtils.instance.buildTableColumns(AllFieldsDomain, ['status'], [displayValueForCombo: true])

    then: 'the columns are defined to sort on the server'
    def templateText = TextUtils.findLine(page, 'template:function')
    templateText.contains(""",template:function(obj){return ef._getMemberSafely(obj,"_statusDisplay_");}""")
  }

  def "verify that getSortType supports handles the supported field types"() {
    expect: 'the columns are created for a domain'
    DomainToolkitUtils.instance.getSortType(format) == result

    where:
    format                     | result
    StringFieldFormat          | 'text'
    IntegerFieldFormat         | 'int'
    LongFieldFormat            | 'int'
    BigDecimalFieldFormat      | 'int'
    BooleanFieldFormat         | 'text'
    DateOnlyFieldFormat        | 'date'
    DateFieldFormat            | 'date'
    DomainReferenceFieldFormat | 'text'
    EnumFieldFormat            | 'text'
    EncodedTypeFieldFormat     | 'text'
    ChildListFieldFormat       | 'text'
    DomainRefListFieldFormat   | 'text'
  }

}