package org.simplemes.eframe.web.ui.webix.freemarker


import groovy.transform.ToString
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.web.ui.webix.freemarker.MarkerCoordinator

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the context a given marker instance is executing in.  This is typically
 * used to make sure the marker knows what controller it is associated with.
 */
@ToString(includeNames = true, includePackage = false)
class MarkerContext {

  /**
   * The controller class that served up the page this marker is in.
   */
  Class controllerClass

  /**
   * The controller instance that served up the page this marker is in.
   */
  Object controller

  /**
   * The view the marker was used in (<b>Default:</b> 'index').
   */
  String view = 'index'

  /**
   * The uri for the page.
   */
  String uri

  /**
   * Holds the various chunks of javascript/html generated for specific places in the result page.
   */
  MarkerCoordinator markerCoordinator = new MarkerCoordinator()

  /**
   * Simple constructor.
   * @param controllerClass The class.
   */
  MarkerContext(Class controllerClass) {
    controller = controllerClass?.newInstance()
    this.controllerClass = controllerClass
  }

  /**
   * Map constructor.
   * @param options The map options.
   */
  MarkerContext(Map options) {
    options?.each { k, v ->
      this[k as String] = v
    }
  }

  /**
   * Returns the current URI.  Source: from the current request.
   * @return The URI (as a string).
   */
  String getUri() {
    if (!uri) {
      uri = Holders.currentRequest?.uri?.toString()
    }
    return uri
  }

  /**
   * Returns the class for the controller.
   * @return The Class.
   */
  Class getControllerClass() {
    if (!controllerClass) {
      controllerClass = controller?.getClass()
    }
    return controllerClass
  }

  /**
   * Builds a human-readable form of this marker context and its context for use in exceptions.
   * This should give user some idea where the error happened.
   * @return The exception display.
   */
  String toStringForException() {
    def sName = getControllerClass()?.simpleName
    def sView = "${getUri() ?: ''} $view" ?: ''
    return "Context: $sName[$sView]"
  }


}
