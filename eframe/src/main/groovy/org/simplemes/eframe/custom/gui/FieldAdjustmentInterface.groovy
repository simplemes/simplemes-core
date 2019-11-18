package org.simplemes.eframe.custom.gui

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This interface defines a adjustment to the fields/order defined for a domain class.
 * The implementations of this interface provides the logic to add/move/remove fields from
 * the GUIs.
 *
 */
interface FieldAdjustmentInterface {

  /**
   * Applies the field adjustment to the given list of fields. Modifies the list in place.
   * @param fieldOrder The original field order (modified).
   * @return The original fieldOrder list (modified).
   */
  List<String> apply(List<String> fieldOrder)

  /**
   * Removes the given field name from this adjustment.
   * @param fieldName The field to remove.
   * @return If true, then the entire field adjustment should be removed.
   */
  boolean removeField(String fieldName)


}
