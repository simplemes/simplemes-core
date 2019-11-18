package org.simplemes.eframe.web.view

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A simple wrapper used to wrap a Map so the Freemarker will not recursively wrap the elements.
 * If a Map is added to the freemarker model, then it will be difficult to extract the value from
 * the model.  The {@link org.simplemes.eframe.web.ui.webix.freemarker.BaseMarker#unwrap(java.lang.Object)}
 * method will automatically unwrap this extra layer of wrapping.
 */
class FreemarkerWrapper {
  /**
   * The wrapped value.
   */
  Object value

  /**
   * Creates a wrapper for the given value.
   * @param value The value.
   * @return The value.
   */
  FreemarkerWrapper(Object value) {
    this.value = value
  }

}
