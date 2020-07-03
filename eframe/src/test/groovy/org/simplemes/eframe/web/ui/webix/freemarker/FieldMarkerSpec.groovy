/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockDomainUtils
import org.simplemes.eframe.test.MockFieldDefinitions
import org.simplemes.eframe.web.ui.webix.widget.TextFieldWidget
import sample.controller.AllFieldsDomainController
import sample.controller.SampleParentController
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

/**
 * Tests.
 */
class FieldMarkerSpec extends BaseMarkerSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [EXTENSION_MOCK]

  def "verify that the marker generates the field - basic scenario"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, [SampleParent], new MockFieldDefinitions(['title'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="SampleParent.title" value="ABC"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the HTML has the correct div for the definition element'
    page.contains('<div id="editContent"></div>')

    and: 'the webix.ui header section is correct'
    def webixBlock = JavascriptTestUtils.extractBlock(page, 'webix.ui')
    webixBlock.contains("container: 'editContent'")

    //        {view: "text", value: 'M2001', id: "order2Key", inputWidth: 150, attributes: {maxlength: 4}}
    and: 'the field is defined correctly'
    def titleFieldLine = TextUtils.findLine(page, 'id: "title"')
    JavascriptTestUtils.extractProperty(titleFieldLine, 'view') == "text"
    JavascriptTestUtils.extractProperty(titleFieldLine, 'value') == "ABC"
    def width = """tk.pw("${TextFieldWidget.adjustFieldCharacterWidth(40)}em")"""
    JavascriptTestUtils.extractProperty(titleFieldLine, 'inputWidth') == width

    def fieldAttributes = JavascriptTestUtils.extractBlock(titleFieldLine, 'attributes:')
    fieldAttributes.contains('maxlength: 40')
  }

  def "verify that the marker generates the field - label specified"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, [SampleParent]).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="SampleParent.title" label="email.label"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the field label is correct'
    def titleLabelLine = TextUtils.findLine(page, 'id: "titleLabel"')
    JavascriptTestUtils.extractProperty(titleLabelLine, 'label') == lookup('email.label')
  }

  def "verify that the marker generates the field - readOnly option"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, [SampleParent]).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="SampleParent.title" readOnly="true" value="ABC"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the value is a label'
    def fieldLine = TextUtils.findLine(page, 'id: "title"')
    JavascriptTestUtils.extractProperty(fieldLine, 'view') == "label"
    JavascriptTestUtils.extractProperty(fieldLine, 'id') == "title"
    JavascriptTestUtils.extractProperty(fieldLine, 'label') == "ABC"
  }

  def "verify that the marker generates the field - css option"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, [SampleParent]).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="SampleParent.title" css="aClass" readOnly=true />
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the CSS is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "title"')
    JavascriptTestUtils.extractProperty(fieldLine, 'css').contains("aClass")
  }

  def "verify that the marker generates the field - blank label specified"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, [SampleParent]).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="SampleParent.title" label=""/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the field label is nto generated'
    !page.contains('"titleLabel"')
  }

  def "verify that the marker supports the labelWidth option correctly"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, [SampleParent]).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="SampleParent.title" label="email.label" labelWidth='15%'/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the label width is correct'
    def viewLine = TextUtils.findLine(page, 'view: "label"')
    JavascriptTestUtils.extractProperty(viewLine, 'width') == "tk.pw('15%')"
  }

  def "verify that the marker body content"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, [SampleParent]).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="SampleParent.title" label="email.label" labelWidth='15%'>// The Body</@efField>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the body is in the output'
    page.contains('// The Body')
  }

  def "verify that the marker generates the field - password type"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, [SampleParent]).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="SampleParent.title" type="password"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the field type is correct'
    def titleFieldLine = TextUtils.findLine(page, 'id: "title"')
    JavascriptTestUtils.extractProperty(titleFieldLine, 'type') == 'password'
  }

  def "verify that the marker generates the field - width specified"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, [SampleParent]).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="SampleParent.title" width="20"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the field width is correct'
    def titleFieldLine = TextUtils.findLine(page, 'id: "title"')
    JavascriptTestUtils.extractProperty(titleFieldLine, 'inputWidth') == """tk.pw("20em")"""
  }

  def "verify that the marker generates the field - maxLength specified quoted"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, [SampleParent]).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="title" maxLength="23"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the field width is correct'
    def titleFieldLine = TextUtils.findLine(page, 'id: "title"')
    JavascriptTestUtils.extractProperty(titleFieldLine, 'attributes').contains('maxlength: 23')
  }

  def "verify that the marker generates the field - maxLength specified unquoted"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, [SampleParent]).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="title" maxLength=43/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the field width is correct'
    def titleFieldLine = TextUtils.findLine(page, 'id: "title"')
    JavascriptTestUtils.extractProperty(titleFieldLine, 'attributes').contains('maxlength: 43')
  }

  def "verify that the marker generates the field - after field specified with efEdit"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, [SampleParent]).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efEdit fields="name,notes"/>
        <@efField field="SampleParent.title" width="20" after="notes"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC'))

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the field type is correct'
    def titleFieldLine = TextUtils.findLine(page, 'id: "title"')
    JavascriptTestUtils.extractProperty(titleFieldLine, 'view') == "text"

    //tk._addRowToForm(createFormData,{margin: 8,cols:[{view: "label", label: "Confirm", align: "right", width: tk.pw(ef.getPageOption('labelWidth','20%'))},
    //{view: "text", id: "confirm", editable: true, inputWidth: 250}]},'email');
    and: 'the field is added using the dynamic add method'
    def addRowLine = TextUtils.findLine(page, 'tk._addRowToForm(')
    addRowLine.contains('editFormData')

    and: 'the field is added after the correct field'
    addRowLine.contains(',"notes")')
  }

  def "verify that the marker generates the field - non-domain case"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="title" value="ABC"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the field is defined correctly with the default maxLength'
    def titleFieldLine = TextUtils.findLine(page, 'id: "title"')
    JavascriptTestUtils.extractProperty(titleFieldLine, 'view') == "text"
    JavascriptTestUtils.extractProperty(titleFieldLine, 'value') == "ABC"
    def width = """tk.pw("${TextFieldWidget.adjustFieldCharacterWidth(40)}em")"""
    JavascriptTestUtils.extractProperty(titleFieldLine, 'inputWidth') == width

    def fieldAttributes = JavascriptTestUtils.extractBlock(titleFieldLine, 'attributes:')
    fieldAttributes.contains('maxlength: 40')
  }

  def "verify that the marker handles multiple fields without extra commas"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="title" value="ABC"/>
        <@efField field="name" value="ABC"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'no extra comma is inserted between the fields'
    !page.contains(',,')
  }

  def "verify that the marker generates the encoded type fields"() {
    given: 'a mocked domain'
    def fieldDef = new SimpleFieldDefinition(name: 'format', type: BasicFieldFormat,
                                             format: EncodedTypeFieldFormat.instance)
    new MockDomainUtils(this, [SampleParent], new MockFieldDefinitions([fieldDef])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="SampleParent.format"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the correct display value is used'
    def fieldLine = TextUtils.findLine(page, 'id: "format"')
    JavascriptTestUtils.extractProperty(fieldLine, 'view') == 'combo'

    and: 'the valid values are in the correct order'
    def optionsBlock = JavascriptTestUtils.extractBlock(page, 'options: [')
    optionsBlock.contains('id: "S"')
  }

  def "verify that the marker uses the value from a model if modelName is given"() {
    given: 'a mocked domain'
    new MockDomainUtils(this, [AllFieldsDomain], new MockFieldDefinitions(['qty'])).install()

    and: 'an object to use as the model'
    def afd = new AllFieldsDomain(qty: 1.2)

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="AllFieldsDomain.qty" modelName="theModel"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: AllFieldsDomainController, dataModel: [theModel: afd])

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the field is defined correctly'
    def qtyFieldLine = TextUtils.findLine(page, 'id: "qty"')
    JavascriptTestUtils.extractProperty(qtyFieldLine, 'value') == "1.2"
  }

  // test with efEdit but no after option
  // test required
  // test missing field
  // test not enclosed in efForm
  // test afterField with efEdit/efCreate/efShow
}
