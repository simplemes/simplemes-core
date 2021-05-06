package org.simplemes.eframe.web
/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Utilities to work with display panels.
 */
class PanelUtils {

  /**
   * Determines if the given 'field' name is a panel. Typically, these start with 'group:'.
   * @param fieldName The field name.
   * @return True if it is a panel (group).
   */
  static boolean isPanel(String fieldName) {
    return fieldName?.startsWith('group:') && fieldName.size() > 6
  }

  /**
   * Determines the panel name from a given 'field' name definition.
   * Leaves the string unchanged if not a panel (group).
   * @param fieldName The field name.
   * @return The panel name.  Returns 'status' if field name is 'group:status'.  Returns null if not a panel.
   */
  static String getPanelName(String fieldName) {
    if (isPanel(fieldName)) {
      return fieldName[6..-1]
    }
    return null
  }

  /**
   * Takes the given list of fields an organizes the fields into a map for each panel.
   *
   * @param fields The fields.  If a field is in the format: 'group:details', then it is a group.
   * @return The Map with an entry for each panel.  Each entry is a list of fields in the panel.
   *         If no panels are found, then an empty map is returned.
   *         This relies on the Java rule for LinkedHashMap's that keySet() returns the list in the order
   *         the keys were inserted.
   */
  static Map<String, List<String>> organizeFieldsIntoPanels(List<String> fields) {
    def res = [:]
    if (!fields) {
      return res as Map<String, List<String>>
    }

    def panels = []
    def panelList = fields.findAll { isPanel(it) }

    if (panelList) {
      def currentFieldList = []
      def panel = getPanelName(fields[0]) ?: 'main'  // Default the first panel to main
      panels << panel
      res[panel] = currentFieldList

      for (field in fields) {
        if (isPanel(field)) {
          panel = getPanelName(field)
          if (res.keySet().contains(panel)) {
            // Panel is already in the result, so just add the fields to its list.
            currentFieldList = res[panel]
          } else {
            currentFieldList = []
            res[panel] = currentFieldList
          }
        } else {
          currentFieldList << field
        }
      }
    }
    return res as Map<String, List<String>>
  }

}
