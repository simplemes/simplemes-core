/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils

/**
 *  Tests.
 */
class ButtonGroupMarkerSpec extends BaseMarkerSpecification {

  def "verify that the marker can support buttons in the content"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efButtonGroup>
          <@efButton label="list.menu.label" click='ABC();'/>
        </@efButtonGroup>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the button group is created'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'cols: [')
    def widthBlock = JavascriptTestUtils.extractBlock(columnsBlock, '{width:')
    JavascriptTestUtils.extractProperty(widthBlock, 'width') == 'tk.pw("15%")'

    and: 'the button is in the group'
    def buttonBlock = JavascriptTestUtils.extractBlock(columnsBlock, '{view:')
    JavascriptTestUtils.extractProperty(buttonBlock, 'label') == lookup('list.menu.label')
  }

  def "verify that the marker supports the spacerWidth option"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efButtonGroup spacerWidth="5%">
          <@efButton label="list.menu.label" click='ABC();'/>
        </@efButtonGroup>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the button group is created'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'cols: [')
    def widthBlock = JavascriptTestUtils.extractBlock(columnsBlock, '{width:')
    JavascriptTestUtils.extractProperty(widthBlock, 'width') == 'tk.pw("5%")'
  }


}
