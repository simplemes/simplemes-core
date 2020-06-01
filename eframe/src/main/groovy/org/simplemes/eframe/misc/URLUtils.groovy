/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc


/**
 * Common URL/URI utils.  Used to encapsulate the URI/URL manipulation utilities.
 *
 *
 */
class URLUtils {

  /**
   * Adds any arguments to the URI (encoded) to the given URI.  This detects if the URI already has any parameters
   * and adds the parameters correctly.
   * @param uri The URI.
   * @param parameters The extra parameters to add (if any). (Can be null).
   * @return The adjusted URI.
   */
  static String addParametersToURI(String uri, Map parameters = null) {
    if (parameters) {
      def hasParams = uri.contains('?')
      for (String key in parameters.keySet()) {
        def value = parameters."$key"
        if (value) {
          if (hasParams) {
            uri += '&'
          } else {
            uri += '?'
          }
          hasParams = true
          uri += "${encode(key)}=${encode(value)}"
        }
      }
    }
    return uri
  }

  /**
   * Internal encoding of URI strings.
   * @param value The value to encode.
   * @return The encoded value.
   */
  protected static String encode(String value) {
    return URLEncoder.encode(value.toString(), 'UTF-8')
  }

}
