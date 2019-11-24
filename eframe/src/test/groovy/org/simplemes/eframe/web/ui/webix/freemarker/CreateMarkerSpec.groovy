package org.simplemes.eframe.web.ui.webix.freemarker

import org.grails.datastore.mapping.validation.ValidationErrors
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockDomainUtils
import org.simplemes.eframe.test.MockFieldDefinitions
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.web.ui.webix.widget.TextFieldWidget
import org.springframework.validation.FieldError
import sample.controller.SampleParentController
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class CreateMarkerSpec extends BaseMarkerSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [EXTENSION_MOCK]

  def "verify that apply generates the specified fields as input fields"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="createXYZ">
        <@efCreate fields="name"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC'))

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the HTML has the correct div for the definition element'
    page.contains('<div id="createXYZContent"></div>')

    and: 'the webix.ui header section is correct'
    def webixBlock = JavascriptTestUtils.extractBlock(page, 'webix.ui')
    webixBlock.contains("container: 'createXYZContent'")

    and: 'the key field is created correctly'
    def nameFieldLine = TextUtils.findLine(page, 'id: "name"')
    JavascriptTestUtils.extractProperty(nameFieldLine, 'view') == "text"
    JavascriptTestUtils.extractProperty(nameFieldLine, 'value') == "ABC"
    def width = """tk.pw("${TextFieldWidget.adjustFieldCharacterWidth(40)}em")"""
    JavascriptTestUtils.extractProperty(nameFieldLine, 'inputWidth') == width

    def fieldAttributes = JavascriptTestUtils.extractBlock(nameFieldLine, 'attributes:')
    fieldAttributes.contains('maxlength: 40')
  }

  def "verify that the standard toolbar and bottom button is generated"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="create">
        <@efCreate fields="name"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC'))

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the correct standard create toolbar list button is generated'
    def listButtonText = TextUtils.findLine(page, 'id: "createList"')
    JavascriptTestUtils.extractProperty(listButtonText, 'view') == 'button'
    JavascriptTestUtils.extractProperty(listButtonText, 'type') == 'htmlbutton'
    JavascriptTestUtils.extractProperty(listButtonText, 'label').contains(lookup('list.menu.label'))
    JavascriptTestUtils.extractProperty(listButtonText, 'icon') == 'fas fa-th-list'
    JavascriptTestUtils.extractProperty(listButtonText, 'tooltip') == lookup('list.menu.tooltip')
    JavascriptTestUtils.extractProperty(listButtonText, 'click') == "window.location='/sampleParent'"

    and: 'the correct create -save- toolbar button is generated'
    def createToolbarButtonText = TextUtils.findLine(page, 'id: "createSave"')
    JavascriptTestUtils.extractProperty(createToolbarButtonText, 'view') == 'button'
    JavascriptTestUtils.extractProperty(createToolbarButtonText, 'type') == 'icon'
    JavascriptTestUtils.extractProperty(createToolbarButtonText, 'label').contains(lookup('create.menu.label'))
    JavascriptTestUtils.extractProperty(createToolbarButtonText, 'icon') == 'fas fa-plus-square'
    JavascriptTestUtils.extractProperty(createToolbarButtonText, 'tooltip') == lookup('create.menu.tooltip')
    JavascriptTestUtils.extractProperty(createToolbarButtonText, 'click') == "createSave"

    and: 'the correct create -save- button at the page bottom is generated'
    def createBottomText = TextUtils.findLine(page, 'id: "createSaveBottom"')
    JavascriptTestUtils.extractProperty(createBottomText, 'view') == 'button'
    JavascriptTestUtils.extractProperty(createBottomText, 'type') == 'iconButton'
    JavascriptTestUtils.extractProperty(createBottomText, 'label').contains(lookup('create.menu.label'))
    JavascriptTestUtils.extractProperty(createBottomText, 'icon') == 'fas fa-plus-square'
    JavascriptTestUtils.extractProperty(createBottomText, 'tooltip') == lookup('create.menu.tooltip')
    JavascriptTestUtils.extractProperty(createBottomText, 'click') == "createSave"

    and: 'the javascript function to handle the save click is correct'
    def functionText = JavascriptTestUtils.extractBlock(page, 'function createSave(')
    functionText.contains("efd._createSave('create','/sampleParent/create')")
  }

  def "verify that the text fields are HTML escaped"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="create">
        <@efCreate fields="name"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: '<script>"ABC</script>'))

    // We can't check the javascript on the page since the extra quote causes problems in this scenario.
    // Leave it to the EditMarkerGUISpec to test this.
    //then: 'the javascript is legal'
    //checkPage(page)

    then: 'the value has the string escaped for use as a javascript string'
    def fieldLine = TextUtils.findLine(page, 'id: "name"')
    fieldLine.contains('value: "<script>\\"ABC<\\/script>"')
    !page.contains('ABC</script>')
  }

  def "verify that the marker gracefully handles no fields to display"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="create">
        <@efCreate fields=","/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC'))

    then: 'the javascript is legal'
    checkPage(page)
  }

  def "verify that the marker fails when used outside of the efForm marker"() {
    when: 'the marker is built'
    def src = """
        <@efCreate fields=","/>
    """

    execute(source: src, controllerClass: SampleParentController, domainObject: new SampleParent(name: 'ABC'))

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['efForm'])
  }

  def "verify that focus is placed in the first key field"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="create">
        <@efCreate fields="name"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC'))

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
        <@efCreate fields="name,title"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: sampleParent)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the focus is set to the key field'
    page.contains('$$("title").focus();')
  }

  def "verify that custom fields are supported"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()
    mockFieldExtension(domainClass: SampleParent, fieldName: 'custom1', afterFieldName: 'title')

    when: 'the marker is built'
    def src = """
      <@efForm id="create">
        <@efCreate fields="name,title"/>
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
