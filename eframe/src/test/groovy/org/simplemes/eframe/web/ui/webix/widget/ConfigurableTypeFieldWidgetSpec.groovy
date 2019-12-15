package org.simplemes.eframe.web.ui.webix.widget


import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.format.ConfigurableTypeDomainFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.JavascriptTestUtils
import sample.domain.RMA

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ConfigurableTypeFieldWidgetSpec extends BaseWidgetSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [JSON, SERVER]

  //TODO: Find alternative to @Rollback
  def "verify that the combobox is generated correctly - a value - editable case"() {
    given: 'a flex type with multiple fields'
    def flexType = DataGenerator.buildFlexType()

    and: 'a domain object for the value'
    def rma = new RMA(rmaType: flexType)

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(domainObject: rma, name: 'rmaType',
                                           format: ConfigurableTypeDomainFormat.instance,
                                           type: FlexType, referenceType: FlexType)
    def page = new ConfigurableTypeFieldWidget(widgetContext).build().toString()
    //println "page = $page"

    then: 'the page fragment is valid'
    JavascriptTestUtils.checkScriptFragment("[$page]")

    and: 'the combo box is used'
    def fieldLine = TextUtils.findLine(page, 'id: "rmaType"')
    JavascriptTestUtils.extractProperty(fieldLine, 'view') == 'combo'

    and: 'the valid values are correct'
    def optionsBlock = JavascriptTestUtils.extractBlock(page, 'options: [')
    optionsBlock.contains('FLEX1')

    and: 'the input width is the minimum width'
    def width = TextFieldWidget.adjustFieldCharacterWidth(ComboboxWidget.MINIMUM_WIDTH)
    JavascriptTestUtils.extractProperty(fieldLine, 'inputWidth') == """tk.pw("${width}em")"""

    and: 'the holder area has the default input field'
    def holder = JavascriptTestUtils.extractBlock(page, 'rows: [')
    def content = JavascriptTestUtils.extractBlock(holder, 'id: "rmaTypeContent",rows: [')
    def field1Line = TextUtils.findLine(content, 'id: "rmaType_FIELD1"')
    JavascriptTestUtils.extractProperty(field1Line, 'view') == 'text'

    and: 'the value for the custom field is empty'
    JavascriptTestUtils.extractProperty(field1Line, 'value') == ''
  }

  //TODO: Find alternative to @Rollback
  def "verify that the widget creates the list of possible choices correctly"() {
    given: 'a flex type with multiple fields'
    def flexType1 = DataGenerator.buildFlexType(flexType: 'FLEX1', fieldName: 'FIELD1')
    def flexType2 = DataGenerator.buildFlexType(flexType: 'FLEX2', fieldName: 'FIELD2')
    def flexType3 = DataGenerator.buildFlexType(flexType: 'FLEX3', fieldName: 'FIELD3')

    and: 'a domain object for the value'
    def rma = new RMA(rmaType: flexType1)

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(domainObject: rma, name: 'rmaType',
                                           format: ConfigurableTypeDomainFormat.instance,
                                           type: FlexType, referenceType: FlexType)
    new ConfigurableTypeFieldWidget(widgetContext).build()
    def post = widgetContext.markerCoordinator.postscript
    //println "post = $post"

    then: 'the page fragment is valid'
    JavascriptTestUtils.checkScriptsOnPage(post)

    and: 'the list has the right number in it'
    post.contains("""rmaTypeChoices["$flexType1.id"]""")
    post.contains("""rmaTypeChoices["$flexType2.id"]""")
    post.contains("""rmaTypeChoices["$flexType3.id"]""")
  }

  //TODO: Find alternative to @Rollback
  def "verify that the change handler on the main ct field"() {
    given: 'a flex type with multiple fields'
    def flexType1 = DataGenerator.buildFlexType(flexType: 'FLEX1', fieldName: 'FIELD1')

    and: 'a domain object for the value'
    def rma = new RMA(rmaType: flexType1)

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(domainObject: rma, name: 'rmaType',
                                           format: ConfigurableTypeDomainFormat.instance,
                                           type: FlexType, referenceType: FlexType)
    new ConfigurableTypeFieldWidget(widgetContext).build()
    def post = widgetContext.markerCoordinator.postscript
    //println "post = $post"

    then: 'the page fragment is valid'
    JavascriptTestUtils.checkScriptsOnPage(post)

    and: 'the change handler is correct'
    def handler = JavascriptTestUtils.extractBlock(post, '$$("rmaType").attachEvent("onChange", function(newValue, oldValue) {')
    //println "handler = $handler"
    handler.contains('$$("rmaTypeHolder").removeView("rmaTypeContent");')
    handler.contains('$$("rmaTypeHolder").addView(choice, 0);')
  }

  //TODO: Find alternative to @Rollback
  def "verify that the field format for input fields is correct"() {
    given: 'a flex type with multiple fields'
    def flexType1 = DataGenerator.buildFlexType(fieldFormat: DateOnlyFieldFormat.instance)

    and: 'a domain object for the value'
    def rma = new RMA(rmaType: flexType1)

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(domainObject: rma, name: 'rmaType',
                                           format: ConfigurableTypeDomainFormat.instance,
                                           type: FlexType, referenceType: FlexType)
    def page = new ConfigurableTypeFieldWidget(widgetContext).build().toString()
    //println "page = $page"

    then: 'the page fragment is valid'
    JavascriptTestUtils.checkScriptFragment("[$page]")

    and: 'the field format is used to build the configurable input fields'
    def fieldLine = TextUtils.findLine(page, 'id: "rmaType_FIELD1"')
    JavascriptTestUtils.extractProperty(fieldLine, 'view') == 'datepicker'
  }

  //TODO: Find alternative to @Rollback
  def "verify that the widget handles the readOnly mode"() {
    given: 'a flex type with multiple fields'
    def flexType1 = DataGenerator.buildFlexType(fieldFormat: DateOnlyFieldFormat.instance)

    and: 'a domain object for the value'
    def rma = new RMA(rmaType: flexType1)

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(domainObject: rma, name: 'rmaType', readOnly: true,
                                           format: ConfigurableTypeDomainFormat.instance,
                                           type: FlexType, referenceType: FlexType)
    def page = new ConfigurableTypeFieldWidget(widgetContext).build().toString()
    //println "page = $page"

    then: 'the page fragment is valid'
    JavascriptTestUtils.checkScriptFragment("[$page]")

    and: 'the main choice field is readOnly'
    def mainFieldLine = TextUtils.findLine(page, 'id: "rmaType"')
    JavascriptTestUtils.extractProperty(mainFieldLine, 'view') == 'label'

    and: 'the configurable field is readOnly'
    def fieldLine = TextUtils.findLine(page, 'id: "rmaType_FIELD1"')
    JavascriptTestUtils.extractProperty(fieldLine, 'view') == 'label'
  }

  //TODO: Find alternative to @Rollback
  def "verify that the widget handles quotes in the custom field value"() {
    given: 'a flex type with multiple fields'
    def flexType1 = DataGenerator.buildFlexType()

    and: 'a domain object for the value'
    def rma = new RMA(rmaType: flexType1)
    rma.setRmaTypeValue('FIELD1', 'abc"123')

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(domainObject: rma, name: 'rmaType',
                                           format: ConfigurableTypeDomainFormat.instance,
                                           type: FlexType, referenceType: FlexType)
    def page = new ConfigurableTypeFieldWidget(widgetContext).build().toString()
    //println "page = $page"

    then: 'the page fragment is valid'
    JavascriptTestUtils.checkScriptFragment("[$page]")

    and: 'the configurable field value is correct and has an escaped quote'
    def fieldLine = TextUtils.findLine(page, 'id: "rmaType_FIELD1"')
    fieldLine.contains('value: "abc\\"123"')
  }

  //TODO: Find alternative to @Rollback
  def "verify that the widget sets the default Configurable Type field setting"() {
    given: 'a flex type with multiple fields'
    DataGenerator.buildFlexType(defaultFlexType: true)

    and: 'a domain object for the value'
    def rma = new RMA()

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(domainObject: rma, name: 'rmaType',
                                           format: ConfigurableTypeDomainFormat.instance,
                                           type: FlexType, referenceType: FlexType)
    def page = new ConfigurableTypeFieldWidget(widgetContext).build().toString()
    //println "page = $page"

    then: 'the page fragment is valid'
    JavascriptTestUtils.checkScriptFragment("[$page]")

    and: 'default flex type is selected'
    def field1Line = TextUtils.findLine(page, 'id: "rmaType_FIELD1"')
    JavascriptTestUtils.extractProperty(field1Line, 'view') == 'text'
  }

  //TODO: Find alternative to @Rollback
  def "verify that the widget handles no current value for the Configurable Type drop-down"() {
    given: 'a flex type with multiple fields'
    DataGenerator.buildFlexType()

    and: 'a domain object for the value'
    def rma = new RMA()

    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(domainObject: rma, name: 'rmaType',
                                           format: ConfigurableTypeDomainFormat.instance,
                                           type: FlexType, referenceType: FlexType)
    def page = new ConfigurableTypeFieldWidget(widgetContext).build().toString()
    //println "page = $page"

    then: 'the page fragment is valid'
    JavascriptTestUtils.checkScriptFragment("[$page]")


    and: 'the default note is displayed'
    def noteLine = TextUtils.findLine(page, 'id: "rmaTypeNote"')
    JavascriptTestUtils.extractProperty(noteLine, 'label') == lookup('selectConfigType.label', lookup("rmaType.label"))
  }

}
