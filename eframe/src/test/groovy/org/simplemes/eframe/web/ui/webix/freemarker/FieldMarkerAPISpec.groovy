/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.JavascriptTestUtils
import sample.controller.SampleParentController
import sample.domain.RMA

/**
 * Tests.
 */
class FieldMarkerAPISpec extends BaseMarkerSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [FlexType]

  def "verify that the marker supports flex type with uuid for flex type"() {
    given: 'a flex type'
    def flexType = DataGenerator.buildFlexType()
    def valueObject = new RMA(rmaType: flexType)

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="RMA.rmaType" valueName="assemblyData"/>
      </@efForm>
    """
    def page = execute(source: src, controllerClass: SampleParentController, dataModel: [assemblyData: valueObject])

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the combo box is used'
    def fieldLine = TextUtils.findLine(page, 'id: "rmaType"')
    JavascriptTestUtils.extractProperty(fieldLine, 'view') == 'combo'

    and: 'the holder area has the default input field'
    def holder = JavascriptTestUtils.extractBlock(page, 'rows: [')
    def content = JavascriptTestUtils.extractBlock(holder, 'id: "rmaTypeContent",rows: [')
    def field1Line = TextUtils.findLine(content, 'id: "rmaType_FIELD1"')
    JavascriptTestUtils.extractProperty(field1Line, 'view') == 'text'

  }

}
