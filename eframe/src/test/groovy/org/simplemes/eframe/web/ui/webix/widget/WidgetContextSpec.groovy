package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.web.ui.webix.freemarker.AssetMarker
import sample.controller.SampleParentController

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class WidgetContextSpec extends BaseSpecification {

  def "verify that the copy constructor works"() {
    given: 'a widgetContext'
    def widgetContext = new WidgetContext()
    widgetContext[property] = value

    and: 'the parameters are set'
    widgetContext.parameters = [otherID: 'PDQ']

    when: 'the widget is cloned through the constructor'
    def widgetContext2 = new WidgetContext(widgetContext, [id: 'XYZ'])

    then: 'the original property is copied'
    widgetContext2[property] == widgetContext[property]

    and: 'the original parameters are not in the new widgets parameters'
    widgetContext2.parameters.keySet().size() == 1
    widgetContext2.parameters.id == 'XYZ'

    where:
    property          | value
    'marker'          | new AssetMarker()
    'controllerClass' | SampleParentController
    'controller'      | new SampleParentController()
    'uri'             | '/uri'
    'view'            | '/view.hbs'
    'readOnly'        | true
    'object'          | 'anObject'
    'fieldDefinition' | new SimpleFieldDefinition(name: 'title', type: Integer)
  }
}
