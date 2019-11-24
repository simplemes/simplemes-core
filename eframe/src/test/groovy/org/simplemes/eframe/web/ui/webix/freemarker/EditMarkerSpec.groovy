package org.simplemes.eframe.web.ui.webix.freemarker

import com.fasterxml.jackson.databind.ObjectMapper
import org.grails.datastore.mapping.validation.ValidationErrors
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.data.format.ChildListFieldFormat
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockDomainUtils
import org.simplemes.eframe.test.MockFieldDefinitions
import org.simplemes.eframe.test.MockPreferenceHolder
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.web.ui.webix.widget.TextFieldWidget
import org.springframework.validation.FieldError
import sample.controller.SampleParentController
import sample.domain.SampleChild
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class EditMarkerSpec extends BaseMarkerSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [EXTENSION_MOCK]

  def "verify that apply generates the specified fields as input fields"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efEdit fields="name"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC'))

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the HTML has the correct div for the definition element'
    page.contains('<div id="editContent"></div>')

    and: 'the webix.ui header section is correct'
    def webixBlock = JavascriptTestUtils.extractBlock(page, 'webix.ui')
    webixBlock.contains("container: 'editContent'")

    //        {view: "text", value: 'M2001', id: "order2Key", inputWidth: 150, attributes: {maxlength: 4>
    and: 'the key field is defined correctly'
    def nameFieldLine = TextUtils.findLine(page, 'id: "name"')
    JavascriptTestUtils.extractProperty(nameFieldLine, 'view') == "text"
    JavascriptTestUtils.extractProperty(nameFieldLine, 'value') == "ABC"
    def width = """tk.pw("${TextFieldWidget.adjustFieldCharacterWidth(40)}em")"""
    JavascriptTestUtils.extractProperty(nameFieldLine, 'inputWidth') == width

    def fieldAttributes = JavascriptTestUtils.extractBlock(nameFieldLine, 'attributes:')
    fieldAttributes.contains('maxlength: 40')

    and: 'the dialog preferences are loaded'
    page.contains('ef.loadDialogPreferences();')
  }

  def "verify that the standard toolbar and bottom button is generated"() {
    given: 'a mocked FieldDefinitions for the domain'
    def fieldDef = new SimpleFieldDefinition(name: 'name', maxLength: 40, type: String)
    new MockDomainUtils(this, new MockFieldDefinitions([fieldDef])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efEdit fields="name"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC', id: 237),
                       uri: '/sampleParent/edit/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the correct standard edit toolbar list button is generated'
    def listButtonText = TextUtils.findLine(page, 'id: "editList"')
    JavascriptTestUtils.extractProperty(listButtonText, 'view') == 'button'
    JavascriptTestUtils.extractProperty(listButtonText, 'type') == 'htmlbutton'
    JavascriptTestUtils.extractProperty(listButtonText, 'label').contains(lookup('list.menu.label'))
    JavascriptTestUtils.extractProperty(listButtonText, 'icon') == 'fas fa-th-list'
    JavascriptTestUtils.extractProperty(listButtonText, 'tooltip') == lookup('list.menu.tooltip')
    JavascriptTestUtils.extractProperty(listButtonText, 'click') == "window.location='/sampleParent'"

    and: 'the correct edit -save- toolbar button is generated'
    def editToolbarButtonText = TextUtils.findLine(page, 'id: "editSave"')
    JavascriptTestUtils.extractProperty(editToolbarButtonText, 'view') == 'button'
    JavascriptTestUtils.extractProperty(editToolbarButtonText, 'type') == 'icon'
    JavascriptTestUtils.extractProperty(editToolbarButtonText, 'label').contains(lookup('update.menu.label'))
    JavascriptTestUtils.extractProperty(editToolbarButtonText, 'icon') == 'fas fa-edit'
    JavascriptTestUtils.extractProperty(editToolbarButtonText, 'tooltip') == lookup('update.menu.tooltip')
    JavascriptTestUtils.extractProperty(editToolbarButtonText, 'click') == "editSave"

    and: 'the correct edit -save- button at the page bottom is generated'
    def editBottomText = TextUtils.findLine(page, 'id: "editSaveBottom"')
    JavascriptTestUtils.extractProperty(editBottomText, 'view') == 'button'
    JavascriptTestUtils.extractProperty(editBottomText, 'type') == 'iconButton'
    JavascriptTestUtils.extractProperty(editBottomText, 'label').contains(lookup('update.menu.label'))
    JavascriptTestUtils.extractProperty(editBottomText, 'icon') == 'fas fa-edit'
    JavascriptTestUtils.extractProperty(editBottomText, 'tooltip') == lookup('update.menu.tooltip')
    JavascriptTestUtils.extractProperty(editBottomText, 'click') == "editSave"

    and: 'the javascript function to handle the save click is correct'
    def functionText = JavascriptTestUtils.extractBlock(page, 'function editSave(')
    functionText.contains("efd._editSave('edit','/sampleParent/edit','237')")
  }

  def "verify that the text fields are escaped for use in javascript string"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efEdit fields="name"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: '<script>"ABC</script>'), uri: '/sampleParent/edit/5')

    // We can't check the javascript on the page since the extra quote causes problems in this scenario.
    // Leave it to the EditMarkerGUISpec to test this.
    //then: 'the javascript is legal'
    //checkPage(page)

    then: 'the value has the string escaped for use as a javascript string'
    def fieldLine = TextUtils.findLine(page, 'id: "name"')
    fieldLine.contains('value: "<script>\\"ABC<\\/script>"')
    !page.contains('abc</script>')
  }

  def "verify that the marker gracefully handles no fields to display"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efEdit fields=","/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC'), uri: '/sampleParent/edit/5')

    then: 'the javascript is legal'
    checkPage(page)
  }

  def "verify that the marker fails when used outside of the efForm marker"() {
    when: 'the marker is built'
    def src = """
        <@efEdit fields=","/>
    """

    execute(source: src, controllerClass: SampleParentController, domainObject: new SampleParent(name: 'ABC'))

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['efForm'])
  }

  def "verify that focus is placed in the first key field"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efEdit fields="name,title"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC'), uri: '/sampleParent/edit/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the focus is set to the key field'
    page.contains('$$("name").focus();')
  }

  def "verify that focus is placed in the first error field"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()

    and: 'a simulated domain record with an error on the title field'
    def errors = new ValidationErrors('dummy')
    errors.addError(new FieldError('sampleParent', 'title', 'bad'))
    def sampleParent = new SampleParent(name: 'ABC', title: 'xyz', errors: errors)

    when: 'the marker is built'
    def src = """
      <@efForm id="create">
        <@efEdit fields="name,title"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: sampleParent)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the focus is set to the key field'
    page.contains('$$("title").focus();')
  }

  def "verify that the field option readOnly is passed to the field correctly"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efEdit fields="name" name@readOnly="$value"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC'))

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the field is read-only'
    def nameFieldLine = TextUtils.findLine(page, 'id: "name"')

    nameFieldLine.contains('view: "label"') == result

    where:
    value   | result
    'true'  | true
    'True'  | true
    't'     | false
    'false' | false
    'False' | false
    ''      | false
    ' '     | false
  }

  static originalObjectMapper

  def "verify that the field option readOnly works on form with an inline grid"() {
    given: 'a mocked FieldDefinitions for the domain'
    def nameFieldDef = new SimpleFieldDefinition(name: 'name', maxLength: 40, type: String)
    def gridFieldDef = new SimpleFieldDefinition(name: 'sampleChildren', format: ChildListFieldFormat.instance, referenceType: SampleChild)
    new MockDomainUtils(this, [SampleChild], new MockFieldDefinitions([nameFieldDef, gridFieldDef])).install()

    and: 'a mocked preference holder'
    new MockPreferenceHolder(this, []).install()
    // new MockObjectMapper(this).install()

    and: 'the object mapper is mocked for the SampleParent record written as JSON to the generated page'
    originalObjectMapper = Holders.objectMapper
    def mockMapper = Mock(ObjectMapper)
    mockMapper.writeValueAsString(*_) >> '{ "sequence": 237}'
    Holders.objectMapper = mockMapper


    //def fieldDefinitions = new MockFieldDefinitions([name: String, sampleChildren: SampleChild])
    //new MockDomainUtils(this, fieldDefinitions).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efEdit fields="name,group:details,sampleChildren" name@readOnly="true"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC'))

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the field is read-only'
    def nameFieldLine = TextUtils.findLine(page, 'id: "name"')

    nameFieldLine.contains('view: "label"')

    cleanup:
    Holders.objectMapper = originalObjectMapper
  }

  def "verify that custom fields are supported"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()
    mockFieldExtension(domainClass: SampleParent, fieldName: 'custom1', afterFieldName: 'title')

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efEdit fields="name,title"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'custom field is created in the right place'
    page.indexOf('id: "title"') < page.indexOf('id: "custom1"')
  }

}
