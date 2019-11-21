package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.web.ui.JSPageOptions
import org.simplemes.eframe.web.ui.WidgetInterface
import org.simplemes.eframe.web.ui.webix.freemarker.BaseMarker

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Common elements used by most UI widgets.  Include HTML ID's and generating the HTML code along with the
 * Javascript for the UI toolkit.
 */
class BaseWidget implements WidgetInterface {

  /**
   * The constant used by many toolkit actions.  This just is two dollar signs, which is used by the toolkit to find
   * a view by id (e.g. $$("userGrid")...)
   */
  public static final String $$ = '$$'

  /**
   * The widget context this widget was built for (e.g. the parameters, uri, controller, etc).
   */
  WidgetContext widgetContext

  /**
   * This builder holds the page as it is being built.
   */
  StringBuilder builder = new StringBuilder()

  /**
   * The list of closing texts to add to the end of the generated UI text.
   */
  List<String> closingTexts = []

  /**
   * This builder holds event handlers and other elements needed outside of the main view object.
   */
  StringBuilder finalScript = new StringBuilder()

  /**
   * The base HTML element ID to use for this widget.  Defaults to the class name minus the 'Widget' and a prefix
   * of the controller argument, if provided).
   */
  String id


  /**
   * If true, then this widget allows the page to specify the 'controller' as a marker option in the page.
   * Sub-classes can set this to true in the constructor if the widget supports the controller option on the marker.
   */
  boolean controllerOverrideAllowed = false

  /**
   * Basic constructor for the widget.
   * @param widgetContext The context the widget is built for.
   */
  BaseWidget(WidgetContext widgetContext) {
    ArgumentUtils.checkMissing(widgetContext, 'widgetContext')
    this.widgetContext = widgetContext

    /**
     * Generate a reasonable ID for the view/HTML ID.
     */
    if (widgetContext.parameters.id) {
      id = widgetContext.parameters.id
    } else if (widgetContext.controllerClass) {
      def baseName = this.getClass().simpleName - 'Widget'
      baseName = widgetContext.controllerClass.simpleName - 'Controller' + baseName
      id = NameUtils.lowercaseFirstLetter(baseName)
    } else {
      def baseName = this.getClass().simpleName - 'Widget'
      if (widgetContext.parameters.controller) {
        baseName = widgetContext.parameters.controller + baseName
      }
      id = NameUtils.lowercaseFirstLetter(baseName)
    }

  }


  /**
   * Escape the string for safe HTML output.
   * @param value The value to escape.
   * @return The HTML escaped value.
   */
  String escape(String value) {
    return BaseMarker.escape(value)
  }

  /**
   * Escape the string for safe use inside of a Javascript string variable.
   * @param value The value to escape.
   * @param labelMode If true, then this is in label mode.  This means '<' will be converted to '&lt;' to work
   *                  around an issue with display labels in the toolkit.
   * @return The escaped value.
   */
  String escapeForJavascript(String value, Boolean labelMode = false) {
    return JavascriptUtils.escapeForJavascript(value, labelMode)
  }

  /**
   * Builds the string for the UI elements.
   * @return
   */
  CharSequence build() {
    throw new IllegalStateException("Please implement the build() method in ${this.getClass().name}")
  }

  /**
   * Returns the final UI text and adds the closing text values as needed.
   * @return The final UI text.
   */
  CharSequence generate() {
    // Add the closing texts to the generated UI
    for (s in closingTexts.reverse()) {
      builder << s
    }

    builder << finalScript
    return builder.toString()
  }

  /**
   * Adds the given text in a holding area to be added to the generated UI text at the end.  This is processed
   * by the generate method.
   * @param s The closing text.
   */
  void addClosingText(String s) {
    closingTexts << s
  }


  /**
   * Convenience method for general message.properties lookup.  See
   * {@link GlobalUtils#lookup} for details.
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
   * Determines the Controller Class to be used by this widget.  This is usually the controller that served
   * the page this widget is displayed on.
   * <p>
   * <b>Note:</b> If the {@link #isControllerOverrideAllowed()}is set to true, then
   *              the controller can be overridden by the page by using the 'controller="UserController"' parameter.
   *
   * @return The controller class.  Can be null.
   */
  Class getControllerClass() {
    if (controllerOverrideAllowed) {
      return getEffectiveControllerClass()
    }
    return widgetContext.controllerClass
  }

  /**
   * Determines the Class for the domain related to the controller.
   * <p>
   * <b>Note:</b> If the {@link #isControllerOverrideAllowed()}is set to true, then
   *              the controller can be overridden by the page by using the 'controller="UserController"' parameter.
   * @return The domain class.  Can be null.
   */
  Class getDomainClass() {
    def controllerClass = getControllerClass()
    if (controllerClass) {
      return ControllerUtils.instance.getDomainClass(controllerClass)
    }
    return null
  }

  /**
   * Determines the effective domain class for the effective controller.
   * This uses the marker option 'controller' as the override of the controller the page was served from.
   * @return The domain class.  Can be null.
   */
  private Class getEffectiveControllerClass() {
    def controllerClass = widgetContext.controllerClass

    // See if the marker has a specific override for the controller.
    def name = widgetContext.parameters.controller
    if (name) {
      if (!name.endsWith('Controller')) {
        name = name + 'Controller'
      }
      controllerClass = ControllerUtils.instance.getControllerByName((String) name)
    }

    return controllerClass
  }

  /**
   * Returns the javascript expression for the standard label width.
   * Allows override with the labelWidth parameter.
   * @return The expression.
   */
  String buildStandardLabelWidth() {
    def override = widgetContext.parameters.labelWidth
    if (override) {
      return """width: tk.pw('$override') """
    } else {
      def name = JSPageOptions.LABEL_WIDTH_NAME
      def width = JSPageOptions.LABEL_WIDTH_DEFAULT
      return """width: tk.pw(ef.getPageOption('${name}','$width')) """
    }
  }

}