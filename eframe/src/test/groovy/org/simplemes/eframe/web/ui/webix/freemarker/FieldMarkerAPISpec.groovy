/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.controller.SampleParentController
import sample.domain.RMA
import sample.domain.SampleParent

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
        <@efField field="RMA.rmaType" modelName="assemblyData"/>
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
    def field1Line = TextUtils.findLine(content, 'id: "FIELD1"')
    JavascriptTestUtils.extractProperty(field1Line, 'view') == 'text'

  }

  @Rollback
  def "verify that the marker detects when a model element matches the domain name - in non-definition page scenario"() {
    // Happens when the model has a 'sampleParent' domain object in it and the value is specified.
    // This triggers some un-wanted definition-page logic.
    given: 'a domain record'
    def sampleParent = new SampleParent(name: 'ABC', title: 'abc').save()

    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
           <@efField field="sampleParent"  value="${sampleParent.name}" readOnly="true"  />
           <@efField field="SampleParent.title"  />
      </@efForm>
    """

    execute(source: src, controllerClass: SampleParentController, dataModel: [sampleParent: sampleParent])

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['sampleParent', 'ABC', 'model'])
  }

  def "verify that the marker generates the on change handler - combobox scenario"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="SampleParent.allFieldsDomain" onChange="someChangeLogic()"/>
      </@efForm>
    """

    def page = execute(source: src, controllerClass: SampleParentController)
    println "page = $page"

    // We can't check the JS since the on:{onChange...} syntax fails.
    // Assume it is checked in ComboboxWidgetGUISpec tests.
    //checkPage(page)

    then: 'the CSS is correct'
    def fieldLine = TextUtils.findLine(page, 'id: "allFieldsDomain"')
    fieldLine.contains("on:{onChange(newValue, oldValue){someChangeLogic()}}")
  }

}
