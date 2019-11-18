package org.simplemes.eframe.controller

import groovy.transform.ToString
import io.micronaut.views.ModelAndView
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.web.ui.webix.freemarker.MarkerContext

import java.security.Principal

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines an enhanced version of the model and view object used to display a page
 * from a controller method.  Adds user principal details and marker data to the normal ModelAndView.
 */
@ToString(includePackage = false, includeNames = true, includeSuper = true)
class StandardModelAndView extends ModelAndView {

  /**
   * The name of the Freemarker data model element that holds the logged in flag.   The logged in flag is a Boolean.
   * True if the user is logged in (<b>Value:</b> '_loggedIn').
   */
  public static final String LOGGED_IN = '_loggedIn'

  /**
   * The name of the Freemarker data model element that holds the logged in user's name.   The logged in user's name
   * is a String (<b>Value:</b> '_userName').
   */
  public static final String USER_NAME = '_userName'

  /**
   * The name of the Freemarker data model element that holds the server-side messages to display to the user.
   * The messages are {@link org.simplemes.eframe.exception.MessageHolder} (<b>Value:</b> '_messages').
   */
  public static final String MESSAGES = '_messages'

  /**
   * The name of the Freemarker data model element that holds the server-side flash message text.
   * Displayed in the errors area of the standard error.ftl page.
   */
  public static final String FLASH = '_flash'

  /**
   * The name of the Freemarker data model element that holds the server-side flash details message text.
   * Displayed in the standard error.ftl page.
   */
  public static final String FLASH_DETAILS = '_flashDetails'

  /**
   * The name of the Freemarker data model element that holds the {@link MarkerContext} for the current
   * page (<b>Value:</b> '_markerContext').
   */
  public static final String MARKER_CONTEXT = '_markerContext'

  /**
   * The name of the Freemarker data model element that holds the HTTP request object (<b>Value:</b> 'request').
   */
  public static final String REQUEST = 'request'

  /**
   * The name of the Freemarker data model element that holds the HTTP request's parameters (as a Map) (<b>Value:</b> 'params').
   */
  public static final String PARAMS = 'params'

  StandardModelAndView() {
  }

  /**
   * Builds the model and view with the standard info added.
   * If there is a current request, then it is added to the model as a 'request' and the
   * params are added as a map 'params'.
   * @param view The view to display.
   * @param principal The logged in user.
   * @param controller The controller instance.
   */
  StandardModelAndView(String view, Principal principal, Object controller) {
    super()
    def model = [:]
    this.view = view
    setModel(model)

    def uri = Holders.currentRequest?.uri
    model[MARKER_CONTEXT] = new MarkerContext(controller: controller, view: view, uri: uri)

    model[LOGGED_IN] = (principal != null)
    model[USER_NAME] = principal?.getName()

    def request = Holders.currentRequest
    if (request) {
      model[REQUEST] = request
      model[PARAMS] = ControllerUtils.instance.convertToMap(request.parameters)
    }

  }

  /**
   * Provides subscript access to the model without worrying about the Optional get() call.
   * @param key The element to get from the model.
   * @return The element from the model.
   */
  Object getAt(String key) {
    return model.get()[key]
  }

  /**
   * Provides subscript access to the model without worrying about the Optional get() call.
   * @param key The element to put into the model.
   * @param value The element to store in the model.
   * @return The element.
   */
  Object putAt(String key, Object value) {
    model.get()[key] = value
    return value
  }

}
