package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.JavascriptTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ToolbarWidgetSpec extends BaseWidgetSpecification {

  def "verify that apply generates the toolbar correctly - simple case with all defaults"() {
    given: 'the buttons for the toolbar'
    def list = [id: 'buttonList', label: 'list.menu.label']
    def buttons = [list]

    and: 'some options the widget'
    def widgetContext = new WidgetContext(parameters: [id: "showList", buttons: buttons])

    when: 'the widget text is built'
    def page = new ToolbarWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the basic properties are correct'
    JavascriptTestUtils.extractProperty(page, 'view') == 'toolbar'
    JavascriptTestUtils.extractProperty(page, 'paddingY') == "-2"
    JavascriptTestUtils.extractProperty(page, 'id') == 'showList'

    and: 'the buttons are created correctly'
    def buttonText = JavascriptTestUtils.extractBlock(page, 'elements: [')

    and: 'the button options are passed to the button widget correctly'
    def listButtonText = TextUtils.findLine(buttonText, 'id: "buttonList"')
    JavascriptTestUtils.extractProperty(listButtonText, 'view') == 'button'
    JavascriptTestUtils.extractProperty(listButtonText, 'label') == lookup('list.menu.label')
  }

  def "verify that apply generates the toolbar correctly - with spacer for right-alignment and list of buttons"() {
    given: 'the buttons for the toolbar'
    def list = [id: 'buttonList', label: 'list.menu.label']
    def create = [id: 'buttonCreate', label: 'create.menu.label']
    def buttons = [list, 'Title', create]

    and: 'some options the widget'
    def widgetContext = new WidgetContext(parameters: [id: "showList", buttons: buttons])

    when: 'the widget text is built'
    def page = new ToolbarWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the buttons are created correctly'
    def buttonText = JavascriptTestUtils.extractBlock(page, 'elements: [')

    and: 'the first button is created'
    def listButtonText = TextUtils.findLine(buttonText, 'id: "buttonList"')
    JavascriptTestUtils.extractProperty(listButtonText, 'view') == 'button'

    and: 'the spacer text is in the middle'
    //        text = """{view: "label", template: "<span>$button</span>"}"""
    def spaceText = TextUtils.findLine(buttonText, 'view: "label"')
    spaceText.contains('template: "<span>Title</span>"')

    and: 'the last button is created'
    def createButtonText = TextUtils.findLine(buttonText, 'id: "buttonCreate"')
    JavascriptTestUtils.extractProperty(createButtonText, 'view') == 'button'
  }

  def "verify that apply generates the toolbar correctly - with paddingY and no ID"() {
    given: 'the buttons for the toolbar'
    def list = [id: 'buttonList', label: 'list.menu.label']
    def buttons = [list]

    and: 'some options the widget'
    def widgetContext = new WidgetContext(parameters: [paddingY: "-137", buttons: buttons])

    when: 'the widget text is built'
    def page = new ToolbarWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the basic properties are correct'
    JavascriptTestUtils.extractProperty(page, 'view') == 'toolbar'
    JavascriptTestUtils.extractProperty(page, 'paddingY') == "-137"

    and: 'the toolbar view has no ID'
    def viewLine = TextUtils.findLine(page, 'view: "toolbar"')
    !JavascriptTestUtils.extractProperty(viewLine, 'id')
  }

  def "verify that apply generates the toolbar correctly - sub-menu"() {
    given: 'the buttons for the toolbar'
    def subMenu = [id: 'buttonDelete', label: 'delete.menu.label', click: 'deleteMenuHandler']
    def list = [id: 'buttonMore', label: 'more.menu.label', subMenus: [subMenu]]
    def buttons = [list]

    and: 'some options the widget'
    def widgetContext = new WidgetContext(parameters: [buttons: buttons])

    when: 'the widget text is built'
    def page = new ToolbarWidget(widgetContext).build().toString()

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the toolbar menu is defined correctly'
    def buttonText = JavascriptTestUtils.extractBlock(page, 'elements: [')
    def moreButtonText = TextUtils.findLine(buttonText, 'id: "buttonMore"')
    JavascriptTestUtils.extractProperty(moreButtonText, 'view') == 'menu'

    and: 'the sub-menu is created correctly'
    def subMenuText = JavascriptTestUtils.extractBlock(buttonText, '{id: "buttonDelete"')
    JavascriptTestUtils.extractProperty(subMenuText, 'value') == lookup('delete.menu.label')
    JavascriptTestUtils.extractProperty(subMenuText, 'tooltip') == lookup('delete.menu.tooltip')

    and: 'the click handler is correct'
    def clickHandlerText = TextUtils.findLine(page, 'if (id=="buttonDelete")')
    clickHandlerText.contains('deleteMenuHandler()')
  }
}
