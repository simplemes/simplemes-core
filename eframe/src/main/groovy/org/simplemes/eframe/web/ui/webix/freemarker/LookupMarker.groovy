package org.simplemes.eframe.web.ui.webix.freemarker
/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides the implementation of the &lt;@efLookup&gt; marker.
 * This marker can localize a simple string from the messages.properties file.
 *
 */
@SuppressWarnings("unused")
class LookupMarker extends BaseMarker {
  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    if (!parameters.key) {
      throw new MarkerException("efLookup requires the 'key' option", this)
    }
    write(lookup(parameters.key))
  }

}

