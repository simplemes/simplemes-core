/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

/**
 * Miscellaneous utilities for manipulating UUID's in the application and for the framework.
 *
 */
class UUIDUtils {

  /**
   * Converts a possible object (string) to a UUID.  Does nothing if the value is not a UUID.
   * @param value The value to convert to a UUID, if needed.
   * @return The original object or a UUID created from the object.
   */
  static Object convertToUUIDIfPossible(Object value) {
    if (!value) {
      return value
    }
    if (value instanceof UUID) {
      return value
    }
    def s = value.toString()
    if (s.length() == 36 && s.count('-') == 4) {
      try {
        return UUID.fromString(s)
      } catch (IllegalArgumentException ignored) {
        // Will just return the original value unchanged.
      }
    }

    return value
  }

  /**
   * Returns true if the given string is a valid UUID.
   * @param s The possible UUID string.
   * @return True if this is a legal UUID.
   */
  static boolean isUUID(String s) {
    def matcher = s =~ /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/
    return matcher.matches()
  }

}
