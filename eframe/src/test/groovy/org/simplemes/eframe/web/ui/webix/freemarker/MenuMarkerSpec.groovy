/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.UnitTestUtils

/**
 * Tests.
 */
class MenuMarkerSpec extends BaseMarkerSpecification {

  def "verify that the marker supports menu item content in a single level menu"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="addPanel" dashboard="true">
        <@efMenu id="configMenu">
          <@efMenuItem key="release" onClick="someMethod()"/>
        </@efMenu>
      </@efForm>
    """

    def page = execute(source: src, dataModel: [params: [_variable: 'A']])
    //println "page = $page"

    then: 'the javascript is legal'
    checkPage(page)

    then: 'the top-level menu is defined'
    page.contains('view: "menu"')
    def viewText = TextUtils.findLine(page, 'view: "menu"')
    JavascriptTestUtils.extractProperty(viewText, 'openAction') == "click"
    JavascriptTestUtils.extractProperty(viewText, 'autowidth') == "true"
    JavascriptTestUtils.extractProperty(viewText, 'subsign') == "true"

    and: 'the tooltip is generated correctly'
    def subMenuConfigText = JavascriptTestUtils.extractBlock(page, 'submenuConfig: {')
    subMenuConfigText.contains('tooltip: function')
    subMenuConfigText.contains('return item.tooltip')

    and: 'the click handler is correct'
    page.contains("var configMenuActions = {};")
    def onItemClickText = JavascriptTestUtils.extractBlock(page, 'onMenuItemClick: function (id) {')
    onItemClickText.contains('var s = configMenuActions[id];')
    onItemClickText.contains('eval(s)')

    and: 'the menu items are defined'
    def dataText = JavascriptTestUtils.extractBlock(page, 'data:[')
    dataText.contains('id: "release"')
    JavascriptTestUtils.extractProperty(dataText, 'value') == "release"
    JavascriptTestUtils.extractProperty(dataText, 'tooltip') == "release.tooltip"
    page.contains("configMenuActions.release='someMethod()';")
  }

  def "verify that the marker generates a nested menu"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="addPanel" dashboard="true">
        <@efMenu>
          <@efMenu label="definitionEditorMenu">
            <@efMenuItem key="definitionEditorMenu.addCustomField" onClick="efd._editorFieldOpenAddDialog()"/>
          </@efMenu>
        </@efMenu>
      </@efForm>
    """

    def page = execute(source: src, dataModel: [params: [_variable: 'A']])

    then: 'the javascript is legal'
    checkPage(page)

    and: 'there is only one top-level element - toolbar'
    (page =~ /view: "menu"/).count == 1

    and: 'the sub-menu is listed'
    def subMenuText = JavascriptTestUtils.extractBlock(page, 'submenu: [')
    JavascriptTestUtils.extractProperty(subMenuText, 'id') == "definitionEditorMenu_addCustomField"
    JavascriptTestUtils.extractProperty(subMenuText, 'value') == lookup("definitionEditorMenu.addCustomField.label")
  }

  def "verify that the action array works for nested menu"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="addPanel" dashboard="true">
        <@efMenu id="configMenu">
          <@efMenu label="definitionEditorMenu">
            <@efMenuItem key="xyz" onClick="xyz()"/>
            <@efMenuItem key="abc" onClick="abc()"/>
            <@efMenuItem key="pdq" onClick="pdq()"/>
          </@efMenu>
        </@efMenu>
      </@efForm>
    """

    def page = execute(source: src, dataModel: [params: [_variable: 'A']])

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the action array is set correctly'
    page.contains("var configMenuActions = {};")
    TextUtils.findLine(page, 'xyz()').contains("configMenuActions.xyz='xyz()';")
    TextUtils.findLine(page, 'abc()').contains("configMenuActions.abc='abc()';")
    TextUtils.findLine(page, 'pdq()').contains("configMenuActions.pdq='pdq()';")
  }

  def "verify that the key can be used for label lookups on subMenus"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="addPanel" dashboard="true">
        <@efMenu id="configMenu">
          <@efMenu id="aSubMenu" key="order">
          </@efMenu>
        </@efMenu>
      </@efForm>
    """

    def page = execute(source: src, dataModel: [params: [_variable: 'A']])

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the label is correct for the menu'
    def subMenuText = TextUtils.findLine(page, 'id: "aSubMenu"')
    JavascriptTestUtils.extractProperty(subMenuText, 'value') == lookup('order.label')
  }

  def "verify that the label attribute can be used for label lookups  on subMenus"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="addPanel" dashboard="true">
        <@efMenu id="configMenu">
          <@efMenu id="aSubMenu" label="order.label">
          </@efMenu>
        </@efMenu>
      </@efForm>
    """

    def page = execute(source: src, dataModel: [params: [_variable: 'A']])

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the label is correct for the menu'
    def subMenuText = TextUtils.findLine(page, 'id: "aSubMenu"')
    JavascriptTestUtils.extractProperty(subMenuText, 'value') == lookup('order.label')
  }

  def "verify that the marker detects when not used inside of an efForm"() {
    when: 'the marker is built'
    def src = """
      <@efMenu label="abc"/>
    """
    execute(source: src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['efMenu', 'efForm'])
  }

}
