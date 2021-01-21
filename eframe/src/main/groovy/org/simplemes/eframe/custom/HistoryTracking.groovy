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
  NONE('NONE', 0),

  /**
   * The values are retained (no user or date info).
   */
  VALUES('VALUES', 1),

  /**
   * All Data is retained, including user and date/time.
   */
  ALL('ALL', 2)

  /**
   * The standardized field type associated with this field format.
   */
  final String id

  /**
   * The level of detail (as a numeric relative value).
   */
  final int detailLevel

  /**
   * Convenience constructor for an interval.
   * @param id The stored value in the DB.
   * @param detailLevel The relative detail level (0- lowest).
   */
  HistoryTracking(String id, int detailLevel) {
    this.id = id
    this.detailLevel = detailLevel
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
