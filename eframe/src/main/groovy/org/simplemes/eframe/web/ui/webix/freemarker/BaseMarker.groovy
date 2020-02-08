/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import freemarker.core.Environment
import freemarker.template.ObjectWrapper
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateModel
import freemarker.template.utility.HtmlEscape
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.web.ui.webix.widget.WidgetContext
import org.simplemes.eframe.web.view.FreemarkerWrapper

/**
 * Defines the base marker class.  Most Freemarker marker helpers will use this base class.
 * Provides simplified access to parameters and common features.
 */
@Slf4j
@ToString(includeNames = true, includePackage = false)
abstract class BaseMarker implements MarkerInterface {


  /**
   * The constant used by many toolkit actions.  This just is two dollar signs, which is used by the toolkit to find
   * a view by id (e.g. $$("userGrid")...)
   */
  public static final String $$ = '$$'

  /**
   * The delimiter to use for the field-specific parameter names. For example: order@width='128'.
   */
  public static final String FIELD_SPECIFIC_PARAMETER_DELIMITER = '@'

  /**
   * The marker body contents.
   */
  TemplateDirectiveBody body

  /**
   * The loop variables for the template.
   */
  TemplateModel[] loopVars

  /**
   * The parameters passed in the options from Freemarker.
   */
  Map<String, String> parameters

  /**
   * The freemarker environment to process the directive in.
   */
  Environment environment

  /**
   * The context the marker is being executed in.
   */
  MarkerContext markerContext

  /**
   * The base HTML element/view ID to use for the marker.  This can be overridden at
   * the
   */
  String id

  /**
   * The controller that is serving the page this marker is used in.
   */
  Class controllerClass

  /**
   * The expected domain class for the controller.
   */
  Class domainClass

  /**
   * The default domain name for the domain object stored in the model.
   */
  String domainObjectName

  /**
   * The domain object from the model.
   */
  Object domainObject

  // These fields below are used to coordinate between

  /**
   * The post script added after the body is generated.
   */
  @SuppressWarnings("unused")
  StringBuilder postscript = new StringBuilder()

  /**
   * Empty Constructor.
   */
  BaseMarker() {
  }

  /**
   * Copy Constructor.  Copies the environment, params, etc from the given marker.
   * @param otherMarker The other marker to copy the freemarker values from.
   */
  BaseMarker(BaseMarker otherMarker) {
    super()
    if (otherMarker) {
      setValues(otherMarker.environment, otherMarker.parameters, otherMarker.loopVars, otherMarker.body)
    }
  }

  /**
   * Sets the values passed from the directive execution.  This includes the values needed to build the HTML output.
   * @param env
   * @param paramsIn
   * @param loopVars
   * @param body
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  @Override
  void setValues(Environment env, Map paramsIn, TemplateModel[] loopVars, TemplateDirectiveBody body) {
    this.environment = env
    this.loopVars = loopVars
    this.body = body

    // Unwrap any parameters and store them in a copy of the paramsIn map.
    parameters = [:]
    paramsIn.each { k, v ->
      parameters[k] = unwrap(paramsIn[k])
    }

    markerContext = objectWrapper.unwrap(env.dataModel?.get(StandardModelAndView.MARKER_CONTEXT), MarkerContext)
    //println "markerContext (${markerContext.hashCode()}, ${markerContext.getClass()}) = $markerContext"

    // Now, build the object to be processed based on the controller.
    controllerClass = markerContext?.controllerClass
    if (controllerClass) {
      domainClass = ControllerUtils.instance.getDomainClass(controllerClass)
      if (domainClass) {
        domainObjectName = NameUtils.toDomainName(domainClass)
        domainObject = unwrap(environment.dataModel?.get(domainObjectName))
      } else {
        // Try for an explicit object in the model.  Used when the controller is not specific to single domain.
        domainObjectName = ControllerUtils.MODEL_KEY_DOMAIN_OBJECT
        domainObject = unwrap(environment.dataModel?.get(domainObjectName))
        domainClass = domainObject?.getClass()
      }
    }

    log.trace('setValues(): parameters {}, markerContext {}', parameters, markerContext)
  }

  /**
   * Unwraps the given object from the freemarker variable holder.
   * @param value The value to unwrap.
   * @return
   */
  def unwrap(Object value) {
    if (value instanceof TemplateModel) {
      return getObjectWrapper().unwrap(value)
    }
    return value
  }

  /**
   * Gets an unwrapped model value for the given key name.
   * @param name The name of the element.
   * @return The unwrapped value.
   */
  def getModelValue(String name) {
    def object = unwrap(environment?.dataModel?.get(name))

    if (object instanceof FreemarkerWrapper) {
      return object.value
    }
    return object
  }

  /**
   * A cached object wrapper for accessing the raw objects in a freemarker data model.
   */
  static ObjectWrapper _objectWrapper

  /**
   * Returns the object wrapper for accessing the raw objects in a freemarker data model.
   */
  static ObjectWrapper getObjectWrapper() {
    if (!_objectWrapper) {
      _objectWrapper = Environment.currentEnvironment.getObjectWrapper()
      //println "_objectWrapper = $_objectWrapper"
    }
    return _objectWrapper
  }

  /**
   * A cached escape writer for Freemarker.
   */
  private static HtmlEscape escape

  /**
   * Escape the string for safe HTML output.
   * @param value The value to escape.
   * @return The HTML escaped value.
   */
  static String escape(String value) {
    if (!value) {
      return value
    }
    if (!escape) {
      escape = new HtmlEscape()
    }
    def stringWriter = new StringWriter()
    escape.getWriter(stringWriter, [:]).write(value)
    return stringWriter.toString()
  }

  /**
   * Writes the given output to the renderer.
   * @param obj The object (written using toString()).
   */
  void write(Object obj) {
    if (obj) {
      environment.out.write(obj.toString())
    }
  }

  /**
   * Builds a human-readable form of this marker and its context for use in exceptions.
   * This should give user some idea where the error happened.
   * @return The exception display.
   */
  String toStringForException() {
    def name = this.class.simpleName - 'Marker'
    def options = new StringBuilder()
    parameters.each { k, v ->
      options << " "
      options << "$k='$v'"
    }
    def sContext = markerContext?.toStringForException() ?: ''

    return "<@ef$name $options/> ($sContext)"
  }

  /**
   * Builds the widget context for this marker.  This avoids embedding knowledge of the marker in the widget classes.
   * @param The field definition for the field this widget is generating the UI for.
   * @return The WidgetContext.
   */
  WidgetContext buildWidgetContext(FieldDefinitionInterface fieldDefinition) {
    def widgetContext = new WidgetContext()

    widgetContext.parameters = (Map) parameters.clone()
    widgetContext.marker = this
    widgetContext.markerCoordinator = markerContext?.markerCoordinator

    widgetContext.controllerClass = markerContext?.controllerClass
    widgetContext.uri = markerContext?.uri
    widgetContext.view = markerContext?.view

    widgetContext.fieldDefinition = fieldDefinition

    return widgetContext
  }

  /**
   * Adds any field-specific parameters from the marker parameters to the given widget context's parameters
   * for the field widget.  For example, the ID can be set with 'qty:id="CustomQTY"'.
   * @param widgetContext The widget context to add the field-specific parameters to.
   * @param fieldName The name of the field to check for field-specific fields.
   * @param parameters The marker's parameters.
   */
  protected void addFieldSpecificParameters(WidgetContext widgetContext, String fieldName, Map parameters) {
    // look for all keys with the field's prefix
    def prefix = "$fieldName${FIELD_SPECIFIC_PARAMETER_DELIMITER}"

    for (String key in parameters.keySet()) {
      if (key.startsWith(prefix)) {
        def suffix = key - prefix
        if (suffix.toLowerCase() == 'readonly') {
          // Handle special case setting of widgetContext field values.
          widgetContext.readOnly = Boolean.valueOf((String) parameters[key])
        }
        widgetContext.parameters[suffix] = parameters[key]
      }
    }

    //  Add any GUI hints from the field definition (usually from an Addition).
    if (widgetContext.fieldDefinition?.guiHints) {
      widgetContext.fieldDefinition.guiHints.each { k, v ->
        widgetContext.parameters[k] = v
      }
    }
  }

  /**
   * Convenience method for general message.properties lookup.  See
   * {@link org.simplemes.eframe.i18n.GlobalUtils#lookup} for details.
   * @param key The key to lookup.
   * @param locale The locale to use for the message. (<b>Default</b>: Request Locale)
   * @param args The replaceable arguments used by the message (if any).
   * @return The looked up message.
   */
  @SuppressWarnings('UnnecessaryGetter')
  String lookup(String key, Locale locale = null, Object... args) {
    return GlobalUtils.lookup(key, locale, args)
  }

  /**
   * Builds the codes to pre-load for the code generated by a marker (e.g. for dialog elements created by this
   * marker).
   * @param codes The list of lookup codes to write for the locale.
   * @return The javascript to pre-load some codes for later lookup.
   */
  String buildPreloadedMessages(List<String> codes) {
    def sb = new StringBuilder()
    for (code in codes) {
      if (sb.length() > 0) {
        sb << "\n,"
      }
      def value = JavascriptUtils.escapeForJavascript(GlobalUtils.lookup((String) code))
      sb << """      {"$code": "$value"}"""
    }

    return """
      eframe._addPreloadedMessages([$sb]);
    """
  }


  /**
   * Generate the body content so we can access any variables set by the markers in the body.
   * @return The content rendered.
   */
  protected String renderContent() {
    def contentWriter = new StringWriter()
    if (body) {
      body.render(contentWriter)
    }
    return contentWriter.toString()
  }
}
