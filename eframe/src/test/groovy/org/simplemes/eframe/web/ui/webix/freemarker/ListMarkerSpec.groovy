package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import sample.controller.SampleParentController

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ListMarkerSpec extends BaseMarkerSpecification {

  static specNeeds = [SERVER]

  def "verify that this marker uses the handlebar options correctly"() {
    when: 'the marker is built'
    def page = execute(source: '<@efList columns="name,title"/>', controllerClass: SampleParentController)

    then: 'the correct fields are in the column list'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'columns: [')
    columnsBlock.contains('id: "name"')
    columnsBlock.contains('id: "title"')
  }

}
