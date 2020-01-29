/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.gui


import org.simplemes.eframe.custom.domain.FieldGUIExtension

/**
 * This class gathers the field customizations for a domain and manipulates the field ordering as needed
 * to deal with the user's customizations.
 *
 */
class FieldAdjuster {

  /**
   * Finds and applies any field order adjustments to the core field order. Will check the given domain class and
   * any super classes.
   * <p>
   * @param domainClass The domain class.
   * @param fieldOrder The current field order array. This modifies the list passed in.
   * @return The effective field order list.  This is a single flattened list of field names.
   */
  static List<String> applyUserAdjustments(Class domainClass, List<String> fieldOrder) {
    List list = fieldOrder
    def fieldGUIExtension = FieldGUIExtension.findByDomainName(domainClass.name)
    List<FieldAdjustmentInterface> adjustments = fieldGUIExtension?.adjustments
    for (adj in adjustments) {
      adj.apply(list)
    }

    if (domainClass.superclass && domainClass.superclass != Object) {
      return applyUserAdjustments(domainClass.superclass, list)
    }
    return list
  }

  /**
   * Compares the two display order lists and generates a list of field adjustments needed to make the fields display
   * in the listB order.  Tries to use a limited add/remove/move list of changes.  Will fall back to a replace all
   * approach if the result does not work.
   *
   * @param listA The original list.
   * @param listB The desired list order.
   * @return The differences needed to achieve the listB ordering.
   */
  static List<FieldAdjustmentInterface> determineDifferences(List<String> listA, List<String> listB) {
    def differences = []

    // First, remove entries not in listB
    for (s in listA) {
      if (!listB.contains(s)) {
        differences << new FieldRemoveAdjustment(fieldName: s)
      }
    }

    // Now, check each element in the desired list
    for (s in listB) {
      def predecessorA = findPredecessor(listA, s)
      def predecessorB = findPredecessor(listB, s)
      if (listA.contains(s)) {
        if (predecessorA == predecessorB) {
          // No change in position.
          //println "No Change for $s: $predecessorA, $predecessorB"
          continue
        }
        // Changed position, so add a move entry
        differences << new FieldMoveAdjustment(fieldName: s, afterFieldName: predecessorB)
      } else {
        // Not in the old list, so insert at the right place.
        //diff << "+$s@$predecessorB"
        differences << new FieldInsertAdjustment(fieldName: s, afterFieldName: predecessorB)
      }
    }

    // Now, replay the adjustments and verify that the results match.
    def newList = listA.clone()
    for (adj in differences) {
      adj.apply(newList)
    }
    if (listB != newList) {
      // Need to fallback to the ugly flush/fill.
      differences = [new FieldReplaceAllAdjustment(fieldNames: listB)]
    }

    //println "diff = $differences"
    return differences
  }

  /**
   * Finds the predecessor string for a given element.
   * @param list The list of strings to check.
   * @param value The value.
   * @return The predecessor ('-' if none).
   */
  protected static String findPredecessor(List<String> list, String value) {
    def i = list.indexOf(value)
    if (i > 0) {
      return list[i - 1]
    }
    return '-'
  }


}
