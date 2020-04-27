/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.misc.LogUtils

/**
 * This is used to coordinate between multiple markers that need to produce a legal web page.
 * This holds various chunks of javascript/HTML produced by the markers and stored in
 * in a MarkerContext for the page being created.  There are specific sections that are used in specific
 * places in the output.  For example, the postscript is used after the ui is generated.
 * <p>
 * Each specific chunk is outlined below with their usage.
 * <p>
 * <b>Note:</b> This coordinator is only useful with nested markers (e.g. efForm with efEdit/efField inside).
 */
class MarkerCoordinator {

  /**
   * The javascript included before the UI is built.  Frequently used
   * to set focus in a specific field.
   */
  StringBuilder prescript = new StringBuilder()

  /**
   * The javascript included after the UI is built.  This is used to alter the formData and other variables before the UI is built.
   */
  StringBuilder postscript = new StringBuilder()

  /**
   * The global javascript included after the UI is built.  This is used to alter the formData and other variables before the UI is built.
   * The global postscript is a section of javascript code at the global context level in the browser.
   */
  StringBuilder globalPostscript = new StringBuilder()

  /**
   * The form's ID.  Used for many variables within the marker structure when elements are nested inside of a form.
   */
  String formID

  /**
   * The form's URL.  Used to provide the URL from a marker inside of the efForm block.
   */
  String formURL

  /**
   * A place to store other elements.  These are used when there the coordination is only needed between two markers.
   * The key should be a constant in one of the markers.
   */
  Map others = [:]

  /**
   * A counter that can be used to generate unique IDs when non are provided by the caller.
   */
  private Integer uniqueIDCounter = 0

  /**
   * Simple constructor.
   */
  MarkerCoordinator() {
  }

  /**
   * Adds the given post script text to the end of the current postscript.
   * The postscript is a section of javascript code executed after the main UI element creation code.
   * @param post The postscript to add.
   */
  void addPostscript(String post) {
    postscript << post
  }

  /**
   * Returns the current postscript (as a string).
   * The postscript is a section of javascript code executed after the main UI element creation code.
   * @return The current postscript.
   */
  String getPostscript() {
    postscript.toString()
  }

  /**
   * Adds the given global post script text to the end of the current global postscript.
   * The global postscript is a section of javascript code at the global context level in the browser.
   * This is typically only used inside of a Form.
   * @param post The postscript to add.
   */
  void addGlobalPostscript(String post) {
    globalPostscript << post
  }

  /**
   * Returns the current global postscript (as a string).
   * The global postscript is a section of javascript code at the global context level in the browser.
   * This is typically only used inside of a Form.
   * @return The current global postscript.
   */
  String getGlobalPostscript() {
    globalPostscript.toString()
  }

  /**
   * Adds the given pre script text to the end of the current prescript.
   * @param pre The prescript to add.
   */
  void addPrescript(String pre) {
    prescript << pre
  }

  /**
   * Returns the current prescript (as a string).
   * @return The current prescript.
   */
  String getPrescript() {
    prescript.toString()
  }

  /**
   * Increments the unique counter and returns the value.
   *
   * @return A counter that can be used to generate unique IDs when non are provided by the caller.
   */
  Integer getUniqueIDCounter() {
    uniqueIDCounter++
    return uniqueIDCounter
  }

  @Override
  String toString() {
    return "MarkerCoordinator{" +
      "formID='" + formID + '\'' +
      ", formURL='" + formURL + '\'' +
      //", others=" + LogUtils.limitedLengthString(others?.toString(),100) +
      ", prescript=" + LogUtils.limitedLengthString(prescript?.toString()) +
      ", postscript=" + LogUtils.limitedLengthString(postscript?.toString()) +
      '}'
  }
}
