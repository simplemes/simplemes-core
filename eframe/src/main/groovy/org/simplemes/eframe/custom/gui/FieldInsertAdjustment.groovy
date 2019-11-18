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
 * This holds a single Insert adjustment to the field order for a GUI.
 *
 */
@ToString(includePackage = false)
@EqualsAndHashCode
class FieldInsertAdjustment implements FieldAdjustmentInterface, TypeableJSONInterface {
  /**
   * The name of the field to insert into the display.
   */
  String fieldName

  /**
   * The name of the core field to insert this field after.  If '-', then insert before the first field.
   * If field not found, then inserts at the end.
   */
  String afterFieldName

  /**
   * Applies the field adjustment to the given list of fields. Modifies the list in place.
   * @param fieldOrder The original field order (modified).
   * @return The original fieldOrder list (modified).
   */
  @Override
  List<String> apply(List<String> fieldOrder) {
    // Find the place to insert it.
    def index = fieldOrder.findIndexOf { it == afterFieldName }
    if (index == -1) {
      index = afterFieldName == '-' ? -1 : fieldOrder.size() - 1
    }
    fieldOrder.add(index + 1, fieldName)
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
