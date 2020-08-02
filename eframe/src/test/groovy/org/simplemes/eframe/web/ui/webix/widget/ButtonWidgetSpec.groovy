/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.HTMLTestUtils
import org.simplemes.eframe.test.JavascriptTestUtils

/**
 * Tests.
 */
class ButtonWidgetSpec extends BaseWidgetSpecification {

  def "verify that apply generates the button correctly - simple case with all defaults"() {
    given: 'some options the widget'
    def widgetContext = new WidgetContext(parameters: [label: 'edit.menu.label', click: 'funcA'])

    when: 'the widget text is built'
    def page = new ButtonWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the basic properties are correct'
    JavascriptTestUtils.extractProperty(page, 'view') == 'button'
    JavascriptTestUtils.extractProperty(page, 'width') == 'tk.pw("4em")'
    JavascriptTestUtils.extractProperty(page, 'type') == 'icon'
    JavascriptTestUtils.extractProperty(page, 'label') == lookup('edit.menu.label')
    JavascriptTestUtils.extractProperty(page, 'tooltip') == lookup('edit.menu.tooltip')
    JavascriptTestUtils.extractProperty(page, 'icon') == null
    JavascriptTestUtils.extractProperty(page, 'click') == 'funcA'

    and: 'no ID is used - the ID from the toolkit library'
    !JavascriptTestUtils.extractProperty(page, 'id')
  }

  def "verify that apply generates the button correctly - html link button with icon"() {
    given: 'some options the widget'
    def widgetContext = new WidgetContext(parameters: [label: 'edit.menu.label',
                                                       icon : 'fa-th-list',
                                                       link : '/user/show/123'])

    when: 'the widget text is built'
    def page = new ButtonWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkJavascriptFragment(page)

    and: 'the basic properties are correct'
    JavascriptTestUtils.extractProperty(page, 'type') == 'htmlbutton'
    JavascriptTestUtils.extractProperty(page, 'tooltip') == lookup('edit.menu.tooltip')
    JavascriptTestUtils.extractProperty(page, 'icon') == 'fas fa-th-list'

    and: 'the link is correct'
    def linkText = JavascriptTestUtils.extractProperty(page, 'label')
    HTMLTestUtils.checkHTML(linkText)
    linkText.contains('<a href="/user/show/123"')
    linkText.contains("""<span class="toolbar-span"> ${lookup('edit.menu.label')}</span>""")
  }

  def "verify that apply generates the button correctly - no label with icon and click script"() {
    given: 'some options the widget'
    def widgetContext = new WidgetContext(parameters: [icon : 'fa-th-list',
                                                       click: 'home()'])

    when: 'the widget text is built'
    def page = new ButtonWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the basic properties are correct'
    JavascriptTestUtils.extractProperty(page, 'type') == 'icon'
    JavascriptTestUtils.extractProperty(page, 'tooltip') == null
    JavascriptTestUtils.extractProperty(page, 'label') == null

    and: 'the click script is real script, as a quoted string'
    JavascriptTestUtils.extractProperty(page, 'click') == 'home()'
    page.contains('"home()"')
  }

  def "verify that apply generates the button correctly - tooltip and ID passed in with no label"() {
    given: 'some options the widget'
    def widgetContext = new WidgetContext(parameters: [tooltip: 'edit.menu.tooltip',
                                                       id     : 'customID'])

    when: 'the widget text is built'
    def page = new ButtonWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the basic properties are correct'
    JavascriptTestUtils.extractProperty(page, 'type') == 'icon'
    JavascriptTestUtils.extractProperty(page, 'tooltip') == lookup('edit.menu.tooltip')
    JavascriptTestUtils.extractProperty(page, 'label') == null
    JavascriptTestUtils.extractProperty(page, 'id') == 'customID'
  }

  def "verify that apply generates the button correctly - css option"() {
    given: 'some options the widget'
    def widgetContext = new WidgetContext(parameters: [css: 'caution-button'])

    when: 'the widget text is built'
    def page = new ButtonWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the basic properties are correct'
    JavascriptTestUtils.extractProperty(page, 'css') == 'caution-button'
  }

  def "verify that apply generates the button correctly - width passed in as number"() {
    given: 'some options the widget'
    def widgetContext = new WidgetContext(parameters: [width: 147])

    when: 'the widget text is built'
    def page = new ButtonWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the basic properties are correct'
    JavascriptTestUtils.extractProperty(page, 'width') == "147"
  }

  def "verify that apply generates the button correctly - multiple sub-menus"() {
    given: 'the buttons for the sub menu'
    def subMenu1 = [id: 'buttonDelete', label: 'delete.menu.label', click: 'deleteMenuHandler']
    def subMenu2 = [id: 'buttonEdit', label: 'edit.menu.label', click: 'editMenuHandler']

    and: 'some options the widget'
    def list = [subMenu1, subMenu2]
    def widgetContext = new WidgetContext(parameters: [id: 'moreButton', label: 'more.menu.label', subMenus: list])

    when: 'the widget text is built'
    def page = new ButtonWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the menu is defined correctly'
    JavascriptTestUtils.extractProperty(page, 'view') == 'menu'
    JavascriptTestUtils.extractProperty(page, 'id') == 'moreButton'
    JavascriptTestUtils.extractProperty(page, 'css') == 'toolbar-with-submenu'
    JavascriptTestUtils.extractProperty(page, 'openAction') == 'click'
    JavascriptTestUtils.extractProperty(page, 'type') == '{subsign: true'

    and: 'the inner menu ID/label is correct'
    //          {id: "moreButtonMenu", value: "More...", submenu: [
    def moreMenuText = JavascriptTestUtils.extractBlock(page, 'id: "moreButtonMenu"')
    JavascriptTestUtils.extractProperty(moreMenuText, 'value') == lookup('more.menu.label')

    and: 'the sub-menu definitions are correct'
    def dataText = JavascriptTestUtils.extractBlock(page, 'data')
    def subMenuText = JavascriptTestUtils.extractBlock(dataText, 'submenu: [')

    and: 'the first sub-menu is correct'
    def subMenu1Text = TextUtils.findLine(subMenuText, 'id: "buttonDelete"')
    JavascriptTestUtils.extractProperty(subMenu1Text, 'value') == lookup('delete.menu.label')
    JavascriptTestUtils.extractProperty(subMenu1Text, 'tooltip') == lookup('delete.menu.tooltip')

    def subMenu2Text = TextUtils.findLine(subMenuText, 'id: "buttonEdit"')
    JavascriptTestUtils.extractProperty(subMenu2Text, 'value') == lookup('edit.menu.label')
    JavascriptTestUtils.extractProperty(subMenu2Text, 'tooltip') == lookup('edit.menu.tooltip')

    and: 'the click handlers are correct'
    def handlersText = JavascriptTestUtils.extractBlock(page, 'onMenuItemClick:')
    def handler1Text = TextUtils.findLine(handlersText, 'id=="buttonDelete"')
    handler1Text.contains('deleteMenuHandler()')
    def handler2Text = TextUtils.findLine(handlersText, 'id=="buttonEdit"')
    handler2Text.contains('editMenuHandler()')
  }

  def "verify that apply generates the button correctly - submenu click handler with parentheses"() {
    given: 'the buttons for the sub menu'
    def subMenu1 = [id: 'buttonDelete', label: 'delete.menu.label', click: 'deleteMenuHandler(a,b)']

    and: 'some options the widget'
    def widgetContext = new WidgetContext(parameters: [id: 'moreButton', label: 'more.menu.label', subMenus: [subMenu1]])

    when: 'the widget text is built'
    def page = new ButtonWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the click handler is correct'
    def handlersText = JavascriptTestUtils.extractBlock(page, 'onMenuItemClick:')
    def handler1Text = TextUtils.findLine(handlersText, 'id=="buttonDelete"')
    handler1Text.contains('deleteMenuHandler(a,b)')
  }

  def "verify that apply generates the button correctly - submenu no top-level button ID"() {
    given: 'the buttons for the sub menu'
    def subMenu1 = [id: 'buttonDelete', label: 'delete.menu.label', click: 'deleteMenuHandler(a,b)']

    and: 'some options the widget'
    def widgetContext = new WidgetContext(parameters: [label: 'more.menu.label', subMenus: [subMenu1]])

    when: 'the widget text is built'
    def page = new ButtonWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkPage(page)
  }

  def "verify that apply handles a label that is not a lookup key with .label"() {
    given: 'some options the widget'
    def widgetContext = new WidgetContext(parameters: [label: 'XYZZY'])

    when: 'the widget text is built'
    def page = new ButtonWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the basic properties are correct'
    JavascriptTestUtils.extractProperty(page, 'label') == 'XYZZY'
  }

  def "verify that the widget supports submenu with menu separator option"() {
    given: 'the buttons for the sub menu'
    def subMenu1 = [separator: true]

    and: 'some options the widget'
    def widgetContext = new WidgetContext(parameters: [id: 'moreButton', label: 'more.menu.label', subMenus: [subMenu1]])

    when: 'the widget text is built'
    def page = new ButtonWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the separator is correct'
    def dataText = JavascriptTestUtils.extractBlock(page, 'data')
    def subMenuText = JavascriptTestUtils.extractBlock(dataText, 'submenu: [')
    subMenuText.contains('{$template: "Separator"}')
  }


}
