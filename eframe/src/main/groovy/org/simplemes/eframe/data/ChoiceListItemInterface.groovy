/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data

/**
 * Defines a single instance of the possible choices for a class.
 * Used with GUI widgets to define the list of values in a combobox or similar widgets.
 */
@SuppressWarnings("GroovyDocCheck")
interface ChoiceListItemInterface {

  /**
   * Returns internal ID for this choice.
   * @return The ID.
   */
  Object getId()

  /**
   * Returns the localized display value for the choice.
   * @param locale The locale to use for the localized string.
   * @return The display value.
   */
  String toStringLocalized(Locale locale)

  /**
   * Returns the localized display value for the choice (uses the default locale).
   * @return The display value.
   */
  String toStringLocalized()

  /**
   * Returns the value for the choice list (e.g. a domain record or enum element).
   * @return The value.
   */
  Object getValue()

  /**
   * Returns true if this choice is the default choice in the list.
   * @return True if it is the default.
   */
  boolean isDefaultChoice()

  /**
   * The display value for the choice.  Usually a lookup key in the language bundle.
   */
  String getDisplayValue()

}