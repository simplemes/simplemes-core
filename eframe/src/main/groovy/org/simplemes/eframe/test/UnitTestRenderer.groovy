/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import freemarker.template.Configuration
import freemarker.template.Template
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.web.ui.webix.freemarker.FreemarkerDirectiveConfiguration
import org.simplemes.eframe.web.ui.webix.freemarker.MarkerContext

/**
 * Defines a Freemarker renderer that can render views from memory(string).
 * <h3>Options</h3>
 * <ul>
 *   <li><b>source</b> - The .ftl source to expand (<b>Required</b>) </li>
 *   <li><b>controllerClass</b> - The controller used to serve up this page. </li>
 *   <li><b>uri</b> - The URI for this page (<b>Default:</b> based on the controller name). </li>
 *   <li><b>domainObject</b> - The domain object to store in the data model for the marker. </li>
 *   <li><b>errors</b> - The domain object errors to store in the data model for the marker. </li>
 *   <li><b>dataModel</b> - The data model for the marker to use. </li>
 * </ul>
 */
class UnitTestRenderer {

  /**
   * The writer that will hold the rendered view.
   */
  StringWriter resultWriter = new StringWriter()

  /**
   * Builds a renderer with the given model and related values for rendering views.
   * @param options See options above.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  UnitTestRenderer(Map options) {
    def uri = options?.uri
    if (!uri && options?.controllerClass) {
      uri = ControllerUtils.instance.getRootPath((Class) options.controllerClass)
    }

    def markerContext = new MarkerContext((Class) options.controllerClass)
    markerContext.uri = uri
    Map map = [(StandardModelAndView.MARKER_CONTEXT): markerContext]

    if (options.domainObject) {
      def domainObject = options.domainObject
      String domainInstanceName = (String) NameUtils.lowercaseFirstLetter(domainObject.getClass().simpleName)
      map[domainInstanceName] = domainObject
    }

    if (options.errors) {
      map[ControllerUtils.MODEL_KEY_DOMAIN_ERRORS] = options.errors
    }

    // Copy the data model to the model for the marker.
    options.dataModel?.each() { k, v ->
      map[k] = options.dataModel[k]
    }

    Template template = new Template(this.class.simpleName, new StringReader(options.source as String), freemarkerConfiguration)
    template.process(map, resultWriter)
  }

  /**
   * Renders the rendered content.
   * @return The content.
   */
  String render() {
    return resultWriter.toString()
  }

  /**
   * The unit test Freemarker configuration class.
   */
  private static Configuration _freemarkerConfiguration

  /**
   * Returns the the unit test Freemarker configuration class.
   * @return
   */
  static Configuration getFreemarkerConfiguration() {
    if (!_freemarkerConfiguration) {
      _freemarkerConfiguration = new Configuration()
      FreemarkerDirectiveConfiguration.addSharedVariables(_freemarkerConfiguration)
    }
    return _freemarkerConfiguration
  }
}
