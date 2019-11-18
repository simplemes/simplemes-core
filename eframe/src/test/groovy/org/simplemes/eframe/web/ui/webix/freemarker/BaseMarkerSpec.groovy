package org.simplemes.eframe.web.ui.webix.freemarker

import freemarker.core.Environment
import freemarker.ext.beans.SimpleMapModel
import freemarker.template.DefaultObjectWrapper
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.custom.controller.ExtensionController
import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.web.ui.webix.widget.WidgetContext
import sample.controller.SampleParentController

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.  Uses some concrete Marker classes to test the base class features.
 */
class BaseMarkerSpec extends BaseMarkerSpecification {

  /**
   * Creates a marker with the values set as given in the options.  The marker is generally not executable, but the methods are
   * generally callable.
   * <h3>Options</h3>
   * <ul>
   *   <li><b>source</b> - The .hbs source to expand (<b>Required</b>) </li>
   *   <li><b>controller</b> - The controller used to serve up this page. </li>
   *   <li><b>parameters</b> - Additional simulated marker parameters. </li>
   *   <li><b>uri</b> - The URI for this page (<b>Default:</b> based on the controller name). </li>
   *   <li><b>domainObject</b> - The domain object to store in the data model for the marker. </li>
   *   <li><b>model</b> - A Map containing additional elements to add to the StandardModelAndView. </li>
   * </ul>
   * @param markerClass The marker to build
   * @param options See options above.
   * @return The marker.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  BaseMarker buildMarker(Class<MarkerInterface> markerClass, Map options) {
    def domainObject = options.domainObject
    String domainInstanceName = (String) NameUtils.lowercaseFirstLetter(domainObject.getClass().simpleName)

    def uri = options?.uri
    if (!uri && options?.controllerClass) {
      uri = ControllerUtils.instance.getRootPath((Class) options.controllerClass)
    }

    def markerContext = new MarkerContext((Class) options.controllerClass)
    markerContext.uri = uri
    markerContext.view = options.view
    def map = [(StandardModelAndView.MARKER_CONTEXT): markerContext]
    map[domainInstanceName] = domainObject

    // Make a mock environment to provide the data model to the marker.
    def environment = GroovyMock(Environment)
    environment.getDataModel() >> new SimpleMapModel(map, new DefaultObjectWrapper())
    if (options.model) {
      // Add any additional model elements desired
      options.model.each() { k, v ->
        map[k] = v
      }
    }

    // And a mock object wrapper to return the MarkerContext created above.
    // For some reason, the Freemarker SimpleMapModel converts the MarkerContext to a string model.
    def objectWrapper = Stub(DefaultObjectWrapper)
    BaseMarker._objectWrapper = objectWrapper
    objectWrapper.unwrap(_, MarkerContext) >> { args -> return markerContext }
    objectWrapper.unwrap(_) >> { args -> return args[0] }

    registerAutoCleanup({ testSpec -> BaseMarker._objectWrapper = null })

    def marker = markerClass.newInstance()
    marker.setValues(environment, options.parameters, null, null)

    return marker
  }

  def "verify that toStringForException re-creates the marker usage as closely as possible"() {
    when: 'the marker is built'
    def marker = buildMarker(ListMarker, [parameters: [columns: 'a,b'], uri: '/sampleParent/show', view: 'showList.ftl', controllerClass: SampleParentController])

    then: 'the toString has the right info'
    def s = marker.toStringForException()
    s.contains('showList')
    s.contains('SampleParentController')

    and: 'the marker text approximates how it would look in the hbs file'
    s.contains('efList')
    s.contains("columns='a,b'")
  }

  def "verify that buildWidgetContext gathers the correct data for the marker"() {
    when: 'the widget context is built'
    def marker = buildMarker(ListMarker, [parameters: [columns: 'a,b'], uri: '/logging', view: 'logging/index', controllerClass: SampleParentController])
    def widgetContext = marker.buildWidgetContext()

    then: 'the widget context is correct'
    widgetContext.parameters == [columns: 'a,b']
    widgetContext.marker.is(marker)
    widgetContext.controllerClass == SampleParentController
    widgetContext.uri == '/logging'
    widgetContext.view == 'logging/index'
  }

  def "verify that buildWidgetContext clones the parameters to allow local changes for a single widget"() {
    when: 'the widget context is built'
    def marker = buildMarker(ListMarker, [uri: '/logging', view: 'logging/index', controllerClass: SampleParentController])
    def widgetContext1 = marker.buildWidgetContext()
    def widgetContext2 = marker.buildWidgetContext()

    and: 'then the value is changed in the parameters for one widgetContext'
    widgetContext1.parameters.testParam = 'abc'

    then: 'the other widget context is unchanged'
    widgetContext2.parameters.testParam == null

    and: 'the marker is stored in the context'
  }

  def "verify that escape works"() {
    when: 'the marker is built'
    def marker = buildMarker(ListMarker, [:])

    then: 'the escape works on the supported cases'
    marker.escape(value) == results

    where:
    value | results
    '<'   | '&lt;'
    '>'   | '&gt;'
    '"'   | '&quot;'
    '&'   | '&amp;'
    ''    | ''
    null  | null
  }

  def "verify that setValues uses the domain object fallback for generic controllers"() {
    when: 'the marker is built'
    def model = [:]
    model[ControllerUtils.MODEL_DOMAIN_OBJECT] = 'ABC'
    def marker = buildMarker(ListMarker, [uri: '/extension', controllerClass: ExtensionController, model: model])

    then: 'the domain object is correct'
    marker.domainObject.toString() == 'ABC'
  }

  def "verify that addFieldSpecificParameters adds any field-specific values"() {
    given: 'a widget context'
    def fieldDef = new SimpleFieldDefinition(name: 'priority')
    def widgetContext = new WidgetContext(fieldDefinition: fieldDef)

    when: 'the field-specific parameters are merged into the context'
    new FieldMarker().addFieldSpecificParameters(widgetContext, 'priority', ['priority@required': 'true'])

    then: 'the fields are added to the context'
    widgetContext.parameters['required'] == 'true'
  }

  def "verify that addFieldSpecificParameters uses the fieldDefinition hints as a priority over the marker params"() {
    given: 'a widget context'
    def fieldDef = new SimpleFieldDefinition(name: 'priority', guiHints: [required: 'false'])
    def widgetContext = new WidgetContext(fieldDefinition: fieldDef)

    when: 'the field-specific parameters are merged into the context'
    new FieldMarker().addFieldSpecificParameters(widgetContext, 'priority', ['priority@required': 'true'])

    then: 'the addition settings overrides the mark settings'
    widgetContext.parameters['required'] == 'false'
  }

}
