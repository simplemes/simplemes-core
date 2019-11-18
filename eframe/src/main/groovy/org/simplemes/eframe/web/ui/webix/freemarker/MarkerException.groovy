package org.simplemes.eframe.web.ui.webix.freemarker

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The general-purpose run-time error for marker exceptions.
 * Usually used for mis-use of the marker or invalid arguments.
 */
class MarkerException extends Exception {

  /**
   * The message.
   */
  String message

  /**
   * The marker that caused the problem.
   */
  MarkerInterface marker

  /**
   * Builds an exception message for the given marker.
   * @param message The message.
   * @param marker The marker.
   */
  MarkerException(String message, MarkerInterface marker) {
    super(message, null)
    this.marker = marker
    this.message = message
  }


  /**
   * Builds the message for display.
   * @return The message.
   */
  @Override
  String toString() {
    def s = marker?.toStringForException() ?: ''
    return "${message}: $s"
  }
}
