/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker
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
    write(lookup(parameters.key, null, findArgs()))
  }

  /**
   * Finds the args passed as arg1, arg2... parameters for the marker.
   * @return The args (if any).
   */
  Object[] findArgs() {
    def args = []
    def argNumbers = []
    parameters?.each { k, v ->
      // Find the argument number for each argument
      if (k.startsWith('arg')) {
        def s = k - 'arg'
        try {
          argNumbers << Integer.valueOf(s)
        } catch (NumberFormatException ignored) {
          throw new MarkerException("efLookup argN options require a valid integer.  '$s' is not valid", this)
        }
      }
    }
    if (argNumbers) {
      argNumbers = argNumbers.sort()
    }
    for (n in argNumbers) {
      def k = "arg$n"
      args << parameters[(String) k]
    }
    args as Object[]
  }


}

