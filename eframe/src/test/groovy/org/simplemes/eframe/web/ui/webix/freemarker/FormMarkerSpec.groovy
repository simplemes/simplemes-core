/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.data.FieldDefinitions
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.reports.ReportFieldDefinition
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockDomainUtils
import org.simplemes.eframe.test.MockFieldDefinitions
import org.simplemes.eframe.web.view.FreemarkerWrapper
import sample.controller.SampleParentController
import sample.domain.SampleParent

/**
 * Tests.
 */
class FormMarkerSpec extends BaseMarkerSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [EXTENSION_MOCK]

  def "verify that the form marker handles the simple case"() {
    when: 'the marker is built'
    def src = """
      <@efForm url="/theUrl">
        // Some content
      </@efForm>
    """
    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the body content is used'
    def content = JavascriptTestUtils.extractBlock(page, 'var _formFormData = [')
    content.contains('// Some content')

    and: 'the container is created correctly'
    def ui = JavascriptTestUtils.extractBlock(page, 'webix.ui({')
    JavascriptTestUtils.extractProperty(ui, 'container') == '_formContent'

    and: 'the view is correct'
    def view = JavascriptTestUtils.extractBlock(ui, '{view')
    JavascriptTestUtils.extractProperty(view, 'view') == 'form'
    JavascriptTestUtils.extractProperty(view, 'id') == '_form'
    JavascriptTestUtils.extractProperty(view, 'width') == "tk.pw('90%')"
  }

  def "verify that the form marker handles the supported options"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="dummy" width="12.4%" height="74.1%">
        // Some content
      </@efForm>
    """
    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the form ID can be passed in'
    page.contains('<div id="dummyContent">')

    and: 'the body content is used'
    def content = JavascriptTestUtils.extractBlock(page, 'var dummyFormData = [')
    content.contains('// Some content')

    and: 'the container is created correctly'
    def ui = JavascriptTestUtils.extractBlock(page, 'webix.ui({')
    JavascriptTestUtils.extractProperty(ui, 'container') == 'dummyContent'

    and: 'the view is correct'
    def view = JavascriptTestUtils.extractBlock(ui, '{view')
    JavascriptTestUtils.extractProperty(view, 'view') == 'form'
    JavascriptTestUtils.extractProperty(view, 'id') == 'dummy'
    JavascriptTestUtils.extractProperty(view, 'width') == "tk.pw('12.4%')"
    JavascriptTestUtils.extractProperty(view, 'height') == "tk.ph('74.1%')"
  }

  def "verify that the form marker works with the edit marker"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efEdit fields="name"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC'), uri: '/sampleParent/edit/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the form ID can be passed in'
    page.contains('<div id="editContent">')

    and: 'the form ID is passed to the nested marker'
    page.contains('var editFormData =')

    and: 'the nested marker can provide the toolbar code'
    def uiBlock = JavascriptTestUtils.extractBlock(page, 'webix.ui({')
    uiBlock.contains('editToolbar')

    and: 'the nested marker can provide the postScript code'
    def endBlock = page[(page.indexOf('webix.ui({'))..-1]
    endBlock.contains('$$("name").focus()')
  }

  def "verify that the form marker handles the dashboard activity case"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="XYZ" dashboard=true>
        // Some content
      </@efForm>
    """
    def page = execute(source: src, dataModel: [params: [_variable: '_ABC']])

    then: 'the javascript is legal'
    JavascriptTestUtils.checkScriptsOnPage(page)

    and: 'the HTML is not generated'
    !page.contains('<div')

    and: 'the webix initialization is not triggered.'
    !page.contains('webix.ui(')

    and: 'the correct variable is used'
    page.contains('_ABC.display = {')

    and: 'the form ID is used'
    page.contains("id: 'XYZ'")
  }

  def "verify that the form marker handles the dashboard activity case with field contents"() {
    when: 'the marker is built'
    def src = """
      <@efForm dashboard=true>
        <@efField field="title"/>
      </@efForm>
    """
    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC'), dataModel: [params: [_variable: '_A']])

    then: 'the javascript is legal'
    JavascriptTestUtils.checkScriptsOnPage(page)

    and: 'the field is in the output'
    page.contains('name: "title"')
  }

  def "verify that the form marker handles the dashboard false option"() {
    when: 'the marker is built'
    def src = """
      <@efForm dashboard=false>
        <@efField field="title"/>
      </@efForm>
    """
    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC'))

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the field created in non-dashboard mode'
    page.contains('<div')
  }

  def "verify that the form marker handles the dashboard buttonHolder case"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="XYZ" dashboard="buttonHolder">
        // Some content
      </@efForm>
    """
    def page = execute(source: src, dataModel: [params: [_panel: 'X', _variable: '_X']])

    then: 'the javascript is legal'
    JavascriptTestUtils.checkScriptsOnPage(page)

    and: 'grab the button holder view'
    def holderLine = TextUtils.findLine(page, 'id: "ButtonsX"')
    def holderText = JavascriptTestUtils.extractBlock(holderLine, '{view: "form"')
    JavascriptTestUtils.extractProperty(holderText, 'type') == 'clean'
    JavascriptTestUtils.extractProperty(holderText, 'borderless') == 'true'
    def elementsText = JavascriptTestUtils.extractBlock(holderLine, 'elements: [')
    JavascriptTestUtils.extractProperty(elementsText, 'view').contains('template')
    JavascriptTestUtils.extractProperty(elementsText, 'id').contains('ButtonsContentX')
  }

  def "verify that the form marker supports the fieldDefinitions option"() {
    given: 'a set of field definitions'
    def fieldDefinitions = new FieldDefinitions()
    fieldDefinitions << new ReportFieldDefinition(name: 'abc', sequence: 20)
    fieldDefinitions << new ReportFieldDefinition(name: 'xyz', sequence: 10)

    when: 'the marker is built'
    def src = """
      <@efForm fieldDefinitions="reportFields">
        // Some content
      </@efForm>
    """
    def page = execute(source: src, dataModel: [reportFields      : new FreemarkerWrapper(fieldDefinitions),
                                                reportFilterValues: [:]])

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the content contains the specified fields'
    page.contains('abc')
    page.contains('xyz')
  }

}
