package org.simplemes.eframe.custom.gui

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.json.TypeableJSONInterface

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This holds a single Removal adjustment to the field order for a GUI.
 *
 */
@ToString(includePackage = false)
@EqualsAndHashCode
class FieldRemoveAdjustment implements FieldAdjustmentInterface, TypeableJSONInterface {
  /**
   * The name of the custom field to remove from the display.
   */
  String fieldName

  /**
   * Applies the field adjustment to the given list of fields. Modifies the list in place.
   * If the field is not found, then does nothing.
   * @param fieldOrder The original field order (modified).
   * @return The original fieldOrder list (modified).
   */
  @Override
  List<String> apply(List<String> fieldOrder) {
    fieldOrder.remove(fieldName)
    return fieldOrder
  }

  /**
   * Removes the given field name from this adjustment.
   * @param fieldName The field to remove.
   * @return If true, then the entire field adjustment should be removed.
   */
  @Override
  boolean removeField(String fieldName) {
    return fieldName == this.fieldName
  }

}
