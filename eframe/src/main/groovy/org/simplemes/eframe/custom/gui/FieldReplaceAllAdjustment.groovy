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
 * This holds a the replace all list for a field list adjustment.
 *
 */
@ToString(includePackage = false)
@EqualsAndHashCode
class FieldReplaceAllAdjustment implements FieldAdjustmentInterface, TypeableJSONInterface {
  /**
   * The name of the fields to insert into the display.  Inserted in order after clearing the list.
   */
  List<String> fieldNames = []

  /**
   * Applies the field adjustment to the given list of fields. Modifies the list in place.
   * @param fieldOrder The original field order (modified).
   * @return The original fieldOrder list (modified).
   */
  @Override
  List<String> apply(List<String> fieldOrder) {
    fieldOrder.clear()
    fieldOrder.addAll(fieldNames)
    return fieldOrder
  }

  /**
   * Removes the given field name from this adjustment.
   * @param fieldName The field to remove.
   * @return If true, then the entire field adjustment should be removed.
   */
  @Override
  boolean removeField(String fieldName) {
    fieldNames.remove(fieldName)
    return fieldNames.size() == 0
  }
}
