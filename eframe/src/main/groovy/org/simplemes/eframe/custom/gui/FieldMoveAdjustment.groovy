package org.simplemes.eframe.custom.gui

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.json.TypeableJSONInterface
import org.simplemes.eframe.misc.LogUtils

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This holds a single field move adjustment to the field order for a GUI.
 *
 */
@ToString(includePackage = false)
@EqualsAndHashCode
class FieldMoveAdjustment implements FieldAdjustmentInterface, TypeableJSONInterface {
  /**
   * The logger for this class.
   */
  protected static log = LogUtils.getLogger(this)

  /**
   * The name of the field to insert into the display.  If it does not exist, then the apply() method does nothing.
   */
  String fieldName

  /**
   * The name of the field to move this field to.
   * If field not found, then moves to the end.  If '-' then moves to beginning.
   */
  String afterFieldName

  /**
   * Applies the field adjustment to the given list of fields. Modifies the list in place.
   * @param fieldOrder The original field order (modified).
   * @return The original fieldOrder list (modified).
   */
  @Override
  List<String> apply(List<String> fieldOrder) {
    // See if the field is already in the right place.
    def currentIndex = fieldOrder.findIndexOf { it == fieldName }
    if (currentIndex >= 0) {
      if (fieldOrder[currentIndex - 1] == fieldName) {
        // no change is needed.
        //println "No Change needed $this into $fieldOrder"
        return fieldOrder
      }
    } else {
      //println "Could not insert $this into $fieldOrder"
      log.warn("Could not insert {} into {}", this, fieldOrder)
      // Field is not in the list, so do nothing.
      return fieldOrder
    }

    // A move is needed, so remove it first.
    fieldOrder.remove(currentIndex)
    // Then add it back at the right place, find the place to insert it.
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
