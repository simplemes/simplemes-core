package org.simplemes.eframe.web.ui

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the interface for all GUI widgets.
 */
interface WidgetInterface {
  /**
   * Builds the string for the UI elements.
   * @return The HTML/Script needed to generate the UI element.
   */
  CharSequence build()

}