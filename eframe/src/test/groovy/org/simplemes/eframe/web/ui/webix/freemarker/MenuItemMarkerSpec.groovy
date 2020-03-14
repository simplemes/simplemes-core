/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockDomainUtils
import org.simplemes.eframe.test.MockFieldDefinitions
import org.simplemes.eframe.test.UnitTestUtils
import sample.controller.SampleParentController
import sample.domain.SampleParent

/**
 * Tests.
 */
class MenuItemMarkerSpec extends BaseMarkerSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [EXTENSION_MOCK]

  def "verify that the marker builds a separator when called with no attributes"() {
    when: 'the marker is built'
    def src = """ 
      <@efForm>
        <@efMenu>
          <@efMenuItem/>
        </@efMenu>
      </@efForm>
    """
    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage("[$page]")

    and: 'separator is generated'
    def dataText = JavascriptTestUtils.extractBlock(page, 'data:[')
    JavascriptTestUtils.extractProperty(dataText, '$template').contains("Separator")
  }

  def "verify that the marker works inside of a menu"() {
    when: 'the marker is built'
    def src = """
      <@efForm>
        <@efMenu>
          <@efMenuItem key="order"/>
        </@efMenu>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage("[$page]")

    and: 'the menu is created'
    def dataText = JavascriptTestUtils.extractBlock(page, 'data:[')
    JavascriptTestUtils.extractProperty(dataText, 'id').contains("order")
    JavascriptTestUtils.extractProperty(dataText, 'value') == lookup('order.label')
    JavascriptTestUtils.extractProperty(dataText, 'tooltip') == lookup('order.tooltip')
  }

  def "verify that the marker supports ID passed in"() {
    when: 'the marker is built'
    def src = """
      <@efForm>
        <@efMenu>
          <@efMenuItem id="order"/>
        </@efMenu>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage("[$page]")

    and: 'the id used for the menu item'
    def dataText = JavascriptTestUtils.extractBlock(page, 'data:[')
    JavascriptTestUtils.extractProperty(dataText, 'id').contains("order")
    JavascriptTestUtils.extractProperty(dataText, 'value').contains(lookup('order.label'))
    JavascriptTestUtils.extractProperty(dataText, 'tooltip').contains(lookup('order.tooltip'))
  }

  def "verify that the marker supports label attribute"() {
    when: 'the marker is built'
    def src = """
      <@efForm>
        <@efMenu>
          <@efMenuItem id="order" label="moreNotes"/>
        </@efMenu>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage("[$page]")

    and: 'the label is used for the menu item'
    def dataText = JavascriptTestUtils.extractBlock(page, 'data:[')
    JavascriptTestUtils.extractProperty(dataText, 'value').contains(lookup('moreNotes.label'))
    JavascriptTestUtils.extractProperty(dataText, 'tooltip').contains(lookup('moreNotes.tooltip'))
  }

  def "verify that the marker supports label and tooltip attribute"() {
    when: 'the marker is built'
    def src = """
      <@efForm>
        <@efMenu>
          <@efMenuItem id="aMenu" label="moreNotes" tooltip="order"/>
        </@efMenu>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage("[$page]")

    and: 'the label and tooltip are correct'
    def dataText = JavascriptTestUtils.extractBlock(page, 'data:[')
    JavascriptTestUtils.extractProperty(dataText, 'value').contains(lookup('moreNotes.label'))
    JavascriptTestUtils.extractProperty(dataText, 'tooltip').contains(lookup('order.tooltip'))
  }

  def "verify that the marker supports uri attribute"() {
    when: 'the marker is built'
    def src = """
      <@efForm>
        <@efMenu>
          <@efMenuItem id="aMenu" uri="/controller/method"/>
        </@efMenu>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage("[$page]")

    and: 'the uri is in the actions list correctly'
    def onItemClickText = JavascriptTestUtils.extractBlock(page, 'onMenuItemClick: function (id) {')
    onItemClickText.contains("if (id=='aMenu') {")
    onItemClickText.contains('window.location="/controller/method"')
  }

  def "verify that the marker detects when not used inside of an efMenu or efShow"() {
    when: 'the marker is built'
    def src = """
      <@efForm>
        <@efMenu>
          <@efMenuItem id="order" label="moreNotes"/>
        </@efMenu>
      </@efForm>
      <@efMenuItem label="abc"/>
    """
    execute(source: src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['efMenuItem', 'efMenu', 'efShow'])
  }

  def "verify that the marker supports use inside of the showMarker"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm>
        <@efShow fields="name,title" menuPlacement="more">
          <@efMenuItem id="release" key="release" onClick="release()"/>
        </@efShow>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC', title: 'xyz'), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage("[$page]")

    and: 'the uri is in the actions list correctly'
    def subMenuText = JavascriptTestUtils.extractBlock(page, 'submenu: [')
    subMenuText.contains('id: "release"')
  }

  def "verify that the marker supports use as menu separator inside of the showMarker"() {
    given: 'a mocked FieldDefinitions for the domain'
    new MockDomainUtils(this, new MockFieldDefinitions(['name', 'title'])).install()

    when: 'the marker is built'
    def src = """
      <@efForm>
        <@efShow fields="name,title" menuPlacement="more">
          <@efMenuItem/>
        </@efShow>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController,
                       domainObject: new SampleParent(name: 'ABC', title: 'xyz'), uri: '/sampleParent/show/5')

    then: 'the javascript is legal'
    checkPage("[$page]")

    and: 'the uri is in the actions list correctly'
    def subMenuText = JavascriptTestUtils.extractBlock(page, 'submenu: [')
    subMenuText.contains('{$template: "Separator"}')
  }

}
