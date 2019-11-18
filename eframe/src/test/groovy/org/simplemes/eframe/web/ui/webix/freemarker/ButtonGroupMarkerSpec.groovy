package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

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


}
