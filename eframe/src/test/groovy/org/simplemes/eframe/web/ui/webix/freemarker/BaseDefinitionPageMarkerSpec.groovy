/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockDomainUtils
import org.simplemes.eframe.test.MockFieldDefinitions
import org.simplemes.eframe.test.UnitTestUtils
import sample.controller.AllFieldsDomainController
import sample.controller.SampleParentController
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

/**
 * Tests.
 */
class BaseDefinitionPageMarkerSpec extends BaseMarkerSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [EXTENSION_MOCK]

  def "verify that execute generates the specified fields"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efShow fields="name,title"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC', title: 'xyz'), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the HTML has the correct div for the definition element'
    page.contains('<div id="showContent"></div>')

    and: 'the webix.ui header section is correct'
    def webixBlock = JavascriptTestUtils.extractBlock(page, 'webix.ui')
    webixBlock.contains("container: 'showContent'")

    and: 'the key field is created correctly'
    def nameFieldLine = TextUtils.findLine(page, 'id: "name"')
    JavascriptTestUtils.extractProperty(nameFieldLine, 'label') == "ABC"

    and: 'the key field uses the default labelWidth'
    def nameLabelFieldLine = TextUtils.findLine(page, 'id: "nameLabel"')
    nameLabelFieldLine.contains('width: tk.pw(ef.getPageOption(')

    and: 'the fields are created correctly'
    def titleFieldLine = TextUtils.findLine(page, 'id: "title"')
    JavascriptTestUtils.extractProperty(titleFieldLine, 'label') == "xyz"

    and: 'the URL is checked for messages'
    page.contains('efd._checkURLMessages();')
  }

  def "verify that execute flags required fields correctly - create"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['*name', 'title'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efCreate fields="name,title"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC', title: 'xyz'), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the key field is created correctly'
    def nameFieldLine = TextUtils.findLine(page, 'id: "name"')
    JavascriptTestUtils.extractProperty(nameFieldLine, 'required') == "true"
  }

  def "verify that execute generates the specified fields - tabbed panel scenario"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title', 'notes'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efCreate fields="group:main,name,notes,group:details,title"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC', title: 'xyz'), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the key field is created correctly'
    def nameFieldLine = TextUtils.findLine(page, 'id: "name"')
    JavascriptTestUtils.extractProperty(nameFieldLine, 'value') == "ABC"

    and: 'the key field uses the more centered labelWidth'
    def nameLabelFieldLine = TextUtils.findLine(page, 'id: "nameLabel"')
    nameLabelFieldLine.contains("width: tk.pw('30%')")

    and: 'the fields are created correctly'
    def titleFieldLine = TextUtils.findLine(page, 'id: "title"')
    JavascriptTestUtils.extractProperty(titleFieldLine, 'value') == "xyz"

    and: 'the main panel is defined correctly'
    def mainPanel = JavascriptTestUtils.extractBlock(page, "header: '${lookup('main.panel.label')}'")
    JavascriptTestUtils.extractProperty(mainPanel, 'id') == 'mainBody'
    mainPanel.contains('id: "notesLabel"')
    mainPanel.contains('id: "notes"')

    and: 'the details panel is defined correctly'
    def detailsPanel = JavascriptTestUtils.extractBlock(page, "header: '${lookup('details.panel.label')}'")
    JavascriptTestUtils.extractProperty(detailsPanel, 'id') == 'detailsBody'
    detailsPanel.contains('id: "titleLabel"')
    detailsPanel.contains('id: "title"')

    and: 'the footer section has the default tab display logic'
    page.contains('$$("theTabView").getTabbar().setValue(selectedTab)')
  }

  def "verify that non-localized panel name is shown as-is - no .panel.local"() {
    // This allows the custom label to be used as-is.  Not all custom panels can have .label entries in the
    // .properties files.
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title', 'notes'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efCreate fields="group:gibberish,name,notes,group:details,title"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC', title: 'xyz'), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the custom panel is defined correctly'
    def customPanel = JavascriptTestUtils.extractBlock(page, "header: 'gibberish'")
    JavascriptTestUtils.extractProperty(customPanel, 'id') == 'gibberishBody'
    customPanel.contains('id: "notesLabel"')
    customPanel.contains('id: "notes"')
  }

  def "verify that the marker supports fieldOrder from the domain class"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efShow/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: AllFieldsDomainController,
                       domainObject: new AllFieldsDomain(name: 'ABC', title: 'xyz'),
                       uri: '/allFieldsDomain/show/5')

    then: 'the javascript is legal'
    checkPage(page)

    //  static fieldOrder = ['name', 'title', 'qty', 'count', 'enabled', 'dueDate', 'dateTime', 'group:details', 'transientField']
    and: 'the correct fields are displayed in the correct order'
    page.indexOf('id: "name"') < page.indexOf('id: "title"')
    page.indexOf('id: "title"') < page.indexOf('id: "qty"')
    page.indexOf('id: "qty"') < page.indexOf('id: "count"')
    page.indexOf('id: "count"') < page.indexOf('id: "enabled"')
    page.indexOf('id: "enabled"') < page.indexOf('id: "dueDate"')
    page.indexOf('id: "dueDate"') < page.indexOf('id: "dateTime"')
    page.indexOf('id: "dateTime"') < page.indexOf('id: "transientField"')
  }

  def "verify that the marker gracefully detects a field not in the field definitions"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efShow fields="gibberish"/>
      </@efForm>
    """

    execute(source: src, controllerClass: AllFieldsDomainController,
            domainObject: new AllFieldsDomain(name: 'ABC', title: 'xyz'),
            uri: '/allFieldsDomain/show/5')

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['gibberish', 'AllFieldsDomain', 'field', 'efShow'])
  }

  def "verify that field-specific options are passed to the generated field widget"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efShow fields="name,title" title@id="customID"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC', title: 'xyz'), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the field uses the custom HTML ID'
    page.contains('id: "customIDLabel"')
    page.contains('id: "customID"')
  }

  def "verify that the marker supports the labelWidth option"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efShow fields="name,title" labelWidth="35%"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC', title: 'xyz'), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'label width is used for the fields'
    def titleFieldLine = TextUtils.findLine(page, 'id: "titleLabel"')
    JavascriptTestUtils.extractProperty(titleFieldLine, 'width').contains('35%')
  }

  def "verify that the marker gracefully handles no fields to display"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efShow fields=","/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC', title: 'xyz'), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage(page)
  }

  def "verify that execute marks field with error and set focus there"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()

    and: 'a simulated domain record'
    //def errors = new ValidationErrors('dummy')
    //errors.addError(new FieldError('sampleParent', 'title', 'bad'))
    def sampleParent = new SampleParent(name: 'ABC', title: 'xyz')

    when: 'the marker is built with an error in the model'
    def src = """
      <@efForm id="create">
        <@efEdit fields="name,title"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: sampleParent, errors: [new ValidationError(1, 'title')])

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the title field is flagged as an error'
    def titleFieldLine = TextUtils.findLine(page, 'id: "title"')
    JavascriptTestUtils.extractProperty(titleFieldLine, 'css') == "webix_invalid"
  }

  def "verify that the marker supports custom fields and fieldOrder"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, AllFieldsDomain).install()

    and: 'a mocked custom field for the domain'
    mockFieldExtension([domainClass   : AllFieldsDomain, fieldName: 'custom1', label: 'Custom Label',
                        afterFieldName: 'title'])

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efShow/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: AllFieldsDomainController,
                       domainObject: new AllFieldsDomain(name: 'ABC', title: 'xyz'),
                       uri: '/allFieldsDomain/show/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the correct fields are displayed in the correct order'
    page.indexOf('id: "title"') < page.indexOf('id: "custom1"')

    and: 'the custom label is correct'
    def customFieldLine = TextUtils.findLine(page, 'id: "custom1Label"')
    JavascriptTestUtils.extractProperty(customFieldLine, 'label') == "Custom Label"
  }

}
