package org.simplemes.eframe.custom


import org.simplemes.eframe.i18n.GlobalUtils

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 *
 * Defines the options for tracking custom field history.
 * This affects the history data retained in the extensible field holder.
 *
 */
enum HistoryTracking implements Comparable {

  /**
   * None.
   */
  NONE('N'),

  /**
   * The values are retained (no user or date info).
   */
  VALUES('V'),

  /**
   * All Data is retained, including user and date/time.
   */
  ALL_DATA_AND_VALUES('A')

  /**
   * The standardized field type associated with this field format.
   */
  final String id

  /**
   * Convenience constructor for an interval.
   * @param id The stored value in the DB.
   */
  HistoryTracking(String id) {
    this.id = id
  }

  /**
   * Build a human-readable version of this object.
   * @param locale The locale to display the enum display text.
   * @return The human-readable string.
   */
  String toStringLocalized(Locale locale = null) {
    return GlobalUtils.lookup("historyTracking.${name()}.label", null, locale)
  }


}
