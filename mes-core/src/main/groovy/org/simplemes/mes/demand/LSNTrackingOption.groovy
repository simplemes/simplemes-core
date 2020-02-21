package org.simplemes.mes.demand

import org.simplemes.eframe.i18n.GlobalUtils

/*
 * Copyright Michael Houston. All rights reserved.
 *
 */

/**
 * Defines the options available for LSN tracking.  Provides localized string values.
 *
 */
enum LSNTrackingOption {

  // Someday, we will add an option to have lightweight LSN with no QTY/routing tracking.  For now, that is not possible.
  // When we do add this option, check the ResolveServiceTest for some commented tests.
  /**
   * No LSN tracking is allowed or used. The Order is considered one big lot.
   * <b>(default)</b>.
   */
  ORDER_ONLY("O"),

  /**
   * Tracking by either LSN or order is allowed. If no LSN is specified, then the LSN will be chosen from the order automatically, if any exist.
   */
  LSN_ALLOWED("B"),

  /**
   * Only LSN tracking is allowed. If no LSN is specified then the request will fail.
   */
  LSN_ONLY("L"),

  /**
   * The ID used for persistence.
   */
  final String id

  /**
   * Build a LSNTrackingOption entry.
   * @param id The ID for the status.
   */
  LSNTrackingOption(String id) {
    this.id = id
  }

  /**
   * Determines if the Order processing is allowed for this tracking option.
   * @return True if Order processing is allowed.
   */
  boolean isOrderAllowed() {
    return (this == LSN_ALLOWED || this == ORDER_ONLY)
  }

  /**
   * Determines if the LSN processing is allowed for this tracking option.
   * @return True if LSN processing is allowed.
   */
  boolean isLSNAllowed() {
    return (this == LSN_ALLOWED || this == LSN_ONLY)
  }

  /**
   * Provide a string format of this option, in the default Locale.
   * @return The string value to display.
   */
  @Override
  String toString() {
    return toStringLocalized(Locale.default)
  }

  /**
   * Provide a string format of this option in the given Locale.
   * @locale The locale to return the string for.
   * @return The string value to display.
   */
  String toStringLocalized(Locale locale = null) {
    String key = "lsnTrackingOption.${id}.label"
    return GlobalUtils.lookup(key, null, locale)
  }

  /**
   * An inner class to hold the constant DB column size needed for the enum.
   */
  static class Sizes {
    /**
     * The size of the column needed for persistence.
     */
    public static final int COLUMN_SIZE = 1
  }

}
