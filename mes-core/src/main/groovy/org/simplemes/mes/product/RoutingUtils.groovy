package org.simplemes.mes.product

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides common utility methods for routings and operations.
 */
class RoutingUtils {

  /**
   * Combines the given key field and an operation sequence in a consistent format.
   * Returns "$key/$sequence".
   * @param key The key value.
   * @param sequence The operation sequence.
   * @return The combined string.
   */
  static String combineKeyAndSequence(String key, int sequence) {
    return "${key ?: '?'}/$sequence"
  }
}
