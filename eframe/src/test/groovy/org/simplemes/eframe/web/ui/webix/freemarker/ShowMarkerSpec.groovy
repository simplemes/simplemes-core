/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockDomainUtils
import org.simplemes.eframe.test.MockFieldDefinitions
import sample.controller.SampleParentController
import sample.domain.SampleParent

/**
 * Tests.
 */
class ShowMarkerSpec extends BaseMarkerSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [EXTENSION_MOCK]

  def "verify that apply generates the specified fields as readOnly fields"() {
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

    and: 'the dialog preferences are loaded'
    page.contains('ef.loadDialogPreferences();')
  }

  def "verify that the create toolbar is generated"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efShow fields="name,title" title@id="customID"/>
      </@efForm>
    """

    def sampleParent = new SampleParent(name: 'ABC', title: 'xyz', uuid: UUID.randomUUID())
    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: sampleParent,
                       uri: "/sampleParent/show/${sampleParent.uuid}")

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the correct standard show toolbar list button is generated'
    def listButtonText = TextUtils.findLine(page, 'id: "showList"')
    JavascriptTestUtils.extractProperty(listButtonText, 'view') == 'button'
    JavascriptTestUtils.extractProperty(listButtonText, 'type') == 'htmlbutton'
    JavascriptTestUtils.extractProperty(listButtonText, 'label').contains(lookup('list.menu.label'))
    JavascriptTestUtils.extractProperty(listButtonText, 'icon') == 'fas fa-th-list'
    JavascriptTestUtils.extractProperty(listButtonText, 'tooltip') == lookup('list.menu.tooltip')
    JavascriptTestUtils.extractProperty(listButtonText, 'click') == "window.location='/sampleParent'"

    and: 'the correct standard show toolbar create button is generated'
    def createButtonText = TextUtils.findLine(page, 'id: "showCreate"')
    JavascriptTestUtils.extractProperty(createButtonText, 'view') == 'button'
    JavascriptTestUtils.extractProperty(createButtonText, 'type') == 'htmlbutton'
    JavascriptTestUtils.extractProperty(createButtonText, 'label').contains(lookup('create.menu.label'))
    JavascriptTestUtils.extractProperty(createButtonText, 'icon') == 'fas fa-plus-square'
    JavascriptTestUtils.extractProperty(createButtonText, 'tooltip') == lookup('create.menu.tooltip')
    JavascriptTestUtils.extractProperty(createButtonText, 'click') == "window.location='/sampleParent/create'"

    and: 'the correct standard show toolbar edit button is generated'
    def editButtonText = TextUtils.findLine(page, 'id: "showEdit"')
    JavascriptTestUtils.extractProperty(editButtonText, 'view') == 'button'
    JavascriptTestUtils.extractProperty(editButtonText, 'type') == 'htmlbutton'
    JavascriptTestUtils.extractProperty(editButtonText, 'label').contains(lookup('edit.menu.label'))
    JavascriptTestUtils.extractProperty(editButtonText, 'icon') == 'fas fa-edit'
    JavascriptTestUtils.extractProperty(editButtonText, 'tooltip') == lookup('edit.menu.tooltip')
    JavascriptTestUtils.extractProperty(editButtonText, 'click') == "window.location='/sampleParent/edit/$sampleParent.uuid'"

    and: 'the correct standard show toolbar more..delete button is generated'
    def moreButtonText = TextUtils.findLine(page, 'view: "menu"')
    JavascriptTestUtils.extractProperty(moreButtonText, 'id') == 'showMore'

    def deleteMenuText = TextUtils.findLine(page, 'id: "showDelete"')
    JavascriptTestUtils.extractProperty(deleteMenuText, 'value') == lookup('delete.menu.label')
    JavascriptTestUtils.extractProperty(deleteMenuText, 'tooltip') == lookup('delete.menu.tooltip')

    and: 'the click handler is correct'
    def clickHandlerText = TextUtils.findLine(page, 'if (id=="showDelete")')
    clickHandlerText.contains("efd._confirmDelete('/sampleParent/delete','$sampleParent.uuid','SampleParent','ABC')")
  }

  def "verify that the record short string is HTML escaped in delete click handler"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efShow fields="name,title"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: '<script>ABC</script>', title: 'xyz'), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the delete record function call uses the HTML escaped name field'
    def clickHandlerText = TextUtils.findLine(page, 'if (id=="showDelete")')
    clickHandlerText.contains("'&lt;script&gt;ABC&lt;/script&gt;'")
    !page.contains('<script>ABC</script>')
  }

  def "verify that the pre-loaded strings are correct for the delete dialog"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efShow fields="name,title"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: '<script>ABC</script>', title: 'xyz'), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the delete record function call uses the HTML escaped name field'
    def preloadText = JavascriptTestUtils.extractBlock(page, 'eframe._addPreloadedMessages([')
    preloadText.contains("""{"ok.label": "${lookup('ok.label')}"}""")
    preloadText.contains("""{"cancel.label": "${lookup('cancel.label')}"}""")
    def msg1 = JavascriptUtils.escapeForJavascript(lookup('delete.confirm.message'))
    preloadText.contains("""{"delete.confirm.message": "${msg1}"}""")
    def msg2 = JavascriptUtils.escapeForJavascript(lookup('delete.confirm.title'))
    preloadText.contains("""{"delete.confirm.title": "${msg2}"}""")
  }

  def "verify that custom fields are supported"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()
    mockFieldExtension(domainClass: SampleParent, fieldName: 'custom1', afterFieldName: 'title')

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efShow fields="name,title"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'custom field is created in the right place'
    page.indexOf('id: "title"') < page.indexOf('id: "custom1"')
  }

  def "verify that the marker supports an efMenuItem for content - after placement"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efShow fields="name,title" $menuPlacementAttribute>
          <@efMenuItem id="release" key="release" onClick="release()"/>
        </@efShow>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC', title: 'xyz'), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the correct standard show toolbar button is generated'
    def loc = page.indexOf('id: "showToolbar"')
    def secondToolbar = page[loc..-1]
    def elementsText = JavascriptTestUtils.extractBlock(secondToolbar, 'elements: [')

    def releaseButtonText = TextUtils.findLine(elementsText, 'id: "release"')
    JavascriptTestUtils.extractProperty(releaseButtonText, 'view') == 'button'
    JavascriptTestUtils.extractProperty(releaseButtonText, 'label') == 'release'
    JavascriptTestUtils.extractProperty(releaseButtonText, 'tooltip') == 'release.tooltip'
    JavascriptTestUtils.extractProperty(releaseButtonText, 'click') == "release()"

    where:
    menuPlacementAttribute  | _
    'menuPlacement="after"' | _
    ''                      | _
  }

  def "verify that the marker supports an efMenuItem for content - more menu placement"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm id="show">
        <@efShow fields="name,title" menuPlacement="more">
          <@efMenuItem id="release" key="release" onClick="release()"/>
        </@efShow>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC', title: 'xyz'), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the correct standard show toolbar more.. menu is generated with the added menu item'
    def moreButtonText = TextUtils.findLine(page, 'view: "menu"')
    JavascriptTestUtils.extractProperty(moreButtonText, 'id') == 'showMore'

    def releaseMenuText = TextUtils.findLine(page, 'id: "release"')
    JavascriptTestUtils.extractProperty(releaseMenuText, 'value') == 'release'
    JavascriptTestUtils.extractProperty(releaseMenuText, 'tooltip') == 'release.tooltip'
  }

}
